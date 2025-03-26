package neelesh.easy_install.neoforge;


import net.minecraft.client.MinecraftClient;
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
}
