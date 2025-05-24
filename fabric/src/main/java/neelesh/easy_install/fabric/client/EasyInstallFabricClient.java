package neelesh.easy_install.fabric.client;

import com.google.gson.JsonArray;
import neelesh.easy_install.*;
import net.fabricmc.api.ClientModInitializer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

import static neelesh.easy_install.EasyInstallClient.checkStatus;
import static neelesh.easy_install.EasyInstallClient.initializeDatapacks;

public class EasyInstallFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
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

