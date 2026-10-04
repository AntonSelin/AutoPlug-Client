package com.osiris.autoplug.client.worlds;

import com.osiris.autoplug.client.browser.VanillaServersReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Retains only display/version metadata from the shared bounded NBT decoder. */
final class LevelDatReader {
    static final class Metadata {
        String name = "", version = "";
        boolean modded;
    }

    Metadata read(Path path) throws IOException {
        Map<String, Object> root = new VanillaServersReader().readCompound(path);
        if (!(root.get("Data") instanceof Map<?, ?>)) throw new IOException("level.dat has no Data compound");
        Map<?, ?> data = (Map<?, ?>) root.get("Data");
        Metadata result = new Metadata();
        if (data.containsKey("LevelName") && !(data.get("LevelName") instanceof String)) throw new IOException("Invalid LevelName tag");
        if (data.containsKey("Version") && !(data.get("Version") instanceof Map<?, ?>)) throw new IOException("Invalid Version tag");
        if (data.containsKey("WasModded") && !(data.get("WasModded") instanceof Number)) throw new IOException("Invalid WasModded tag");
        if (data.get("LevelName") instanceof String) result.name = (String) data.get("LevelName");
        if (data.get("Version") instanceof Map<?, ?>) {
            Object name = ((Map<?, ?>) data.get("Version")).get("Name");
            if (name != null && !(name instanceof String)) throw new IOException("Invalid version Name tag");
            if (name instanceof String) result.version = (String) name;
        }
        result.modded = data.get("WasModded") instanceof Number && ((Number) data.get("WasModded")).intValue() != 0;
        for (String marker : Arrays.asList("FML", "fml", "ForgeData", "forge", "neoforge", "fabric", "Mods", "mods", "ModList"))
            if (root.containsKey(marker) || data.containsKey(marker)) result.modded = true;
        Object brands = data.get("ServerBrands");
        if (brands instanceof List<?>) for (Object brand : (List<?>) brands)
            if (brand instanceof String && !"vanilla".equals(brand)) result.modded = true;
        return result;
    }
}
