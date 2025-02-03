package neelesh.easy_install;

import com.kitfox.svg.app.beans.SVGIcon;
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
import java.net.URI;
import java.net.URL;

public class IconManager {

    public static Identifier loadIcon(ProjectInfo info, Identifier textureId, Thread thread) {
        MinecraftClient client = MinecraftClient.getInstance();
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
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            try (InputStream inputStream = connection.getInputStream()) {
                NativeImage image = loadImage(inputStream, isWebp, true);
                TextureManager textureManager = client.getTextureManager();
                if (!thread.isInterrupted()) {
                    client.execute(() -> {

                        try {
                            NativeImageBackedTexture texture;
                            texture = new NativeImageBackedTexture(image);
                            texture.upload();
                            textureManager.registerTexture(textureId, texture);
                            image.close();
                        } catch (Throwable var3) {
                            image.close();
                            throw var3;
                        }
                    });
                }

            } finally {
                connection.disconnect(); // Disconnect the connection
            }
        } catch (IOException e) {
            LogUtils.getLogger().error("Failed to load image: {}", e.getMessage());
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
    
    public static NativeImage loadIcon(URL url, Identifier textureId, MinecraftClient client) {
        NativeImage image = null;
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
            String fileEnd = url.toString().substring(url.toString().length() - 4);
            boolean isWebp = fileEnd.equals("webp");
            if (!fileEnd.equals(".svg")) {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("GET");
                try (InputStream inputStream = connection.getInputStream()) {
                    image = loadImage(inputStream, isWebp, false);
                    if (image == null) {
                        image = loadSvgImage(url.toURI());
                    }
                } finally {
                    connection.disconnect();
                }
            } else {
                image = loadSvgImage(url.toURI());
            }

        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to load image: {}", e.getMessage());
        }
        if (image == null) {
            return null;
        }
        TextureManager textureManager = client.getTextureManager();
        NativeImage finalImage = image;
        client.execute(() -> {

            try {
                NativeImageBackedTexture texture;
                texture = new NativeImageBackedTexture(finalImage);

                texture.upload();
                textureManager.registerTexture(textureId, texture);
            } catch (Throwable var3) {
                finalImage.close();
                throw var3;
            }
        });
        return image;
    }

    private static NativeImage loadSvgImage(URI url) {
        try {


            SVGIcon svgIcon = new SVGIcon();
            svgIcon.setSvgURI(url);
            svgIcon.setAntiAlias(true);


            int width = svgIcon.getIconWidth();
            int height = svgIcon.getIconHeight();


            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);


            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            svgIcon.paintIcon(null, g2d, 0, 0);
            g2d.dispose();

            NativeImage nativeImage = new NativeImage(bufferedImage.getWidth(), bufferedImage.getHeight(), false);
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                for (int y = 0; y < bufferedImage.getHeight(); y++) {
                    int rgb = bufferedImage.getRGB(x, y);
                    nativeImage.setColorArgb(x, y, rgb);
                }
            }
            return nativeImage;


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
