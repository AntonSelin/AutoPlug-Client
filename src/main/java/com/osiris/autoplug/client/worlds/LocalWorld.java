package com.osiris.autoplug.client.worlds;

import java.nio.file.Path;

/** A read-only view of an existing Minecraft save; it is never a managed server. */
public final class LocalWorld {
    public final String id, name, gameVersion;
    public final Path directory, gameDirectory, icon;
    public final boolean modded;

    LocalWorld(String id, String name, String gameVersion, Path directory, Path gameDirectory, Path icon, boolean modded) {
        this.id = id; this.name = name; this.gameVersion = gameVersion;
        this.directory = directory; this.gameDirectory = gameDirectory; this.icon = icon; this.modded = modded;
    }

    /** The directory name, rather than LevelName, is what Minecraft Quick Play accepts. */
    public String saveName() { return directory.getFileName().toString(); }
}
