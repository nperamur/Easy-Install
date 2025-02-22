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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EasyInstallClient implements ClientModInitializer {
	private static int rowsOnPage = 20;
	private static ProjectInfo[] projectInfo = new ProjectInfo[100];
	private static String GAME_VERSION;
	private static Path dataPackTempDir;
	private static int totalPages;
	private static String sortMethod = "Relevance";
	private static int numRows = 20;
	private static HashMap<String, String> oldHashes = new HashMap<>();
	private static int numUpdates;
	private static HashMap<ProjectType, HashSet<String>> updatesNeeded = new HashMap<>();
	private static HashMap<ProjectType, HashSet<String>> installedProjects = new HashMap<>();

    public static String getSortMethod() {
        return sortMethod;
    }

    public static void setSortMethod(String sortMethod) {
        EasyInstallClient.sortMethod = sortMethod;
    }


	public static int getNumUpdates() {
		return numUpdates;
	}

	public static void setNumUpdates(int n) {
		numUpdates = n;
	}

	@Override
	public void onInitializeClient() {
		GAME_VERSION = SharedConstants.getGameVersion().getName();
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
		installedProjects.put(ProjectType.DATA_PACK, new HashSet<>());
		updatesNeeded.put(ProjectType.DATA_PACK, new HashSet<>());
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

	public static ProjectInfo[] getProjectInformation() {
		return projectInfo;
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

		int numberOfThreads = 5;
		try(ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads)) {
			try {
				URL versionURL = URI.create(jsonObject.get("url").getAsString()).toURL();
				executorService.submit(() -> downloadVersion(versionURL, filename, projectType));
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
			JsonArray dependencies = JsonParser.parseString(response).getAsJsonArray().get(0).getAsJsonObject().get("dependencies").getAsJsonArray();
			for (int i = 0; i < dependencies.size(); i++) {
				JsonObject dependency = dependencies.get(i).getAsJsonObject();
				if (dependency.get("dependency_type").getAsString().equals("required")) {
					String id = dependency.get("project_id").getAsString();
					executorService.submit(() -> downloadVersion(id, getProjectType(id)));

				}
			}
			executorService.shutdown();
			try {
				executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
			}
			httpURLConnection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }


	public static void checkStatus(ProjectType projectType) {
		HashSet<String> hashes = getFileHashes(projectType);
		if (Thread.currentThread().isInterrupted() || hashes == null) {
			return;
		}
        String response = getUpdates(hashes, projectType);
        assert response != null;
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
		HashSet<String> installedProjectIds = new HashSet<>();
		HashSet<String> updateNeededProjectIds = new HashSet<>();
		HashMap<String, String> hashMap = new HashMap<>();
		oldHashes.clear();
		for (String hash : hashes) {
			if (jsonObject.get(hash) != null) {
				String h = jsonObject.get(hash).getAsJsonObject().get("files").getAsJsonArray().get(0).getAsJsonObject().get("hashes").getAsJsonObject().get("sha1").getAsString();
				oldHashes.put(hash, h);
				String projectId = jsonObject.get(hash).getAsJsonObject().get("project_id").getAsString();
				if (hashes.contains(h)) {
					installedProjectIds.add(projectId);
				} else {
					updateNeededProjectIds.add(projectId);
					hashMap.put(projectId, h);
				}
			}
		}

		numUpdates = updateNeededProjectIds.size();
		updatesNeeded.put(projectType, updateNeededProjectIds);
		installedProjects.put(projectType, installedProjectIds);
		for (ProjectInfo info : projectInfo) {
			if (info != null) {
				info.setInstalled(installedProjectIds.contains(info.getId()));
				if (updateNeededProjectIds.contains(info.getId())) {
					info.setLatestHash(hashMap.get(info.getId()));
				}
				info.setUpdated(!updateNeededProjectIds.contains(info.getId()));
			} else {
				break;
			}
		}

	}

	public static void downloadVersion(URL url, String fileName, ProjectType projectType) {
		String savePath = getSavePath(projectType, fileName).toString();
		try {
			try (InputStream in = new BufferedInputStream(url.openStream());
				 FileOutputStream out = new FileOutputStream(savePath)) {
				in.transferTo(out);
			}
            EasyInstall.LOGGER.info("Download complete: {}", savePath);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void initializeProject(String urlString, ProjectType projectType) {
		int rows = 0;
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("GET");
			httpURLConnection.setConnectTimeout(5000);
			int responseCode = httpURLConnection.getResponseCode();
			String response;
			if (responseCode == httpURLConnection.HTTP_OK) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
					response = reader.lines().collect(Collectors.joining("\n"));
				}
			} else {
				numRows = 0;
				totalPages = 1;
				return;
			}
			httpURLConnection.disconnect();
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			for (int x = 0; x < rowsOnPage; x++) {
				JsonObject jsonObject;
				try {
					jsonObject = JsonParser.parseString(response).getAsJsonObject().get("hits").getAsJsonArray().get(x).getAsJsonObject();
				} catch (Exception e) {
					projectInfo[x] = null;
					continue;
				}
				try {
					projectInfo[x] = new ProjectInfo(new URL(jsonObject.get("icon_url").getAsString()),
							jsonObject.get("title").getAsString(),
							jsonObject.get("description").getAsString(),
							jsonObject.get("author").getAsString(),
							jsonObject.get("slug").getAsString(),
							jsonObject.get("project_id").getAsString(),
							false, projectType);
				} catch (MalformedURLException e) {
					projectInfo[x] = new ProjectInfo(null,
							jsonObject.get("title").getAsString(),
							jsonObject.get("description").getAsString(),
							jsonObject.get("author").getAsString(),
							jsonObject.get("slug").getAsString(),
							jsonObject.get("project_id").getAsString(),
							false, projectType);

				}
				projectInfo[x].setInstalled(installedProjects.get(projectType).contains(projectInfo[x].getId()));
				projectInfo[x].setUpdated(!updatesNeeded.get(projectType).contains(projectInfo[x].getId()));
				rows++;
			}
			totalPages = (JsonParser.parseString(response).getAsJsonObject().get("total_hits").getAsInt() - 1) / rowsOnPage + 1;
			numRows = Math.max(0, rows);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}



	public static void search(String query, ProjectType projectType, int offset, HashSet<String> categories) {
		StringBuilder categoriesString = new StringBuilder();
		for (String category : categories) {
            categoriesString.append(",[\"categories:").append(category).append("\"]");
		}
		String encodedFacets;
		encodedFacets = switch(projectType) {
			case MOD -> URLEncoder.encode(String.format("[[\"categories:fabric\"],[\"versions:%s\"],[\"project_type:mod\"]" + categoriesString + "]", GAME_VERSION), StandardCharsets.UTF_8);
			case RESOURCE_PACK -> URLEncoder.encode(String.format("[[\"versions:%s\"],[\"project_type:resourcepack\"]" + categoriesString + "]", GAME_VERSION), StandardCharsets.UTF_8);
			case DATA_PACK -> URLEncoder.encode(String.format("[[\"versions:%s\"],[\"project_type:datapack\"]" + categoriesString + "]", GAME_VERSION), StandardCharsets.UTF_8);
			case SHADER -> URLEncoder.encode(String.format("[[\"versions:%s\"],[\"project_type:shader\"],[\"categories:iris\"]" + categoriesString + "]", GAME_VERSION), StandardCharsets.UTF_8);
		};

		String urlString = "https://api.modrinth.com/v2/search?limit=" + rowsOnPage + "&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&facets=" + encodedFacets + "&offset=" + offset + "&index=" + sortMethod.toLowerCase();
		initializeProject(urlString, projectType);
	}


	public static void search(String query, ProjectType projectType) {
		search(query, projectType, 0, new HashSet<>());
	}

	public static String createFileHash(Path path) throws IOException {
		File file = new File(path.toString());
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
			String jsonInputString = buildUpdateRequestBody(hashes, projectType);
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

	private static String buildUpdateRequestBody(HashSet<String> hashes, ProjectType projectType) {
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
        return jsonObject.toString();
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
		Set<String> hashes = ConcurrentHashMap.newKeySet();
		int numberOfThreads;
		numberOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2 - 2);
		try (ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads)) {
            assert files != null;
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
            executorService.shutdown();
			try {
				executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				executorService.shutdownNow();
				return null;
			}
		}
        return new HashSet<>(hashes);
	}

	public static JsonArray getCategoryTags(ProjectType projectType) {
		String urlString = "https://api.modrinth.com/v2/tag/category";
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("GET");
			int responseCode = httpURLConnection.getResponseCode();
			if (responseCode == httpURLConnection.HTTP_OK) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
					String response = reader.lines().collect(Collectors.joining("\n"));
					JsonArray arr = JsonParser.parseString(response).getAsJsonArray();
					JsonArray finalArr = new JsonArray();
					for (int i = 0; i < arr.size(); i++) {
						String type = arr.get(i).getAsJsonObject().get("project_type").toString();
						if (type.equals("\"shader\"") && projectType == ProjectType.SHADER
						|| type.equals("\"mod\"") && projectType == ProjectType.MOD
						|| type.equals("\"resourcepack\"") && projectType == ProjectType.RESOURCE_PACK
						|| type.equals("\"mod\"") && projectType == ProjectType.DATA_PACK) {
							finalArr.add(arr.get(i));
						}
					}
					return finalArr;
				}
			}
			httpURLConnection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new JsonArray();
	}


	public static JsonObject getUserProfile(String name) {
		try {
			URL url = URI.create("https://api.modrinth.com/v2/user/" + name).toURL();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
					String response = reader.lines().collect(Collectors.joining("\n"));
					return JsonParser.parseString(response).getAsJsonObject();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }

	public static void deleteOldFiles(ProjectType projectType, String latestHash) {
		File dir = new File(EasyInstallClient.getDir(projectType));
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				try {
					String hash = EasyInstallClient.createFileHash(file.toPath());
					if (oldHashes.containsKey(hash) && oldHashes.get(hash).equals(latestHash)) {
						boolean deleted = file.delete();
						if (!deleted) {
							EasyInstallJsonHandler.addDeletedFile(String.valueOf(file.toPath()));
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}

