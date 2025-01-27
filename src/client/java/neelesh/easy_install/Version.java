package neelesh.easy_install;

import com.google.gson.JsonArray;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Version {
    private String name;
    private String versionNumber;
    private String versionType;
    private URL downloadUrl;
    private int numDownloads;
    private ProjectType projectType;
    private JsonArray dependencies;
    private String id;
    private String filename;
    private String hash;

    public Version(String name, String versionNumber, String versionType, URL downloadUrl, int numDownloads, ProjectType projectType, String filename, JsonArray dependencies, String hash, String id) {
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

    public void download() {
        EasyInstallClient.downloadVersion(this.downloadUrl, this.filename, this.projectType);
//        if (this.projectType.equals(ProjectType.MOD)) {
        int numberOfThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        for(int i = 0; i < this.dependencies.size(); i++) {
            if (this.dependencies.get(i).getAsJsonObject().get("dependency_type").getAsString().equals("required")) {
                String id = this.dependencies.get(i).getAsJsonObject().get("project_id").getAsString();
                executorService.submit(() -> EasyInstallClient.downloadVersion(id, EasyInstallClient.getProjectType(id)));
            }
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return numDownloads == version.numDownloads && Objects.equals(name, version.name) && Objects.equals(versionNumber, version.versionNumber) && Objects.equals(versionType, version.versionType) && Objects.equals(downloadUrl, version.downloadUrl) && projectType == version.projectType && Objects.equals(dependencies, version.dependencies) && Objects.equals(id, version.id) && Objects.equals(filename, version.filename) && Objects.equals(hash, version.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, versionNumber, versionType, downloadUrl, numDownloads, projectType, dependencies, id, filename, hash);
    }
}
