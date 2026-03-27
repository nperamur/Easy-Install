package neelesh.easy_install.fabric;

import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.File;

public class EasyInstallClientImpl implements Platform {
    public String getLoader() {
        return "fabric";
    }

    public String getGameDir() {
        return FabricLoader.getInstance().getGameDir().toString();
    }

    public File getGameDirAsFile() {
        return FabricLoader.getInstance().getGameDir().toFile();
    }

    public String getModLoaderDisplayText() {
        return "Minecraft " + EasyInstallClient.getGameVersion() + " (Modded)";
    }
}
