package neelesh.easy_install.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.File;

public class EasyInstallClientImpl {
    public static String getLoader() {
        return "fabric";
    }

    public static String getGameDir() {
        return FabricLoader.getInstance().getGameDir().toString();
    }

    public static File getGameDirAsFile() {
        return FabricLoader.getInstance().getGameDir().toFile();
    }

    public static String getModLoaderDisplayText() {
        return "Minecraft " + Minecraft.getInstance().name() + "/Fabric (Modded)";
    }
}
