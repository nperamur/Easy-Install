package neelesh.easy_install.neoforge;


import com.google.gson.JsonArray;
import neelesh.easy_install.EasyInstall;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.EasyInstallJsonHandler;
import neelesh.easy_install.ProjectType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import java.io.File;

import static neelesh.easy_install.EasyInstallClient.*;

@Mod(EasyInstall.MOD_ID)
@EventBusSubscriber(modid = EasyInstall.MOD_ID, value = Dist.CLIENT)
public class EasyInstallNeoforgeClient {
	public EasyInstallNeoforgeClient() {
		JsonArray deletedFiles = EasyInstallJsonHandler.getDeletedFiles();
		for (int i = 0; i < deletedFiles.size(); i++) {
			File file = new File(deletedFiles.get(i).getAsString());
			file.delete();
		}
		EasyInstallJsonHandler.clearDeletedFiles();
		cleanUpTempFiles();
		for (ProjectType projectType : ProjectType.values()) {
			if (projectType != ProjectType.DATA_PACK) {
				checkStatus(projectType);
			}
		}
		initializeDatapacks();
	}

	@SubscribeEvent
	public static void onShutdown(GameShuttingDownEvent event) {
		EasyInstallClient.stopAllDownloads();
	}

}

