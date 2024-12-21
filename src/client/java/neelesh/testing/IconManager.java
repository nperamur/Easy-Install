package neelesh.testing;

import com.mojang.logging.LogUtils;
import com.zakgof.webp4j.Webp4j;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class IconManager {
    public static Identifier loadIcon(ModInfo info, Identifier textureId, MinecraftClient client) {
        try {
            URL url = info.getIconUrl();
            if (url == null) {
                client.execute(() -> {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(new NativeImage(64, 64, false));
                    for (int x = 0; x < 64; x++) {
                        for (int j = 0; j < 64; j++) {
                            texture.getImage().setColorArgb(x, j, 0xFF000000);
                        }
                    }
                    texture.upload();
                    client.getTextureManager().registerTexture(textureId, texture);
                });
                return null;
            }
            boolean isWebp = url.toString().substring(url.toString().length()-4).equals("webp");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            System.out.println(url);
            try (InputStream inputStream = connection.getInputStream()) {
                NativeImage image = loadImage(inputStream, isWebp, true);
                TextureManager textureManager = client.getTextureManager();
                client.execute(() -> {

                    try {
                        NativeImageBackedTexture texture;
                        texture = new NativeImageBackedTexture(image);
                        texture.upload();
                        textureManager.registerTexture(textureId, texture);
                    } catch (Throwable var3) {
                        image.close();
                        throw var3;
                    }
                });

            } finally {
                connection.disconnect(); // Disconnect the connection
            }
        } catch (IOException e) {
            LogUtils.getLogger().error("Failed to load image for IconWidget: {}", e.getMessage());
        }
        return textureId;
    }
    private static NativeImage loadImage(InputStream input, boolean webp, boolean scaled) {
        try {


            BufferedImage bufferedImage;

            if (webp) {
                try {
                    bufferedImage = Webp4j.decode(input.readAllBytes());
                } catch (Exception e) {
                    bufferedImage = null;
                }
            } else {

                bufferedImage = ImageIO.read(input);
            }
            if (bufferedImage == null) {
                System.err.println("Failed to read image.");
                if (!scaled) {
                    return null;
                }
                bufferedImage = new BufferedImage(1500, 1500, BufferedImage.TYPE_INT_RGB);
                bufferedImage.createGraphics();
            }
            NativeImage nativeImage;
//            if (scaled) {
//                int newWidth = 200;
//                int newHeight = 200;
//                BufferedImage scaledImage = bufferedImage;
//                nativeImage = new NativeImage(scaledImage.getWidth(), scaledImage.getHeight(), false);
//                for (int x = 0; x < scaledImage.getWidth(); x++) {
//                    for (int y = 0; y < scaledImage.getHeight(); y++) {
//                        int rgb = scaledImage.getRGB(x, y);
//                        nativeImage.setColorArgb(x, y, rgb);
//                    }
//                }
//                return nativeImage;
//            }
            nativeImage = new NativeImage(bufferedImage.getWidth(), bufferedImage.getHeight(), false);
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                for (int y = 0; y < bufferedImage.getHeight(); y++) {
                    int rgb = bufferedImage.getRGB(x, y);
                    nativeImage.setColorArgb(x, y, rgb);
                }
            }
            return nativeImage;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


    }
    private static BufferedImage scaleImage(BufferedImage original, int newWidth, int newHeight) {
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();

        // Draw the original image scaled to the new size
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return scaledImage;
    }
    public static NativeImage loadIcon(URL url, Identifier textureId, MinecraftClient client) {
        try {
            if (url == null) {
                client.execute(() -> {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(new NativeImage(1, 1, false));
                    texture.getImage().setColorArgb(1, 1, 0xFF000000);
                    texture.upload();
                    client.getTextureManager().registerTexture(textureId, texture);
                });
                return null;
            }
            boolean isWebp = url.toString().substring(url.toString().length() - 4).equals("webp");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            System.out.println(url);
            try (InputStream inputStream = connection.getInputStream()) {
                NativeImage image = loadImage(inputStream, isWebp, false);
                if (image == null) {
                    return null;
                }
                TextureManager textureManager = client.getTextureManager();
                client.execute(() -> {

                    try {
                        NativeImageBackedTexture texture;
                        texture = new NativeImageBackedTexture(image);

                        texture.upload();
                        textureManager.registerTexture(textureId, texture);
                    } catch (Throwable var3) {
                        image.close();
                        throw var3;
                    }
                });
                connection.disconnect();
                return image;
            } finally {
                connection.disconnect(); // Disconnect the connection
            }
        } catch (IOException e) {
            LogUtils.getLogger().error("Failed to load image for IconWidget: {}", e.getMessage());
        }
        return null;
    }
}
