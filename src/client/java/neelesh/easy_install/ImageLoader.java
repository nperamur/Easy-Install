package neelesh.easy_install;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.nodes.Image;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
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
import java.util.logging.Level;

public class ImageLoader {
    public static void loadIcon(ProjectInfo info, Identifier textureId, Thread thread) {
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
                return;
            }
            boolean isWebp = url.toString().endsWith("webp");
            NativeImage image;
            if (url.toString().endsWith("svg")) {
                image = loadSvgImage(url);

            } else {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("GET");
                try (InputStream inputStream = connection.getInputStream()) {
                    image = loadImage(inputStream, isWebp, true);

                } finally {
                    connection.disconnect(); // Disconnect the connection
                }
            }

            if (!thread.isInterrupted()) {
                TextureManager textureManager = client.getTextureManager();
                NativeImage finalImage = image;
                client.execute(() -> {
                    NativeImageBackedTexture texture;
                    texture = new NativeImageBackedTexture(finalImage);
                    texture.upload();
                    if (!thread.isInterrupted()) {
                        textureManager.registerTexture(textureId, texture);
                    }
                    finalImage.close();
                });
            }


        } catch (IOException e) {
            LogUtils.getLogger().error("Failed to load image: {}", e.getMessage());
        }
    }

    private static NativeImage loadImage(InputStream input, boolean webp, boolean icon) {
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
//                System.err.println("Failed to read image.");
                if (!icon) {
                    return null;
                }
                bufferedImage = new BufferedImage(1500, 1500, BufferedImage.TYPE_INT_RGB);
                bufferedImage.createGraphics();
            }
            NativeImage nativeImage;
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

    public static NativeImage loadImage(URL url, Identifier textureId, MinecraftClient client) {
        NativeImage image = null;
        try {
            if (url == null) {
                client.execute(() -> {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(new NativeImage(1, 1, false));
                    texture.getImage().setColorArgb(0, 0, 0xFF000000);
                    texture.upload();
                    client.getTextureManager().registerTexture(textureId, texture);
                });
                return null;
            }
            boolean isWebp = url.toString().endsWith("webp");
            if (!url.toString().endsWith("svg")) {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("GET");
                try (InputStream inputStream = connection.getInputStream()) {
                    image = loadImage(inputStream, isWebp, false);
                    if (image == null && !url.toString().endsWith("webp") && !url.toString().endsWith("png") && !url.toString().endsWith("jpg")) {
                        image = loadSvgImage(url);
                    }
                } finally {
                    connection.disconnect();
                }
            } else {
                image = loadSvgImage(url);
            }

        } catch (Exception e) {
            EasyInstall.LOGGER.warn("Failed to load image: {}", e.getMessage());
        }
        if (image == null) {
            return null;
        }
        TextureManager textureManager = client.getTextureManager();
        NativeImage finalImage = image;
        client.execute(() -> {
            NativeImageBackedTexture texture;
            texture = new NativeImageBackedTexture(finalImage);
            texture.upload();
            textureManager.registerTexture(textureId, texture);
            finalImage.close();
        });
        return image;
    }

    private static NativeImage loadSvgImage(URL url) {
        java.util.logging.Logger svgLogger = java.util.logging.Logger.getLogger(Image.class.getName());
        svgLogger.setLevel(Level.SEVERE);
        try {

            SVGLoader loader = new SVGLoader();
            SVGDocument svgDocument = loader.load(url, LoaderContext.createDefault());

            assert svgDocument != null;
            int width = (int) svgDocument.viewBox().getWidth();
            int height = (int) svgDocument.viewBox().getHeight();


            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);


            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            svgDocument.render(null, g2d);
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
            return null;
        }
    }

    public static void loadPlaceholder(Identifier id) {
        TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
        MinecraftClient.getInstance().execute(() -> {
            NativeImage image = new NativeImage(1, 1, false);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            textureManager.registerTexture(id, texture);
        });
    }
}
