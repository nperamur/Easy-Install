package neelesh.easy_install;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EasyInstallClient implements ClientModInitializer {
	private static int rowsOnPage = 20;
	private static ModInfo[] modInfo = new ModInfo[100];
	private static final String GAME_VERSION = SharedConstants.getGameVersion().getName();
	private static Path dataPackTempDir;
	private static int totalPages;
	private static String sortMethod = "Relevance";
	private static int numRows = 20;
	private static HashMap<String, String> oldHashes = new HashMap<>();
	private static int numUpdates;

    public static String getSortMethod() {
        return sortMethod;
    }

    public static void setSortMethod(String sortMethod) {
        EasyInstallClient.sortMethod = sortMethod;
    }

	public static HashMap<String, String> getOldHashes() {
		return oldHashes;
	}

	public static int getNumUpdates() {
		return numUpdates;
	}

	public static void setNumUpdates(int n) {
		numUpdates = n;
	}

	@Override
	public void onInitializeClient() {
		System.out.println("HELLOWORLD");
	}

	public static int getRowsOnPage() {
		return rowsOnPage;
	}

	public static int getNumRows() {
		return numRows;
	}

	public static void setRowsOnPage(int rows) {
		rowsOnPage = rows;
	}

	public static ModInfo[] getModInformation() {
		return modInfo;
	}

	public static void setDataPackTempDir(Path path) {
		dataPackTempDir = path;
	}

	public static int getTotalPages() {
		return totalPages;
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
//		if (projectType.equals(ProjectType.MOD)) {
		int numberOfThreads = 5;
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

		JsonArray dependencies = JsonParser.parseString(response).getAsJsonArray().get(0).getAsJsonObject().get("dependencies").getAsJsonArray();
			for (int i = 0; i < dependencies.size(); i++) {
				JsonObject dependency = dependencies.get(i).getAsJsonObject();
				if (dependency.get("dependency_type").getAsString().equals("required")) {
					String id = dependency.get("project_id").getAsString();
					executorService.submit(() -> downloadVersion(id, getProjectType(id)));

				}
			}
//		}
		executorService.shutdown();
	}

	public static ProjectType getProjectType(String id) {
		String urlString = "https://api.modrinth.com/v2/project/" + id;
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("GET");
			int responseCode = httpURLConnection.getResponseCode();
			if (responseCode == httpURLConnection.HTTP_OK) {
				String response;
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
					response = reader.lines().collect(Collectors.joining("\n"));
				}
				String projectType = JsonParser.parseString(response).getAsJsonObject().get("project_type").getAsString();
				return switch (projectType) {
					case "mod" -> ProjectType.MOD;
					case "datapack" -> ProjectType.DATA_PACK;
					case "resourcepack" -> ProjectType.RESOURCE_PACK;
					case "shader" -> ProjectType.SHADER;
					default -> null;
				};
			} else {
				System.out.println(responseCode);
			}
			httpURLConnection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	public static String getVersions(String slug, ProjectType projectType) {
		String urlString = switch(projectType) {
			case MOD -> "https://api.modrinth.com/v2/project/" + slug + "/version?loaders=" + URLEncoder.encode("[\"fabric\"]") + "&game_versions=" + URLEncoder.encode(String.format("[\"%s\"]", GAME_VERSION));
			case DATA_PACK -> "https://api.modrinth.com/v2/project/" + slug + "/version?loaders=" + URLEncoder.encode("[\"datapack\"]") + "&game_versions=" + URLEncoder.encode(String.format("[\"%s\"]", GAME_VERSION));
			case SHADER -> "https://api.modrinth.com/v2/project/" + slug + "/version?loaders=" + URLEncoder.encode("[\"iris\"]") + "&game_versions=" + URLEncoder.encode(String.format("[\"%s\"]", GAME_VERSION));
			default -> "https://api.modrinth.com/v2/project/" + slug + "/version?game_versions=" + URLEncoder.encode(String.format("[\"%s\"]", GAME_VERSION));
		};

		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("GET");
			int responseCode = httpURLConnection.getResponseCode();
			if (responseCode == httpURLConnection.HTTP_OK) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                    return reader.lines().collect(Collectors.joining("\n"));

                }
			} else {
				System.out.println(responseCode);
			}
			httpURLConnection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }


	public static void checkInstalled(ProjectType projectType) {
		HashSet<String> hashes = getFileHashes(projectType);
        assert hashes != null;
        String response = getUpdates(hashes, projectType);
        assert response != null;
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
		HashSet<String> installedProjectIds = new HashSet<>();
		HashSet<String> updateNeededProjectIds = new HashSet<>();
		HashMap<String, String> hashMap = new HashMap<>();
		oldHashes.clear();
		for (String hash : hashes) {
			if (jsonObject.get(hash) != null) {
				String h = jsonObject.get(hash).getAsJsonObject().get("files").getAsJsonArray().get(0).getAsJsonObject().get("hashes").getAsJsonObject().get("sha1").getAsString();
				oldHashes.put(hash, h);
				if (hashes.contains(h)) {
					installedProjectIds.add(jsonObject.get(hash).getAsJsonObject().get("project_id").getAsString());
				} else {
					updateNeededProjectIds.add(jsonObject.get(hash).getAsJsonObject().get("project_id").getAsString());
					hashMap.put(jsonObject.get(hash).getAsJsonObject().get("project_id").getAsString(), h);
				}
			}
		}

		numUpdates = updateNeededProjectIds.size();

		for (ModInfo info : modInfo) {
			if (info != null) {
				info.setInstalled(installedProjectIds.contains(info.getId()));
				if (updateNeededProjectIds.contains(info.getId())) {
					info.setLatestHash(hashMap.get(info.getId()));
				}
				info.setUpdated(!updateNeededProjectIds.contains(info.getId()));
			}
		}

	}

	public static void downloadVersion(URL url, String fileName, ProjectType projectType) {
		String savePath = getSavePath(projectType, fileName).toString();
		try {
			try (InputStream in = new BufferedInputStream(url.openStream());
				 FileOutputStream out = new FileOutputStream(savePath)) {
				byte[] dataBuffer = new byte[8192];
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
		int rows = 0;
		try {
			URL url2 = URI.create(urlString).toURL();
			HttpURLConnection httpURLConnection = (HttpURLConnection) url2.openConnection();
			httpURLConnection.setRequestMethod("GET");
			httpURLConnection.setConnectTimeout(5000);
			int responseCode2 = httpURLConnection.getResponseCode();
			String response2;
			if (responseCode2 == httpURLConnection.HTTP_OK) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
					response2 = reader.lines().collect(Collectors.joining("\n"));
				}
			} else {
				throw new RuntimeException();
			}
			httpURLConnection.disconnect();
			for (int x = 0; x < rowsOnPage; x++) {
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
							jsonObject.get("project_id").getAsString(),
							false, projectType);
				} catch (MalformedURLException e) {
					modInfo[x] = new ModInfo(null,
							jsonObject.get("title").getAsString(),
							jsonObject.get("description").getAsString(),
							jsonObject.get("author").getAsString(),
							jsonObject.get("slug").getAsString(),
							jsonObject.get("project_id").getAsString(),
							false, projectType);

				}
				rows++;
			}
			totalPages = (JsonParser.parseString(response2).getAsJsonObject().get("total_hits").getAsInt() - 1) / rowsOnPage + 1;
			numRows = Math.max(1, rows);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}



	public static void search(String query, ProjectType projectType, int offset) {
		String encodedFacets;
		encodedFacets = switch(projectType) {
			case MOD -> URLEncoder.encode(String.format("[[\"categories:fabric\"],[\"versions:%s\"],[\"project_type:mod\"]]", GAME_VERSION), StandardCharsets.UTF_8);
			case RESOURCE_PACK -> URLEncoder.encode(String.format("[[\"versions:%s\"],[\"project_type:resourcepack\"]]", GAME_VERSION), StandardCharsets.UTF_8);
			case DATA_PACK -> URLEncoder.encode(String.format("[[\"versions:%s\"],[\"project_type:datapack\"]]", GAME_VERSION), StandardCharsets.UTF_8);
			case SHADER -> URLEncoder.encode(String.format("[[\"versions:%s\"],[\"project_type:shader\"],[\"categories:iris\"]]", GAME_VERSION), StandardCharsets.UTF_8);
		};
		String urlString = "https://api.modrinth.com/v2/search?limit=" + rowsOnPage + "&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&facets=" + encodedFacets + "&offset=" + offset + "&index=" + sortMethod.toLowerCase();
		initializeModInfo(urlString, projectType);
	}


	public static void search(String query, ProjectType projectType) {
		search(query, projectType, 0);
	}

	public static String createFileHash(Path path) throws IOException {
		File file = new File(path.toString());
//		MessageDigest messageDigest;
		//		try {
//			messageDigest = MessageDigest.getInstance("SHA-512");
//		} catch (NoSuchAlgorithmException e) {
//			messageDigest = null;
//			e.printStackTrace();
//		}
//		byte[] bytes = new byte[8192];
//		int numBytes = 0;
//		while (numBytes != -1) {
//			messageDigest.update(bytes, 0, numBytes);
//			numBytes = file.read(bytes);
//		}
//		file.close();
//		bytes = messageDigest.digest();
//		StringBuilder stringBuilder = new StringBuilder();
//		for (byte b : bytes) {
//			stringBuilder.append(String.format("%02x", b));
//		}
		return Files.asByteSource(file).hash(Hashing.sha1()).toString();


	}

	public static ArrayList<Version> getUpdatedVersions(ProjectType projectType) {
		oldHashes.clear();
		HashSet<String> hashes = getFileHashes(projectType);
		String response = getUpdates(hashes, projectType);
        assert response != null;
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
		HashSet<Version> versions = new HashSet<>();
		for (String hash : hashes) {
			if (jsonObject.get(hash) != null) {
				String h = jsonObject.get(hash).getAsJsonObject().get("files").getAsJsonArray().get(0).getAsJsonObject().get("hashes").getAsJsonObject().get("sha1").getAsString();
				if (!hashes.contains(h)) {
					System.out.println(jsonObject.get(hash).getAsJsonObject().get("name").getAsString() + " needs an update!");
					JsonObject versionInfo = jsonObject.get(hash).getAsJsonObject();
					try {
						Version version = createVersion(versionInfo, projectType);
						versions.add(version);
						oldHashes.put(hash, version.getHash());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
		}
		numUpdates = versions.size();
		return new ArrayList<>(versions);
    }

	private static String getUpdates(HashSet<String> hashes, ProjectType projectType) {
		try {
			JsonObject jsonObject = new JsonObject();
			JsonArray hashArray = new JsonArray();
			for (String hash : hashes) {
				hashArray.add(hash);
			}
			jsonObject.add("hashes", hashArray);
			jsonObject.addProperty("algorithm", "sha1");
			JsonArray loaders = new JsonArray();
			switch(projectType) {
				case MOD -> loaders.add("fabric");
				case RESOURCE_PACK -> loaders.add("minecraft");
				case DATA_PACK -> loaders.add("datapack");
				case SHADER -> loaders.add("iris");
			}
			jsonObject.add("loaders", loaders);
			JsonArray gameVersions = new JsonArray();
			gameVersions.add(GAME_VERSION);
			jsonObject.add("game_versions", gameVersions);
			String jsonInputString = jsonObject.toString();
			URL url = URI.create("https://api.modrinth.com/v2/version_files/update").toURL();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(5000);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);
			try (OutputStream outputStream = connection.getOutputStream()) {
				byte[] inputBytes = jsonInputString.getBytes(StandardCharsets.UTF_8);
				outputStream.write(inputBytes, 0, inputBytes.length);
			}
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    return reader.lines().collect(Collectors.joining("\n"));
				}
			}
		} catch(MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Version createVersion(JsonObject versionInfo, ProjectType projectType) throws MalformedURLException {
		return new Version(versionInfo.get("name").getAsString(),
				versionInfo.get("version_number").getAsString(),
				versionInfo.get("version_type").getAsString(),
				new URL(versionInfo.get("files").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString()),
				versionInfo.get("downloads").getAsInt(),
				projectType,
				versionInfo.get("files").getAsJsonArray().get(0).getAsJsonObject().get("filename").getAsString(),
				versionInfo.get("dependencies").getAsJsonArray(),
				versionInfo.get("files").getAsJsonArray().get(0).getAsJsonObject().get("hashes").getAsJsonObject().get("sha1").getAsString(),
				versionInfo.get("project_id").getAsString()
		);
	}



	public static Path getSavePath(ProjectType projectType, String fileName) {
		return Paths.get(getDir(projectType), fileName);
	}

	public static String getDir(ProjectType projectType) {
		return switch(projectType) {
			case MOD -> FabricLoader.getInstance().getGameDir() + "/mods";
			case RESOURCE_PACK -> FabricLoader.getInstance().getGameDir() + "/resourcepacks";
			case DATA_PACK -> dataPackTempDir.toString();
			case SHADER -> FabricLoader.getInstance().getGameDir() + "/shaderpacks";
		};
	}


	private static HashSet<String> getFileHashes(ProjectType projectType) {
		File dir = new File(getDir(projectType));
		File[] files = dir.listFiles();
		Set<String> hashes = Collections.synchronizedSet(new HashSet<>());
		int numberOfThreads;
		numberOfThreads = 5;
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
		if (files != null) {
			for (File file : files) {
				executorService.submit(() -> {
					try {
						if (!Thread.currentThread().isInterrupted()) {
							String hash = createFileHash(file.toPath());
							hashes.add(hash);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
		}
		executorService.shutdown();
		try {
			executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			executorService.shutdownNow();
            return null;
        }
        return new HashSet<>(hashes);
	}
}

