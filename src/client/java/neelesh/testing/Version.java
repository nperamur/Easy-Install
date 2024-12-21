package neelesh.testing;

import com.google.gson.JsonArray;

import java.net.URL;

public class Version {
    private String name;
    private String versionNumber;
    private String versionType;
    private URL downloadUrl;
    private int numDownloads;
    private ProjectType projectType;
    private JsonArray dependencies;

    public String getFilename() {
        return filename;
    }

    private String filename;
    private String hash;

    public Version(String name, String versionNumber, String versionType, URL downloadUrl, int numDownloads, ProjectType projectType, String filename, JsonArray dependencies, String hash) {
        this.name = name;
        this.versionNumber = versionNumber;
        this.versionType = versionType;
        this.downloadUrl = downloadUrl;
        this.numDownloads = numDownloads;
        this.projectType = projectType;
        this.filename = filename;
        this.dependencies = dependencies;
        this.hash = hash;
    }

    public Version() {

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


    public URL getDownloadUrl() {
        return downloadUrl;
    }


    public int getNumDownloads() {
        return numDownloads;
    }

    public String getHash() {
        return this.hash;
    }

    public void downloadVersion() {
        TestingClient.downloadVersion(this.downloadUrl, this.filename, this.projectType);
        if (this.projectType.equals(ProjectType.MOD)) {
            for(int i = 0; i < this.dependencies.size(); i++) {
                if (this.dependencies.get(i).getAsJsonObject().get("dependency_type").getAsString().equals("required")) {
                    TestingClient.downloadVersion(this.dependencies.get(i).getAsJsonObject().get("project_id").getAsString(), this.projectType);
                }
            }
        }
    }

}
