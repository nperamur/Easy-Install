package neelesh.easy_install;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.net.URL;

public class GalleryImage {
    private Identifier id;
    private URL url;
    private NativeImage image;
    private String description;
    private String title;

    public GalleryImage(Identifier id, URL url, String description) {
        this.id = id;
        this.url = url;
        this.description = description;
        this.title = "";
        System.out.println(description);
    }

    public GalleryImage(Identifier id, URL url) {
        this.id = id;
        this.url = url;
        this.description = "";
        this.title = "";
    }

    public URL getUrl() {
        return url;
    }

    public Identifier getId() {
        return id;
    }


    public void setImage(NativeImage image) {
        this.image = image;
    }

    public NativeImage getImage() {
        return this.image;
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }
}
