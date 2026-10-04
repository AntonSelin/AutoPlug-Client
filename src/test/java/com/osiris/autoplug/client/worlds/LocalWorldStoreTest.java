package com.osiris.autoplug.client.worlds;

import com.osiris.autoplug.client.profiles.LauncherServices;
import com.osiris.autoplug.client.ui.LauncherActions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class LocalWorldStoreTest {
    @TempDir Path temporary;

    @Test void discoversRawAndCompressedMetadataAndIconsWithoutChangingSaves() throws Exception {
        Path minecraft = temporary.resolve("minecraft");
        Path modern = save(minecraft, "Folder with spaces", "Display name", "1.21.1", true, false, false);
        save(minecraft, "Legacy", "", null, false, false, false);
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "png", modern.resolve("icon.png").toFile());
        byte[] before = Files.readAllBytes(modern.resolve("level.dat"));
        java.nio.file.attribute.FileTime modified = Files.getLastModifiedTime(modern.resolve("level.dat"));
        List<LocalWorld> worlds = new LocalWorldStore(minecraft).list();
        assertEquals(2, worlds.size());
        LocalWorld world = worlds.get(0);
        assertEquals("local:Folder with spaces", world.id);
        assertEquals("Display name", world.name);
        assertEquals("Folder with spaces", world.saveName());
        assertEquals("1.21.1", world.gameVersion);
        assertEquals(minecraft, world.gameDirectory);
        assertEquals(modern.resolve("icon.png"), world.icon);
        assertEquals("Legacy", worlds.get(1).name);
        assertEquals("", worlds.get(1).gameVersion);
        assertArrayEquals(before, Files.readAllBytes(modern.resolve("level.dat")));
        assertEquals(modified, Files.getLastModifiedTime(modern.resolve("level.dat")));
        assertFalse(Files.exists(minecraft.resolve(".autoplug")));
    }

    @Test void missingMinecraftDirectoryIsNotCreatedByDiscovery() throws Exception {
        Path absent = temporary.resolve("not-installed");
        assertTrue(new LocalWorldStore(absent).list().isEmpty());
        assertFalse(Files.exists(absent));
    }

    @Test void versionMustMatchAndUnknownRequiresAnExplicitChoice() throws Exception {
        Path minecraft = temporary.resolve("minecraft");
        save(minecraft, "Modern", "Modern", "1.21.1", true, false, false);
        save(minecraft, "Old", "Old", null, false, false, false);
        LocalWorldStore store = new LocalWorldStore(minecraft);
        LocalWorld modern = store.get("local:Modern"), old = store.get("local:Old");
        assertEquals("1.21.1", store.launchVersion(modern, null));
        assertThrows(IOException.class, () -> store.launchVersion(modern, "1.21.2"));
        assertThrows(IOException.class, () -> store.launchVersion(old, ""));
        assertEquals("1.7.10", store.launchVersion(old, "1.7.10"));
        assertThrows(IOException.class, () -> store.launchVersion(old, "../../latest"));
        // Metadata is not cached: a changed save is reflected before any subsequent launch.
        save(minecraft, "Modern", "Modern", "1.21.2", true, false, false);
        assertEquals("1.21.2", store.get("local:Modern").gameVersion);
    }

    @Test void moddedMarkersRemainVisibleButPreventVanillaLaunch() throws Exception {
        Path minecraft = temporary.resolve("minecraft");
        save(minecraft, "WasModded", "Modded", "1.21.1", true, true, false);
        save(minecraft, "Forge", "Forge", "1.21.1", true, false, true);
        LocalWorldStore store = new LocalWorldStore(minecraft);
        assertEquals(2, store.list().size());
        for (LocalWorld world : store.list()) {
            assertTrue(world.modded);
            assertTrue(assertThrows(IOException.class, () -> store.launchVersion(world, "1.21.1")).getMessage().contains("modded"));
        }
    }

    @Test void prereleaseDisplayNamesResolveOnlyToTheSamePublishedBuild() throws Exception {
        Path minecraft = temporary.resolve("minecraft");
        save(minecraft, "Prerelease", "Prerelease", "1.20 Pre-release 1", true, false, false);
        save(minecraft, "Candidate", "Candidate", "1.20.2 Release Candidate 2", true, false, false);
        LocalWorldStore store = new LocalWorldStore(minecraft);
        LocalWorld prerelease = store.get("local:Prerelease"), candidate = store.get("local:Candidate");
        assertEquals("1.20 Pre-release 1", prerelease.gameVersion);
        assertEquals("1.20-pre1", store.launchVersion(prerelease, null));
        assertEquals("1.20-pre1", store.launchVersion(prerelease, "1.20-pre1"));
        assertEquals("1.20-pre1", store.launchVersion(prerelease, "1.20 pRE-rELEASe 1"));
        assertEquals("1.20.2-rc2", store.launchVersion(candidate, "1.20.2-rc2"));
        assertEquals("1.20.2-rc2", store.launchVersion(candidate, "1.20.2 RELEASE CANDIDATE 2"));
        assertThrows(IOException.class, () -> store.launchVersion(prerelease, "1.20-pre2"));
        assertThrows(IOException.class, () -> store.launchVersion(prerelease, "1.20"));
        assertThrows(IOException.class, () -> store.launchVersion(candidate, "1.20.2-rc1"));
        assertThrows(IOException.class, () -> store.launchVersion(candidate, "1.20.2-rc2 extra"));
    }

    @Test void malformedSaveDoesNotHideValidWorldAndThumbnailIsOptional() throws Exception {
        Path minecraft = temporary.resolve("minecraft");
        Path good = save(minecraft, "Valid", "Valid", "1.21.1", true, false, false);
        Files.write(good.resolve("icon.png"), new byte[]{1, 2, 3});
        Path broken = Files.createDirectories(minecraft.resolve("saves/Broken"));
        Files.write(broken.resolve("level.dat"), new byte[]{10, 0, 0, 10});
        List<String> messages = new ArrayList<>();
        List<LocalWorld> worlds = new LocalWorldStore(minecraft, messages::add).list();
        assertEquals(1, worlds.size()); assertNull(worlds.get(0).icon);
        assertTrue(messages.stream().anyMatch(message -> message.contains("Broken")));
        assertThrows(IOException.class, () -> new LocalWorldStore(minecraft).get("local:Broken"));
    }

    @Test void rejectsTraversalAndLinkedSaveOrMetadata() throws Exception {
        Path minecraft = temporary.resolve("minecraft");
        Path original = save(minecraft, "Original", "Original", "1.21.1", false, false, false);
        LocalWorldStore store = new LocalWorldStore(minecraft);
        for (String invalid : Arrays.asList("Original", "local:..", "local:../Original", "local:nested/Original", "local:C:world", "local:"))
            assertThrows(IOException.class, () -> store.get(invalid));
        Path linked = minecraft.resolve("saves/Linked");
        try { Files.createSymbolicLink(linked, original); }
        catch (IOException | UnsupportedOperationException | SecurityException e) { Assumptions.assumeTrue(false, "Host does not permit fixture symlinks"); }
        assertThrows(IOException.class, () -> store.get("local:Linked"));
        Path linkedMetadata = Files.createDirectories(minecraft.resolve("saves/LinkedMetadata"));
        Files.createSymbolicLink(linkedMetadata.resolve("level.dat"), original.resolve("level.dat"));
        assertThrows(IOException.class, () -> store.get("local:LinkedMetadata"));
        assertEquals(1, store.list().size());
    }

    @Test void boundedReaderRejectsNegativeArraysDeepNestingAndCompressedExpansion() throws Exception {
        Path badLength = temporary.resolve("negative.dat");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(badLength))) {
            tag(out, 10, ""); tag(out, 7, "bytes"); out.writeInt(-1);
        }
        assertThrows(IOException.class, () -> new LevelDatReader().read(badLength));
        Path deep = temporary.resolve("deep.dat");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(deep))) {
            tag(out, 10, ""); for (int i = 0; i < 40; i++) tag(out, 10, "nested");
            for (int i = 0; i < 41; i++) out.writeByte(0);
        }
        assertThrows(IOException.class, () -> new LevelDatReader().read(deep));
        Path expanded = temporary.resolve("expanded.dat");
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(expanded)))) {
            tag(out, 10, ""); tag(out, 10, "Data"); tag(out, 7, "bytes"); out.writeInt(16 * 1024 * 1024);
            byte[] block = new byte[8192]; for (int i = 0; i < 2048; i++) out.write(block);
            out.writeByte(0); out.writeByte(0);
        }
        assertTrue(Files.size(expanded) < 100000);
        assertThrows(IOException.class, () -> new LevelDatReader().read(expanded));
    }

    @Test void duplicateVersionTagsAreRejectedRatherThanSilentlyOverridingSafetyMetadata() throws Exception {
        Path file = temporary.resolve("duplicate.dat");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
            tag(out, 10, ""); tag(out, 10, "Data");
            tag(out, 1, "WasModded"); out.writeByte(1);
            tag(out, 1, "WasModded"); out.writeByte(0);
            out.writeByte(0); out.writeByte(0);
        }
        assertThrows(IOException.class, () -> new LevelDatReader().read(file));
    }

    @Test void servicesCombineLocalAndManagedWorldsAndRejectUpgradeBeforeDownloads() throws Exception {
        Path minecraft = temporary.resolve("minecraft");
        Path original = save(minecraft, "Local", "Local save", "1.21.1", true, false, false);
        byte[] before = Files.readAllBytes(original.resolve("level.dat"));
        try (LauncherServices services = new LauncherServices(temporary.resolve("autoplug"), minecraft, ignored -> { })) {
            LauncherActions.ProfileInfo server = services.createProfile("Server", "1.21.1", "VANILLA", "MODS_SERVER", false);
            LauncherActions.ProfileInfo client = services.createProfile("Client", "1.21.1", "VANILLA", "MODS", false);
            services.createWorld("Managed", server.id, client.id);
            List<LauncherActions.WorldInfo> worlds = services.worlds();
            assertEquals(2, worlds.size());
            assertTrue(worlds.get(0).local); assertEquals("1.21.1", worlds.get(0).gameVersion);
            assertEquals(original.toString(), worlds.get(0).directory);
            assertFalse(worlds.get(1).local);
            assertThrows(IOException.class, () -> services.launchLocalWorld(worlds.get(0).id, "1.21.2"));
        }
        assertArrayEquals(before, Files.readAllBytes(original.resolve("level.dat")));
        assertFalse(Files.exists(minecraft.resolve(".autoplug")));
    }

    @Test void invalidLocalSavesRootDoesNotHideManagedWorlds() throws Exception {
        Path minecraft = Files.createDirectories(temporary.resolve("minecraft"));
        byte[] original = new byte[]{1, 2, 3};
        Files.write(minecraft.resolve("saves"), original);
        List<String> messages = new ArrayList<>();
        try (LauncherServices services = new LauncherServices(temporary.resolve("autoplug"), minecraft, messages::add)) {
            LauncherActions.ProfileInfo server = services.createProfile("Server", "1.21.1", "VANILLA", "MODS_SERVER", false);
            LauncherActions.ProfileInfo client = services.createProfile("Client", "1.21.1", "VANILLA", "MODS", false);
            LauncherActions.WorldInfo managed = services.createWorld("Managed", server.id, client.id);
            List<LauncherActions.WorldInfo> worlds = services.worlds();
            assertEquals(1, worlds.size());
            assertEquals(managed.id, worlds.get(0).id);
            assertFalse(worlds.get(0).local);
            assertTrue(messages.stream().anyMatch(message -> message.contains("Could not list local Minecraft saves")));
        }
        assertArrayEquals(original, Files.readAllBytes(minecraft.resolve("saves")));
    }

    private static Path save(Path minecraft, String folder, String name, String version, boolean compressed, boolean modded, boolean forge) throws IOException {
        Path directory = Files.createDirectories(minecraft.resolve("saves").resolve(folder));
        OutputStream raw = Files.newOutputStream(directory.resolve("level.dat"));
        try (DataOutputStream out = new DataOutputStream(compressed ? new GZIPOutputStream(raw) : raw)) {
            tag(out, 10, ""); tag(out, 10, "Data");
            tag(out, 8, "LevelName"); out.writeUTF(name);
            if (version != null) { tag(out, 10, "Version"); tag(out, 8, "Name"); out.writeUTF(version); out.writeByte(0); }
            tag(out, 1, "WasModded"); out.writeByte(modded ? 1 : 0);
            // Unknown arrays must be consumed correctly without being interpreted as metadata.
            tag(out, 12, "Unrelated"); out.writeInt(2); out.writeLong(1); out.writeLong(2);
            out.writeByte(0);
            if (forge) { tag(out, 10, "FML"); out.writeByte(0); }
            out.writeByte(0);
        }
        return directory;
    }
    private static void tag(DataOutputStream out, int type, String name) throws IOException { out.writeByte(type); out.writeUTF(name); }
}
