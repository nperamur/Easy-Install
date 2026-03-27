package neelesh.easy_install;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EasyInstallJsonHandler {

    private static Path getPath() {
        return EasyInstallClient.getGameDirAsFile().toPath().resolve("easy_install/deleted_files.json");
    }
    public static void addDeletedFile(String file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            JsonArray jsonArray = getDeletedFiles();
            jsonArray.add(file);
            Files.write(getPath(), gson.toJson(jsonArray).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JsonArray getDeletedFiles() {
        if (Files.notExists(getPath().getParent())) {
            try {
                Files.createDirectories(getPath().getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File file = getPath().toFile();
        if (!file.exists()) {
            return new JsonArray();
        }
        JsonArray jsonArray = null;
        try (BufferedReader bufferedReader = Files.newBufferedReader(getPath())) {
            jsonArray = new JsonParser().parse(bufferedReader).getAsJsonArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    public static void clearDeletedFiles() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.write(getPath(), gson.toJson(new JsonArray()).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
