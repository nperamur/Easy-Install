package neelesh.easy_install;

import java.net.URL;

public class ProjectInfo {
    private URL iconUrl;
    private String title;
    private String description;
    private String author;
    private String slug;

    private boolean installed;
    private String body;
    private ProjectType projectType;
    private boolean updated;
    private String id;
    private String latestHash;
    private boolean installing;

    public ProjectInfo(URL iconUrl, String title, String description, String author, String slug, String id, boolean installed, ProjectType projectType) {
        this.iconUrl = iconUrl;
        this.title = title;
        this.description = description;
        this.author = author;
        this.slug = slug;
        this.installed = installed;
        this.id = id;
        this.body = "";
        this.projectType = projectType;
        this.updated = true;
    }

    public URL getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(URL iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getTitle() {
        return title;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public String getId() {
        return id;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public void setLatestHash(String hash) {
        this.latestHash = hash;
    }

    public String getLatestHash() {
        return latestHash;
    }

    public void setInstalling(boolean installing) {
        this.installing = installing;
    }

    public boolean isInstalling() {
        return this.installing;
    }
}
