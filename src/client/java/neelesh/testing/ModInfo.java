package neelesh.testing;

import java.net.URL;

public class ModInfo {
    private URL iconUrl;
    private String title;
    private String description;
    private String author;
    private String slug;

    private boolean installed;
    private String body;
    private ProjectType projectType;

    public ModInfo(URL iconUrl, String title, String description, String author, String slug, boolean installed, ProjectType projectType) {
        this.iconUrl = iconUrl;
        this.title = title;
        this.description = description;
        this.author = author;
        this.slug = slug;
        this.installed = installed;
        this.body = "";
        this.projectType = projectType;
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
}
