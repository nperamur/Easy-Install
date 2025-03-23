package neelesh.easy_install.neoforge;


import net.minecraft.client.MinecraftClient;

import java.io.File;

public class EasyInstallClientImpl {
    public static String getLoader() {
        return "neoforge";
    }

    public static String getGameDir() {
        return MinecraftClient.getInstance().runDirectory.toString();
    }

    public static File getGameDirAsFile() {
        return MinecraftClient.getInstance().runDirectory;
    }
}
