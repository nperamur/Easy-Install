package neelesh.easy_install;


import com.google.gson.*;
import neelesh.easy_install.gui.screen.ConfigButtonPlacement;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EasyInstallJsonHandler {

    private static Path getDeletedFilesPath() {
        return EasyInstallClient.getGameDirAsFile().toPath().resolve("easy_install/deleted_files.json");
    }

    private static Path getConfigDir() {
        return EasyInstallClient.getConfigDir().resolve("easy_install.json");
    }
    public static void addDeletedFile(String file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            JsonArray jsonArray = getDeletedFiles();
            jsonArray.add(file);
            Files.write(getDeletedFilesPath(), gson.toJson(jsonArray).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setButtonPlacementConfig(String buttonPlacement) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            JsonObject jsonObject = getConfig();
            jsonObject.addProperty("config_button_placement", buttonPlacement);

            Files.write(getConfigDir(), gson.toJson(jsonObject).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static ConfigButtonPlacement getButtonPlacementConfig() {
        JsonObject json = getConfig();
        ConfigButtonPlacement defaultConfig = ConfigButtonPlacement.getDefaultConfigButtonPlacement();
        if (json == null) {
            return defaultConfig;
        }
        JsonElement configButtonPlacement = json.get("config_button_placement");
        if (configButtonPlacement == null) {
            return defaultConfig;
        }
        return ConfigButtonPlacement.fromType(configButtonPlacement.getAsString());
    }


    public static JsonObject getConfig() {
        JsonElement jsonResult = parseJson(getConfigDir());
        if (jsonResult == null) {
            return new JsonObject();
        }
        return jsonResult.getAsJsonObject();
    }
    public static JsonArray getDeletedFiles() {
        JsonElement jsonResult = parseJson(getDeletedFilesPath());
        if (jsonResult == null) {
            return new JsonArray();
        }

        return jsonResult.getAsJsonArray();
    }

    private static JsonElement parseJson(Path path) {
        if (Files.notExists(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File file = path.toFile();
        if (!file.exists()) {
            return null;
        }
        JsonElement jsonElement = null;
        try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
            jsonElement = new JsonParser().parse(bufferedReader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonElement;
    }

    public static void clearDeletedFiles() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.write(getDeletedFilesPath(), gson.toJson(new JsonArray()).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
