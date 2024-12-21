package neelesh.testing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;

public class TestingClient implements ClientModInitializer {
	public static final int ROWS_ON_PAGE = 100;
	private static ModInfo[] modInfo = new ModInfo[ROWS_ON_PAGE];
	private static final String GAME_VERSION = SharedConstants.getGameVersion().getName();
	private static Path dataPackTempDir;

	@Override
	public void onInitializeClient() {
		System.out.println("HELLOWORLD");
	}


	public static ModInfo[] getModInformation() {
		return modInfo;
	}

	public static void setDataPackTempDir(Path path) {
		dataPackTempDir = path;
	}

	public static Path getDataPackTempDir() {
		return dataPackTempDir;
	}

	public static void downloadVersion(String slug, ProjectType projectType) {
		String response = getVersions(slug, projectType);
		JsonObject jsonObject = JsonParser.parseString(response).getAsJsonArray().get(0).getAsJsonObject().get("files").getAsJsonArray().get(0).getAsJsonObject();
		String filename = jsonObject.get("filename").getAsString();
		try {
			URL versionURL = URI.create(jsonObject.get("url").getAsString()).toURL();
			downloadVersion(versionURL, filename, projectType);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		if (projectType.equals(ProjectType.MOD)) {
			JsonArray dependencies = JsonParser.parseString(response).getAsJsonArray().get(0).getAsJsonObject().get("dependencies").getAsJsonArray();
			for (int i = 0; i < dependencies.size(); i++) {
				JsonObject dependency = dependencies.get(i).getAsJsonObject();
				if (dependency.get("dependency_type").getAsString().equals("required")) {
					downloadVersion(dependency.get("project_id").getAsString(), projectType);
				}
			}
		}
	}
	public static boolean isInstalled(String slug, ProjectType projectType) {
		String response = getVersions(slug, projectType);
        assert response != null;
        String fileName = JsonParser.parseString(response).getAsJsonArray().get(0).getAsJsonObject().get("files").getAsJsonArray().get(0).getAsJsonObject().get("filename").getAsString();
		Path path = switch(projectType) {
			case MOD -> Paths.get(FabricLoader.getInstance().getGameDir() + "/mods", fileName);
			case RESOURCE_PACK -> Paths.get(FabricLoader.getInstance().getGameDir() + "/resourcepacks", fileName);
			case DATA_PACK -> Paths.get(dataPackTempDir.toString(), fileName);
			case SHADER -> Paths.get(FabricLoader.getInstance().getGameDir() + "/shaderpacks", fileName);
		};
		String hash1 = JsonParser.parseString(response).getAsJsonArray().get(0).getAsJsonObject().get("files").getAsJsonArray().get(0).getAsJsonObject().get("hashes").getAsJsonObject().get("sha512").getAsString();
		String hash2;
		try {
			hash2 = createFileHash(path);
		} catch (IOException e) {
            return false;
        }
        return hash1.equals(hash2);
	}


	public static String getVersions(String slug, ProjectType projectType) {
		String urlString = switch(projectType) {
			case MOD -> "https://api.modrinth.com/v2/project/" + slug + "/version?loaders=" + URLEncoder.encode("[\"fabric\"]") + "&game_versions=" + URLEncoder.encode(String.format("[\"%s\"]", GAME_VERSION));
			case DATA_PACK -> "https://api.modrinth.com/v2/project/" + slug + "/version?loaders=" + URLEncoder.encode("[\"datapack\"]") + "&game_versions=" + URLEncoder.encode(String.format("[\"%s\"]", GAME_VERSION));
			default -> "https://api.modrinth.com/v2/project/" + slug + "/version?game_versions=" + URLEncoder.encode(String.format("[\"%s\"]", GAME_VERSION));
		};

		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("GET");
			int responseCode = httpURLConnection.getResponseCode();
			if (responseCode == httpURLConnection.HTTP_OK) {
				String response = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
				return response;
			} else {
				System.out.println(responseCode);
			}
			httpURLConnection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }



	public static void updateInstalled(ProjectType projectType) {
		for (int i = 0; i < modInfo.length; i++) {
			if (modInfo[i] != null && !Thread.currentThread().isInterrupted()) {
                modInfo[i].setInstalled(isInstalled(modInfo[i].getSlug(), projectType));
			} else {
				break;
			}
		}
	}

	public static void downloadVersion(URL url, String fileName, ProjectType projectType) {
		String savePath = switch(projectType) {
			case MOD -> Paths.get(FabricLoader.getInstance().getGameDir() + "/mods",  fileName).toString();
			case RESOURCE_PACK -> Paths.get(FabricLoader.getInstance().getGameDir() + "/resourcepacks",  fileName).toString();
			case DATA_PACK -> Paths.get(dataPackTempDir.toString(), fileName).toString();
			case SHADER -> Paths.get(FabricLoader.getInstance().getGameDir() + "/shaderpacks",  fileName).toString();
		};

		try {
			try (InputStream in = new BufferedInputStream(url.openStream());
				 FileOutputStream out = new FileOutputStream(savePath)) {

				byte[] dataBuffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(dataBuffer, 0, dataBuffer.length)) != -1) {
					out.write(dataBuffer, 0, bytesRead);
				}
			}
			System.out.println("Download complete: " + savePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void initializeModInfo(String urlString, ProjectType projectType) {
		System.out.println("HI");
		try {
			URL url2 = URI.create(urlString).toURL();
			HttpURLConnection httpURLConnection = (HttpURLConnection) url2.openConnection();
			httpURLConnection.setRequestMethod("GET");
			httpURLConnection.setConnectTimeout(5000);
			int responseCode2 = httpURLConnection.getResponseCode();
			String response2;
			if (responseCode2 == httpURLConnection.HTTP_OK) {
				response2 = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
			} else {
				throw new RuntimeException();
			}
			httpURLConnection.disconnect();
			for (int x = 0; x < ROWS_ON_PAGE; x++) {
				JsonObject jsonObject;
				try {
					jsonObject = JsonParser.parseString(response2).getAsJsonObject().get("hits").getAsJsonArray().get(x).getAsJsonObject();
				} catch (Exception e) {
					modInfo[x] = null;
					continue;
				}
				try {
					modInfo[x] = new ModInfo(new URL(jsonObject.get("icon_url").getAsString()),
							jsonObject.get("title").getAsString(),
							jsonObject.get("description").getAsString(),
							jsonObject.get("author").getAsString(),
							jsonObject.get("slug").getAsString(),
							false, projectType);
				} catch (MalformedURLException e) {
					modInfo[x] = new ModInfo(null,
							jsonObject.get("title").getAsString(),
							jsonObject.get("description").getAsString(),
							jsonObject.get("author").getAsString(),
							jsonObject.get("slug").getAsString(),
							false, projectType);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}



	public static void search(String query, ProjectType projectType) {
		String encodedFacets;
		encodedFacets = switch(projectType) {
			case MOD -> URLEncoder.encode(String.format("[[\"categories:fabric\"],[\"versions:%s\"],[\"project_type:mod\"]]", GAME_VERSION), StandardCharsets.UTF_8);
			case RESOURCE_PACK -> URLEncoder.encode(String.format("[[\"versions:%s\"],[\"project_type:resourcepack\"]]", GAME_VERSION), StandardCharsets.UTF_8);
			case DATA_PACK -> URLEncoder.encode(String.format("[[\"versions:%s\"],[\"project_type:datapack\"]]", GAME_VERSION), StandardCharsets.UTF_8);
			case SHADER -> URLEncoder.encode(String.format("[[\"versions:%s\"],[\"project_type:shader\"],[\"categories:iris\"]]", GAME_VERSION), StandardCharsets.UTF_8);
		};
		String urlString = "https://api.modrinth.com/v2/search?limit=" + ROWS_ON_PAGE + "&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&facets="+encodedFacets;
		initializeModInfo(urlString, projectType);
	}


	public static String createFileHash(Path path) throws IOException {
		FileInputStream file = new FileInputStream(String.valueOf(path));
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			messageDigest = null;
			e.printStackTrace();
		}
		byte[] bytes = new byte[4096];
		int numBytes = 0;
		while (numBytes != -1) {
			messageDigest.update(bytes, 0, numBytes);
			numBytes = file.read(bytes);
		}
		bytes = messageDigest.digest();
		StringBuilder stringBuilder = new StringBuilder();
		for (byte b : bytes) {
			stringBuilder.append(String.format("%02x", b));
		}
		return stringBuilder.toString();


	}
}
