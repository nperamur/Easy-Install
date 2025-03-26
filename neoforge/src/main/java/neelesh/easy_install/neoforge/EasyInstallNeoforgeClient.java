package neelesh.easy_install.neoforge;


import com.google.gson.JsonArray;
import neelesh.easy_install.EasyInstall;
import neelesh.easy_install.EasyInstallJsonHandler;
import neelesh.easy_install.ProjectType;
import net.neoforged.fml.common.Mod;

import java.io.File;

import static neelesh.easy_install.EasyInstallClient.checkStatus;
import static neelesh.easy_install.EasyInstallClient.initializeDatapacks;

@Mod(EasyInstall.MOD_ID)
public class EasyInstallNeoforgeClient {
	public EasyInstallNeoforgeClient() {
		JsonArray deletedFiles = EasyInstallJsonHandler.getDeletedFiles();
		for (int i = 0; i < deletedFiles.size(); i++) {
			File file = new File(deletedFiles.get(i).getAsString());
			file.delete();
		}
		EasyInstallJsonHandler.clearDeletedFiles();
		for (ProjectType projectType : ProjectType.values()) {
			if (projectType != ProjectType.DATA_PACK) {
				checkStatus(projectType);
			}
		}
		initializeDatapacks();
	}

}

