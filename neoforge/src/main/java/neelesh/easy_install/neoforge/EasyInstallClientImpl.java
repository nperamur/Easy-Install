package neelesh.easy_install.neoforge;


import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;

public class EasyInstallClientImpl {
    public static String getLoader() {
        return "neoforge";
    }

    public static String getGameDir() {
        return FMLPaths.GAMEDIR.get().toFile().toString();
    }

    public static File getGameDirAsFile() {
        return FMLPaths.GAMEDIR.get().toFile();
    }

    public static String getModLoaderDisplayText() {
        return "NeoForge " + FMLLoader.getCurrent().getVersionInfo().neoForgeVersion() + " (" + ModList.get().size() + " Mods)";
    }
}
