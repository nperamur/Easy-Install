package neelesh.easy_install;

import com.google.gson.JsonArray;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Version {
    private final String name;
    private final String versionNumber;
    private final String versionType;
    private final URL downloadUrl;
    private final int numDownloads;
    private final ProjectType projectType;
    private final JsonArray dependencies;
    private final String id;
    private final String filename;
    private final String hash;
    private final String changelog;
    private final JsonArray gameVersions;
    private int fileSize;

    public Version(String name, String versionNumber, String versionType, URL downloadUrl, int numDownloads, ProjectType projectType, String filename, JsonArray gameVersions, JsonArray dependencies, String hash, String id, String changelog, int fileSize) {
        this.name = name;
        this.versionNumber = versionNumber;
        this.versionType = versionType;
        this.downloadUrl = downloadUrl;
        this.numDownloads = numDownloads;
        this.projectType = projectType;
        this.filename = filename;
        this.dependencies = dependencies;
        this.hash = hash;
        this.id = id;
        this.changelog = changelog;
        this.gameVersions = gameVersions;
        this.fileSize = fileSize;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getName() {
        return name;
    }

    public String getVersionType() {
        return versionType;
    }




    public int getNumDownloads() {
        return numDownloads;
    }

    public String getHash() {
        return this.hash;
    }

    public String getFilename() {
        return filename;
    }

    public String getId() {
        return id;
    }

    public void download(boolean installDependencies) {
        int numberOfThreads = 5;
        try (ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads)) {
            executorService.submit(() -> EasyInstallClient.downloadVersion(this.downloadUrl, this.filename, this.projectType));
            if (installDependencies) {
                for (int i = 0; i < this.dependencies.size(); i++) {
                    if (this.dependencies.get(i).getAsJsonObject().get("dependency_type").getAsString().equals("required")) {
                        String id = this.dependencies.get(i).getAsJsonObject().get("project_id").getAsString();
                        executorService.submit(() -> EasyInstallClient.downloadVersion(id, EasyInstallClient.getProjectType(id), true));
                    }
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



    @Override
    public String toString() {
        return "Version{" +
                "name='" + name + '\'' +
                ", versionNumber='" + versionNumber + '\'' +
                ", versionType='" + versionType + '\'' +
                ", downloadUrl=" + downloadUrl +
                ", numDownloads=" + numDownloads +
                ", projectType=" + projectType +
                ", dependencies=" + dependencies +
                ", id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", hash='" + hash + '\'' +
                ", changelog='" + changelog + '\'' +
                '}';
    }

    public String getChangelog() {
        return changelog;
    }

    public JsonArray getDependencies() {
        return dependencies;
    }

    public JsonArray getGameVersions() {
        return gameVersions;
    }

    public int getFileSize() {
        return this.fileSize;
    }
}
