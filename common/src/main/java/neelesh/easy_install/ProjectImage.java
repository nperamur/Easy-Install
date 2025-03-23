package neelesh.easy_install;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

public class ProjectImage {
    private Identifier id;
    private NativeImage image;
    private int position;
    private int width;
    private String link;

    public ProjectImage(NativeImage image, Identifier id, int position) {
        this.image = image;
        this.id = id;
        this.position = position;
        this.width = -1;
    }

    public Identifier getId() {
        return id;
    }

    public NativeImage getImage() {
        return image;
    }

    public int getPosition() {
        return position;
    }

    public String getLink() {
        return link;
    }

    public boolean isClickable() {
        return this.link != null;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setWidth(int width) {
        this.width = width;
    }


    public int getWidth() {
        return width;
    }
}
