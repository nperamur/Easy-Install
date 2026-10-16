package neelesh.easy_install;

import com.google.gson.JsonArray;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static neelesh.easy_install.EasyInstallClient.getProjectType;

public class Version {
    private final String name;
    private final String versionNumber;
    private final String versionType;
    private final URL downloadUrl;
    private final int numDownloads;
    private final ProjectType projectType;
    private final JsonArray dependencies;
    private final String projectId;
    private final String filename;
    private final String hash;
    private final String changelog;
    private final JsonArray gameVersions;
    private int fileSize;

    public Version(String name, String versionNumber, String versionType, URL downloadUrl, int numDownloads, ProjectType projectType, String filename, JsonArray gameVersions, JsonArray dependencies, String hash, String projectId, String changelog, int fileSize) {
        this.name = name;
        this.versionNumber = versionNumber;
        this.versionType = versionType;
        this.downloadUrl = downloadUrl;
        this.numDownloads = numDownloads;
        this.projectType = projectType;
        this.filename = filename;
        this.dependencies = dependencies;
        this.hash = hash;
        this.projectId = projectId;
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

    public String getProjectId() {
        return projectId;
    }

    public boolean download(boolean installDependencies) {
        int numberOfThreads = 5;
        AtomicBoolean successful = new AtomicBoolean(true);
        try (ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads)) {
            executorService.submit(() -> {
                EasyInstallClient.getInstallationState(projectType).addInstallingHash(hash);
                successful.set(EasyInstallClient.downloadVersion(this.downloadUrl, this.filename, this.projectType));
            });
            if (installDependencies) {
                for (int i = 0; i < this.dependencies.size(); i++) {
                    if (this.dependencies.get(i).getAsJsonObject().get("dependency_type").getAsString().equals("required")) {
                        String id = this.dependencies.get(i).getAsJsonObject().get("project_id").getAsString();
                        ProjectType dependencyProjectType = getProjectType(id);
                        if (!EasyInstallClient.installedProject(dependencyProjectType, id)) {
                            EasyInstallClient.getInstallationState(projectType).setProjectStatus(id, ProjectStatus.INSTALLING_REGULAR);
                            executorService.submit(() ->{
                                boolean dependencyDownloadSuccess = EasyInstallClient.downloadVersion(id, dependencyProjectType, true);
                                if (dependencyDownloadSuccess) {
                                    EasyInstallClient.getInstallationState(projectType).setProjectStatus(id, ProjectStatus.INSTALLED);
                                } else {
                                    EasyInstallClient.getInstallationState(projectType).removeProject(id);
                                }
                            });
                        }
                    }
                }
            }
            executorService.shutdown();
            try {
                executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            EasyInstallClient.getInstallationState(projectType).removeInstallingHash(hash);
        }
        return successful.get();
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
                ", id='" + projectId + '\'' +
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
