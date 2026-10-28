package neelesh.easy_install.neoforge;


import neelesh.easy_install.Platform;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;

public class EasyInstallClientImpl implements Platform {
    public String getLoader() {
        return "neoforge";
    }

    public String getGameDir() {
        return FMLPaths.GAMEDIR.get().toFile().toString();
    }

    public File getGameDirAsFile() {
        return FMLPaths.GAMEDIR.get().toFile();
    }

    public String getModLoaderDisplayText() {
        return "NeoForge " + FMLLoader.getCurrent().getVersionInfo().neoForgeVersion() + " (" + ModList.get().size() + " Mods)";
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
