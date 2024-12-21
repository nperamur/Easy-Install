package neelesh.testing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static net.minecraft.client.gui.screen.world.CreateWorldScreen.TAB_HEADER_BACKGROUND_TEXTURE;

public class ProjectScreen extends Screen {
    private ModInfo modInfo;
    private final Identifier iconTextureId;
    //make the image into an object
    private final ArrayList<Identifier> projectImageIds = new ArrayList<Identifier>();
    private final ArrayList<NativeImage> projectImages = new ArrayList<NativeImage>();
    private final ArrayList<Integer> imagePositions = new ArrayList<Integer>();
    private final ArrayList<Integer> imageWidths = new ArrayList<Integer>();
    private ArrayList<GalleryImage> galleryImages;
    //make the link into an object
    private ArrayList<ButtonWidget> linkButtons = new ArrayList<ButtonWidget>();
    private ArrayList<String> linkUrls = new ArrayList<String>();
    private final ArrayList<Integer> linkIndexes = new ArrayList<Integer>();
    private final ArrayList<Integer> linkLengths = new ArrayList<Integer>();
    private final ArrayList<Integer> originalY = new ArrayList<Integer>();
    private ArrayList<String> clickableImageLinks;
    private ArrayList<Integer> clickableImageIndexes;
    private VersionsTab versionsTab;
    private final ButtonWidget installButton = ButtonWidget.builder(Text.of("Install"), button -> {
        Thread thread = new Thread(() -> {
            TestingClient.downloadVersion(modInfo.getSlug(), modInfo.getProjectType());
            modInfo.setInstalled(true);
            versionsTab.setInitialized(false);
        });
        thread.start();
    }).build();
    private int count;

    private final ButtonWidget siteButton = ButtonWidget.builder(Text.of("Modrinth↗"), button -> {
        try {
            Util.getOperatingSystem().open(new URI("https://modrinth.com/project/" + modInfo.getSlug()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }).build();

    private Screen prevScreen;
    private final ButtonWidget doneButton = ButtonWidget.builder(Text.of("Done"), button -> {
        MinecraftClient.getInstance().setScreen(this.prevScreen);
    }).build();
    private DescriptionTab descriptionTab;
    private GalleryTab galleryTab;
    private Tab prevTab;

    private final TabManager tabManager = new TabManager(this::addDrawableChild, child -> this.remove(child));
    private TabNavigationWidget tabNavigationWidget;
    private int scrollAmount = 15;
    public static final Identifier VERTICAL_SEPARATOR_TEXTURE = Identifier.of(Testing.MOD_ID,"textures/gui/vertical_separator.png");
    private Thread thread;
    protected ProjectScreen(Screen parent, ModInfo modInfo) {
        super(Text.literal(modInfo.getTitle()));
        this.modInfo = modInfo;
        iconTextureId = Identifier.of("project_texture_id");
        this.prevScreen = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        doneButton.setWidth(110);
        doneButton.setHeight(17);
        doneButton.setPosition(10, height - doneButton.getHeight());
        doneButton.render(context, mouseX, mouseY, delta);
        if (tabManager.getCurrentTab() instanceof VersionsTab && tabManager.getCurrentTab() != prevTab) {
            ((VersionsTab) (tabManager.getCurrentTab())).setInitialized(false);
        }
        prevTab = tabManager.getCurrentTab();
        ((Drawable) tabManager.getCurrentTab()).render(context, mouseX, mouseY, delta);
        for (int i = 0; i < tabNavigationWidget.children().size(); i++) {
            ((TabButtonWidget) tabNavigationWidget.children().get(i)).setWidth((this.width - 130)/(tabNavigationWidget.children().size()));
            ((TabButtonWidget) tabNavigationWidget.children().get(i)).setY(scrollAmount-10);
            ((TabButtonWidget) tabNavigationWidget.children().get(i)).setX(130 + i * ((TabButtonWidget) tabNavigationWidget.children().get(i)).getWidth());
            ((TabButtonWidget) tabNavigationWidget.children().get(i)).render(context, mouseX, mouseY, delta);
        }
        float titleSize = 1.4f;
        context.getMatrices().scale(titleSize, titleSize, 1.0f);
        context.drawTextWrapped(textRenderer, StringVisitable.plain(modInfo.getTitle()), (int) (10 /titleSize), 40, (int) (110/titleSize),0xFFFFFF);
        int wrappedHeight =  textRenderer.getWrappedLinesHeight(StringVisitable.plain(modInfo.getTitle()), (int) (110/titleSize));
        context.getMatrices().scale(1/titleSize, 1/titleSize, 1.0f);
        context.drawTexture(RenderLayer::getGuiTextured, iconTextureId, 10, 0, 0, 0, 50, 50, 50, 50);
        context.drawTextWrapped(textRenderer, StringVisitable.plain(modInfo.getDescription()), 10, (int) (65 + wrappedHeight*titleSize), 110, 0xFFFFFF);
        installButton.active = !modInfo.isInstalled();
        installButton.setPosition(10, (int) ((65 + textRenderer.getWrappedLinesHeight(StringVisitable.plain(modInfo.getDescription()), 110) + wrappedHeight * titleSize + 10)));
        siteButton.setPosition(65, (int) ((65 + textRenderer.getWrappedLinesHeight(StringVisitable.plain(modInfo.getDescription()), 110) + wrappedHeight * titleSize + 10)));
        installButton.render(context, mouseX, mouseY, delta);
        siteButton.render(context, mouseX, mouseY, delta);
        int i = 0;
        if (tabManager.getCurrentTab() instanceof DescriptionTab) {
            for (ButtonWidget linkButton : linkButtons) {
                linkButton.active = true;
                linkButton.setY(scrollAmount + originalY.get(i));
                i++;
            }
        } else {
            for (ButtonWidget linkButton : linkButtons) {
                linkButton.active = false;
            }
        }


        if (!installButton.active) {
            installButton.setMessage(Text.of("Installed"));
        } else {
            installButton.setMessage(Text.of("Install"));
        }
        context.getMatrices().translate(0, 0, -1000);
    }


    @Override
    protected void init() {
        super.init();
        this.count = -1;
        installButton.setWidth(52);
        installButton.setHeight(12);
        siteButton.setWidth(55);
        siteButton.setHeight(12);
        galleryImages = new ArrayList<GalleryImage>();
        clickableImageLinks = new ArrayList<String>();
        clickableImageIndexes = new ArrayList<Integer>();

        this.addSelectableChild(doneButton);
        this.addSelectableChild(installButton);
        this.addSelectableChild(siteButton);
        Thread thread = new Thread(() -> {
            IconManager.loadIcon(modInfo, iconTextureId, client);
        });
        thread.start();
//        boolean isImage = false;
//        boolean linkInImage = false;
//        boolean puttingImageUrl = false;
//        boolean puttingImageWidth = false;
//        int imageCount = 0;
//        String imageWidth = "";
        String urlString = "https://api.modrinth.com/v2/project/" + modInfo.getSlug();
        try {
            URL url = URI.create(urlString).toURL();
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == httpURLConnection.HTTP_OK) {
                String response = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
                JsonObject jsonObject = (JsonObject) JsonParser.parseString(response);
                String body = jsonObject.get("body").getAsString();
                modInfo.setBody(extractTextFromHtml(body, true).getString());
                JsonArray gallery = jsonObject.get("gallery").getAsJsonArray();
                for (int i = 0; i < gallery.size(); i++) {
                    try {
                        galleryImages.add(new GalleryImage(Identifier.of("gallery_image:" + i), URI.create(gallery.get(i).getAsJsonObject().get("url").getAsString()).toURL(), gallery.get(i).getAsJsonObject().get("description").getAsString()));
                    } catch (UnsupportedOperationException e) {
                        galleryImages.add(new GalleryImage(Identifier.of("gallery_image:" + i), URI.create(gallery.get(i).getAsJsonObject().get("url").getAsString()).toURL()));
                    }
                    try {
                        galleryImages.get(i).setTitle(gallery.get(i).getAsJsonObject().get("title").getAsString());
                    } catch (UnsupportedOperationException ignored) {

                    }
                    System.out.println("GalleryImage: " + gallery.get(i));
                }
            }
            httpURLConnection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        for (int i = 0; i < modInfo.getBody().length(); i++) {
//            if (i < modInfo.getBody().length() - 2 && (modInfo.getBody().charAt(i) == '-') && modInfo.getBody().charAt(i+1) == '\n' && (i == 0 || modInfo.getBody().charAt(i-1) != '-')) {
//                modInfo.setBody(modInfo.getBody().substring(0, i + 1) + " " + modInfo.getBody().substring(i + 3));
//            }
//            if (modInfo.getBody().startsWith("[![", i)) {
//                linkInImage = true;
//            }
//            if (modInfo.getBody().startsWith("![", i)) {
//                isImage = true;
//            } else if (modInfo.getBody().startsWith("width=", i) && isImage) {
//                puttingImageWidth = true;
//                i+=5;
//            } else if (puttingImageWidth) {
//                puttingImageWidth = Character.isDigit(modInfo.getBody().charAt(i));
//                if (puttingImageWidth) {
//                    imageWidth += Integer.parseInt(modInfo.getBody().substring(i, i+1));
//                }
//            }
//            if (modInfo.getBody().charAt(i) == ')' && isImage) {
//                puttingImageUrl = false;
//                isImage = false;
//                URL url;
//                if (linkInImage && i < modInfo.getBody().length() - 2 && modInfo.getBody().charAt(i+1) == '\n') {
//                    modInfo.setBody(modInfo.getBody().substring(0,i+1) + " " + modInfo.getBody().substring(i+2));
//                }
//
//
//                try {
//                    url = new URL(str);
//                    Identifier id = Identifier.of("project_image:" + i);
//                    NativeImage image = IconManager.loadIcon(url, id, client);
//                    if (image != null) {
//                        projectImageIds.add(id);
//                        projectImages.add(image);
//                        imagePositions.add(i);
//                        if (!imageWidth.isEmpty()) {
//                            imageWidths.add(Integer.parseInt(imageWidth));
//                        } else {
//                            imageWidths.add(-1);
//                        }
//                        if (linkInImage) {
//                            clickableImageIndexes.add(imageCount);
//                            clickableImageLinks.add(modInfo.getBody().substring(modInfo.getBody().substring(i + 1).indexOf('(') + 2 + i, modInfo.getBody().substring(i + 1).indexOf(')') + 1 + i));
//                            System.out.println("Clickable Button: " + modInfo.getBody().substring(modInfo.getBody().substring(i + 1).indexOf('(') + i + 2, modInfo.getBody().substring(i + 1).indexOf(')') + 1 + i));
//                        }
//                        imageCount++;
//                    }
//                    linkInImage = false;
//                    System.out.println("successfully loaded project image" + " " + str);
//                } catch (MalformedURLException e) {
//                    System.out.println("cannot load project image");
//                    linkInImage = false;
//                }
//                imageWidth = "";
//                str = "";
//            }
//            if (puttingImageUrl) {
//                str+=modInfo.getBody().charAt(i);
//            }
//            if (modInfo.getBody().charAt(i) == '(' && isImage) {
//                puttingImageUrl = true;
//            }
//        }
        this.galleryTab = new GalleryTab(Text.of("Gallery"));
        this.descriptionTab = new DescriptionTab(Text.of("Description"));
        this.versionsTab = new VersionsTab(Text.of("Versions"));
        if (!galleryImages.isEmpty()) {
            tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, this.width-130).tabs(descriptionTab, galleryTab, versionsTab).build();
        } else {
            tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, (this.width-130)).tabs(descriptionTab, versionsTab).build();

        }
        tabNavigationWidget.init();
        tabNavigationWidget.selectTab(0, false);
        this.addSelectableChild(tabNavigationWidget);
        System.out.println(modInfo.getBody());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollAmount + verticalAmount * 12 < 20) {
            scrollAmount += (int) (verticalAmount*12);
        }
        return true;
    }
    private static String convertMarkdownToHtml(String markdown) {
        // Create the Markdown parser and renderer
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        // Parse the Markdown and render it to HTML

        Node document = parser.parse(String.valueOf(markdown));
        return renderer.render(document);
    }

    private MutableText extractTextFromHtml(String htmlContent, boolean preserveLinks) {
        htmlContent = htmlContent.replace("<li>", "-").replace("</p>", "\n");
        Document document = Jsoup.parse(htmlContent);
        Elements headers = document.select("h1, h2, h3, h4, h5, h6");
        for (Element header: headers) {
            int level = Integer.parseInt(header.tagName().substring(1));
            String markdownHeader = "#".repeat(level) + " " + header.text();
            header.html(markdownHeader);
        }

        if (preserveLinks) {
            Elements images = document.select("img");
            for (Element img : images) {
                String altText = img.attr("alt");
                String imgUrl = img.attr("src");
                int width;
                String markdownImage;
                try {
                    if (img.attr("width").contains("%")){
                        width = (int) (Double.parseDouble(img.attr("width").replace("%", "")) * 10);
                    } else {
                        width = Integer.parseInt(img.attr("width").replace("px", ""));
                    }
                    markdownImage = String.format("![width=%d](%s)", width, imgUrl);
                } catch (NumberFormatException e) {
                    markdownImage = String.format("![%s](%s)", altText, imgUrl);
                }
                img.html(markdownImage);
            }
        }
        MutableText finalText = Text.literal("");

        linkUrls.clear();
        linkLengths.clear();
        linkIndexes.clear();
        Elements formatting = document.select("i, em, b, strong, a");
        int lastIndex = 0;
        if (!preserveLinks) {
            for (Element e : formatting) {
                try {
                    String textBeforeLink = document.wholeText().substring(lastIndex, document.wholeText().indexOf(e.wholeText()));
                    finalText.append(textBeforeLink);
                    MutableText text = Text.literal(e.wholeText().replaceAll("\\s*->\\s*", " → ").replaceAll("\\s*<-\\s*", " ← ").replace("\\", ""));
                    if (!e.select("a").isEmpty()) {
                        //text = text.setStyle(text.getStyle().withColor(0x257DE6));
                        MutableText newText = Text.literal("");
                        int lastIndex2 = 0;

                        for (Element link : e.select("a")) {
                            newText.append(Text.literal(text.getString().substring(lastIndex2, text.getString().indexOf(link.text()))));
                            newText.append(Text.literal(text.getString().substring(text.getString().indexOf(link.text()), text.getString().indexOf(link.text()) + link.text().length())).setStyle(text.getStyle().withColor(0x257DE6)));
                            if (!link.text().isEmpty()) {
                                linkUrls.add(link.attr("href"));
                                linkIndexes.add(finalText.getString().length() + text.getString().indexOf(link.text()));
                                linkLengths.add(link.text().length());
                            }
                            lastIndex2 += newText.getString().length();
                        }
                        newText.append(text.getString().substring(text.getString().indexOf(e.select("a").getLast().text()) + e.select("a").getLast().text().length()));
                        text = newText;
                    }
                    if (!e.select("b, strong").isEmpty()) {
                        text = text.setStyle(text.getStyle().withBold(true));
                    }
                    if (!e.select("i, em").isEmpty()) {
                        text = text.setStyle(text.getStyle().withItalic(true));
                    }

                    finalText.append(text);
                    lastIndex = document.wholeText().indexOf(e.wholeText()) + e.wholeText().length();

                } catch (Exception ignored) {

                }
            }
            document.outputSettings().prettyPrint(true);
            finalText.append(document.wholeText().substring(lastIndex).replaceAll("\\s*->\\s*", " → ").replaceAll("\\s*<-\\s*", " ← ").replace("\\", ""));

        } else {
            Elements links = document.select("a");
            for (Element link : links) {
                String str = String.format("[%s](%s)", link.text(), link.attr("href"));
                link.html(str);
            }
            for (Element e : formatting) {
                String text = e.text();
                String t = "";
                if (!e.tagName().equals("a") && document.wholeText().indexOf(text) - 1 > 0 && !(Character.getType(document.wholeText().charAt(document.wholeText().indexOf(text) - 1)) == Character.SPACE_SEPARATOR)) {
                    t = " ";
                }
                if ((e.tagName().equals("b") || e.tagName().equals("strong")) && e.select("img").isEmpty()) {
                    t += String.format("__%s__", text);
                } else if ((e.tagName().equals("i") || e.tagName().equals("em")) &&  e.select("img").isEmpty()) {
                    t += String.format("_%s_", text);
                } else {
                    t = text;
                }
                if (!e.tagName().equals("a") && document.wholeText().indexOf(text) + 1 < document.wholeText().length() - 1 && (Character.getType(document.wholeText().charAt(document.wholeText().indexOf(text) + 1)) != Character.SPACE_SEPARATOR)) {
                    t += " ";
                }
                e.html(t);
            }
            String text = document.wholeText();
            String wholeText = document.wholeText();
            for (int i = 0; i < wholeText.length(); i++) {
                if ((i == 0 || (wholeText.charAt(i - 1) != '(' && wholeText.charAt(i - 1) != '[' && wholeText.charAt(i - 1) != '"' && wholeText.charAt(i - 1) != '=')) && ((wholeText.startsWith("https://", i) || wholeText.startsWith("http://", i)))) {
                    String url;
                    try {
                        if (wholeText.indexOf(" ", i) == -1 || wholeText.indexOf("\n", i) == -1) {
                            url = wholeText.substring(i, Math.max(wholeText.indexOf(" ", i), wholeText.indexOf("\n", i)));
                        } else {
                            url = wholeText.substring(i, Math.min(wholeText.indexOf(" ", i), wholeText.indexOf("\n", i)));
                        }
                    } catch (IndexOutOfBoundsException e) {
                        url = wholeText.substring(i);
                    }
                    text = text.substring(0, text.indexOf(url)) + String.format("[%s](%s)", url, url) + text.substring(text.indexOf(url) + url.length());
                }
            }
            finalText = Text.literal(text);
        }
                //return document.wholeText().replaceAll("\\s*->\\s*", " → ").replace("\\", "");
        return finalText;
    }

    @Override
    protected void renderDarkening(DrawContext context) {
        context.drawTexture(RenderLayer::getGuiTextured, TAB_HEADER_BACKGROUND_TEXTURE, 0, 0, 0.0F, 0.0F, this.width, ((TabButtonWidget) this.tabNavigationWidget.children().get(0)).getHeight(), 16, 16);
    }

    private class GalleryTab extends GridScreenTab implements Drawable {
        public GalleryTab(Text title) {
            super(title);
            Thread thread = new Thread(() -> {
                for (int i = 0; i < galleryImages.size(); i++) {
                    galleryImages.get(i).setImage(IconManager.loadIcon(galleryImages.get(i).getUrl(), galleryImages.get(i).getId(), client));
                }
            });
            thread.start();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            int y = 30;
            int prevHeight = 0;
            for (int i = 0; i < galleryImages.size(); i++) {
                if (galleryImages.get(i).getImage() == null) {
                    continue;
                }
                double imageSize = (double) (width-130)/2.5;
                double v = Math.pow(-1, i + 1) * (imageSize / 2 + 10);
                float titleSize = 1.2f;

                context.getMatrices().scale(titleSize, titleSize, 1f);
                context.drawTextWrapped(textRenderer, StringVisitable.plain(galleryImages.get(i).getTitle()), (int) ((135 + ((double) (width - 130) /2 + v) - imageSize/2)/titleSize), (int) ((int) (y + 5 + scrollAmount + imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight())/titleSize),(int) (imageSize/titleSize), 0xFFFFFF);
                float titleHeight = (textRenderer.getWrappedLinesHeight(StringVisitable.plain(galleryImages.get(i).getTitle()), (int) (imageSize/titleSize)) * titleSize);
                context.getMatrices().scale(1.0f/titleSize, 1.0f/titleSize, 1f);
                context.drawTextWrapped(textRenderer, StringVisitable.plain(galleryImages.get(i).getDescription()), 135 + ((int)((double) (width - 130)/2 + v)  - (int) imageSize/2), (int) (y + titleHeight + 8 + scrollAmount + (imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight())),(int) imageSize, 0xFFFFFF);
                context.drawTexture(RenderLayer::getGuiTextured, galleryImages.get(i).getId(), 135 + ((int)((double) (width - 130)/2 + v) - (int) imageSize/2), y + scrollAmount, 0, 0, (int) imageSize, (int)(imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight()), (int) imageSize, (int)(imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight()));
                float descriptionHeight = (textRenderer.getWrappedLinesHeight(StringVisitable.plain(galleryImages.get(i).getDescription()), (int) imageSize));
                if (i % 2 == 1 || i == galleryImages.size() - 1) {
                    y += Math.max(prevHeight, (int) (descriptionHeight + titleHeight + 8 + (imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight()))) + 15;
                    prevHeight = 0;
                } else {
                    prevHeight = (int) (titleHeight + 8 + descriptionHeight + (imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight()));
                }

            }
            context.getMatrices().translate(0, 0, -1000);
            renderDarkening(context, 130, scrollAmount + ((TabButtonWidget) tabNavigationWidget.children().get(0)).getHeight()-10, width, y);
            context.drawTexture(
                    RenderLayer::getGuiTextured, VERTICAL_SEPARATOR_TEXTURE, 130, scrollAmount + ((TabButtonWidget) tabNavigationWidget.children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, y, 2, 32
            );
        }
    }

    private class VersionsTab extends GridScreenTab implements Drawable {
        private Version[] versions;
        private ButtonWidget[] versionButtons;
        private boolean initialized;

        public VersionsTab(Text title) {
            super(title);
            Thread thread = new Thread(() -> {
                String response = TestingClient.getVersions(modInfo.getSlug(), modInfo.getProjectType());
                JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                versions = new Version[jsonArray.size()];
                versionButtons = new ButtonWidget[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {

                    JsonObject versionInfo = jsonArray.get(i).getAsJsonObject();
                    Version version;
                    try {
                        version = new Version(versionInfo.get("name").getAsString(),
                                versionInfo.get("version_number").getAsString(),
                                versionInfo.get("version_type").getAsString(),
                                new URL(versionInfo.get("files").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString()),
                                versionInfo.get("downloads").getAsInt(),
                                modInfo.getProjectType(),
                                versionInfo.get("files").getAsJsonArray().get(0).getAsJsonObject().get("filename").getAsString(),
                                versionInfo.get("dependencies").getAsJsonArray(),
                                versionInfo.get("files").getAsJsonArray().get(0).getAsJsonObject().get("hashes").getAsJsonObject().get("sha512").getAsString()

                        );
                        versions[i] = version;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int finalI = i;
                    versionButtons[i] = ButtonWidget.builder(Text.of("Install"), buttonWidget -> {
                        versions[finalI].downloadVersion();
                        initialized = false;
                        if (finalI == 0) {
                            modInfo.setInstalled(true);
                        }
                    }).build();
                    versionButtons[i].setDimensions(55, 12);
                    addSelectableChild(versionButtons[i]);
                }
            });
            thread.start();

        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (versions == null) {
                return;
            }
            for (int i = 0; i < versions.length; i++) {
                if (versions[i] == null) {
                    break;
                }
                context.drawText(textRenderer, Text.of(versions[i].getName()), 140, i * 40 + scrollAmount + 20, 0xFFFFFF, true);
                Formatting formatting;
                formatting = switch(versions[i].getVersionType()) {
                    case "release" -> Formatting.GREEN;
                    case "beta" -> Formatting.GOLD;
                    case "alpha" -> Formatting.RED;
                    default -> null;
                };
                context.drawText(textRenderer, Text.literal("•" + versions[i].getVersionType()).formatted(formatting), 140, i * 40 + scrollAmount + 30, 0xFFFFFF, true);
                context.drawText(textRenderer, Text.of(versions[i].getVersionNumber()), 140 + textRenderer.getWidth("•" + versions[i].getVersionType()) + 8, i * 40 + scrollAmount + 30, 0xFFFFFF, true);
                context.drawText(textRenderer, Text.of(String.format("%,d", versions[i].getNumDownloads()) + " downloads"), width - textRenderer.getWidth(String.format("%,d", versions[i].getNumDownloads()) + " downloads") - 8, i * 40 + scrollAmount + 36, 0xFFFFFF, true);
                File file = switch(modInfo.getProjectType()) {
                    case MOD -> new File(Paths.get(FabricLoader.getInstance().getGameDir() + "/mods", versions[i].getFilename()).toString());
                    case RESOURCE_PACK -> new File(Paths.get(FabricLoader.getInstance().getGameDir() + "/resourcepacks", versions[i].getFilename()).toString());
                    case DATA_PACK -> new File(Paths.get(TestingClient.getDataPackTempDir().toString(), versions[i].getFilename()).toString());case SHADER -> new File(Paths.get(FabricLoader.getInstance().getGameDir() + "/shaderpacks", versions[i].getFilename()).toString());
                };


                if (file.exists() && tabManager.getCurrentTab() == this && !initialized) {
                    String hash;
                    try {
                        hash = TestingClient.createFileHash(Path.of(file.getPath()));
                        System.out.println(hash);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (versions[i].getHash().equals(hash)) {
                        versionButtons[i].active = false;
                        versionButtons[i].setMessage(Text.of("Installed"));
                    }
                } else if (!initialized) {
                    versionButtons[i].active = true;
                    versionButtons[i].setMessage(Text.of("Install"));
                }
                versionButtons[i].setPosition(width - versionButtons[i].getWidth() - 10, i * 40 + 20 + scrollAmount);
                versionButtons[i].render(context, mouseX, mouseY, delta);
            }
            initialized = true;
            renderDarkening(context, 130, scrollAmount + ((TabButtonWidget) tabNavigationWidget.children().get(0)).getHeight()-10, width, versions.length * 40 + 10);
            context.drawTexture(
                    RenderLayer::getGuiTextured, VERTICAL_SEPARATOR_TEXTURE, 130, scrollAmount + ((TabButtonWidget) tabNavigationWidget.children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, versions.length * 40 + 10, 2, 32
            );
        }

        public void setInitialized(boolean initialized) {
            this.initialized = initialized;
        }
    }

    private class DescriptionTab extends GridScreenTab implements Drawable {
        public DescriptionTab(Text title) {
            super(title);
            if (thread != null) {
                thread.interrupt();
            }
            for (int i = 0; i < modInfo.getBody().length(); i++) {
                if (i < modInfo.getBody().length() - 2 && (modInfo.getBody().charAt(i) == '-') && modInfo.getBody().charAt(i + 1) == '\n' && (i == 0 || modInfo.getBody().charAt(i - 1) != '-')) {
                    modInfo.setBody(modInfo.getBody().substring(0, i + 1) + " " + modInfo.getBody().substring(i + 3));
                }
            }
            thread = new Thread(() ->{
                String str = "";
                boolean isImage = false;
                boolean linkInImage = false;
                boolean puttingImageUrl = false;
                boolean puttingImageWidth = false;
                int imageCount = 0;
                String imageWidth = "";
                for (int i = 0; i < modInfo.getBody().length(); i++) {
                    if (modInfo.getBody().startsWith("[![", i)) {
                        linkInImage = true;
                    }
                    if (modInfo.getBody().startsWith("![", i)) {
                        isImage = true;
                    } else if (modInfo.getBody().startsWith("width=", i) && isImage) {
                        puttingImageWidth = true;
                        i += 5;
                    } else if (puttingImageWidth) {
                        puttingImageWidth = Character.isDigit(modInfo.getBody().charAt(i));
                        if (puttingImageWidth) {
                            imageWidth += Integer.parseInt(modInfo.getBody().substring(i, i + 1));
                        }
                    }
                    if (modInfo.getBody().charAt(i) == ')' && isImage) {
                        puttingImageUrl = false;
                        isImage = false;
                        URL url;
                        if (linkInImage && i < modInfo.getBody().length() - 2 && modInfo.getBody().charAt(i + 1) == '\n') {
                            modInfo.setBody(modInfo.getBody().substring(0, i + 1) + " " + modInfo.getBody().substring(i + 2));
                        }


                        try {
                            url = new URL(str);
                            Identifier id = Identifier.of("project_image:" + i);
                            NativeImage image = IconManager.loadIcon(url, id, client);
                            if (image != null) {
                                projectImageIds.add(id);
                                projectImages.add(image);
                                imagePositions.add(i);
                                if (!imageWidth.isEmpty()) {
                                    imageWidths.add(Integer.parseInt(imageWidth));
                                } else {
                                    imageWidths.add(-1);
                                }
                                if (linkInImage) {
                                    clickableImageIndexes.add(imageCount);
                                    clickableImageLinks.add(modInfo.getBody().substring(modInfo.getBody().substring(i + 1).indexOf('(') + 2 + i, modInfo.getBody().substring(i + 1).indexOf(')') + 1 + i));
                                    System.out.println("Clickable Button: " + modInfo.getBody().substring(modInfo.getBody().substring(i + 1).indexOf('(') + i + 2, modInfo.getBody().substring(i + 1).indexOf(')') + 1 + i));
                                }
                                imageCount++;
                            }
                            linkInImage = false;
                            System.out.println("successfully loaded project image" + " " + str);
                        } catch (MalformedURLException e) {
                            System.out.println("cannot load project image");
                            linkInImage = false;
                        }
                        imageWidth = "";
                        str = "";
                    }
                    if (puttingImageUrl) {
                        str += modInfo.getBody().charAt(i);
                    }
                    if (modInfo.getBody().charAt(i) == '(' && isImage) {
                        puttingImageUrl = true;
                    }
                    count = 0;
                }
            });
            thread.start();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            String body = modInfo.getBody();
            String s = "";
            float scale = 1.0f;
            int y = 30;
            int x = 140;
            int imageIndex = 0;
            int imageHeight = 0;
            for (int i = 0; i < body.length(); i++) {
                if (imagePositions.size() > imageIndex && imagePositions.get(imageIndex).equals(i)) {
                    int imageWidth;
                    if (!(imageWidths.get(imageIndex) == -1) && (imageWidths.get(imageIndex) * (width - 150)/1000) < width - x - 10) {
                        imageWidth = (int) (imageWidths.get(imageIndex) * (width - 150)/(1000 * scale));
                        context.drawTexture(RenderLayer::getGuiTextured, projectImageIds.get(imageIndex), (int)(x/scale), (int)((y+scrollAmount)/scale), 0, 0, imageWidth, projectImages.get(imageIndex).getHeight() * imageWidth/projectImages.get(imageIndex).getWidth(), imageWidth, projectImages.get(imageIndex).getHeight() * imageWidth/projectImages.get(imageIndex).getWidth());
                        imageHeight = Math.max(imageHeight, projectImages.get(imageIndex).getHeight() * imageWidth/projectImages.get(imageIndex).getWidth() + 10);
                        if (clickableImageIndexes.contains(imageIndex) && count == 0) {
                            createClickableImageButtons(x, y, imageWidth, projectImages.get(imageIndex).getHeight() * imageWidth/projectImages.get(imageIndex).getWidth(), clickableImageIndexes.indexOf(imageIndex));
                        }
                        x+=imageWidth + 5;
                    } else if (projectImages.get(imageIndex).getWidth()/2 < (width - 150)) {
                        if (projectImages.get(imageIndex).getWidth()/2 >= (width - x - 10)) {
                            x = 140;
                            y += imageHeight;
                            imageHeight = 0;
                        }
                        context.drawTexture(RenderLayer::getGuiTextured, projectImageIds.get(imageIndex), (int)(x/scale), (int)((y+scrollAmount)/scale), 0, 0, (int)(projectImages.get(imageIndex).getWidth()/(2 * scale)), (int)(projectImages.get(imageIndex).getHeight()/(2 * scale)), (int)(projectImages.get(imageIndex).getWidth()/(2 * scale)), (int) (projectImages.get(imageIndex).getHeight()/(2 * scale)));
                        imageWidth = projectImages.get(imageIndex).getWidth()/2;
                        imageHeight = Math.max(imageHeight, projectImages.get(imageIndex).getHeight()/2 + 10);
                        if (clickableImageIndexes.contains(imageIndex) && count == 0) {
                            createClickableImageButtons(x, y, imageWidth, projectImages.get(imageIndex).getHeight()/2, clickableImageIndexes.indexOf(imageIndex));
                        }
                        x+=imageWidth + 5;
                    } else {
                        if (imageHeight > 0) {
                            x = 140;
                            y += imageHeight;
                            imageHeight = 0;
                        }
                        imageWidth = (width - x - 10);
                        if (clickableImageIndexes.contains(imageIndex) && count == 0) {
                            createClickableImageButtons(x, y, imageWidth, projectImages.get(imageIndex).getHeight() * imageWidth/projectImages.get(imageIndex).getWidth() + 10, clickableImageIndexes.indexOf(imageIndex));
                        }
                        context.drawTexture(RenderLayer::getGuiTextured, projectImageIds.get(imageIndex), (int)(x/scale), (int)((y+scrollAmount)/scale), 0, 0, (width - x - 10), projectImages.get(imageIndex).getHeight() * (int)((width-x-10)/scale)/projectImages.get(imageIndex).getWidth(), (int)((width-x-10)/scale), projectImages.get(imageIndex).getHeight() * (int)((width-x-10)/scale)/projectImages.get(imageIndex).getWidth());
                        y+=projectImages.get(imageIndex).getHeight() * imageWidth/projectImages.get(imageIndex).getWidth() + 10;
                    }
                    imageIndex++;
                }
                if (body.charAt(i) == '#' && (i == 0 || body.charAt(i-1) != '#') && s.replace(" ", "").replace("\n", "").isEmpty() || i == body.length() - 1) {
                    if (i == body.length() - 1) {
                        s+=body.charAt(i);
                    }
                    MutableText text = extractTextFromHtml(convertMarkdownToHtml(s), false);
                    if (count == 0) {
                        putLinkButtons(text, x, y, (int) ((width-x-10) / scale), scale);
                    }
                    context.drawTextWrapped(textRenderer, text, (int) (x/scale), (int) (y/scale + scrollAmount / scale), (int) ((width-x-10) / scale), 0xFFFFFF);
                    int wrappedSize = textRenderer.getWrappedLinesHeight(text, (int) ((width-x-10) / scale));
                    if (scale == 1) {
                        scale = 1.4f;
                        context.getMatrices().scale(scale, scale, 1.0f);
                    }
                    s = "";
                    x = 140;
                    y += imageHeight;
                    imageHeight = 0;
                    y += (int) (wrappedSize * scale);
                } else if ((body.charAt(i) == '\n')) {
                    MutableText text = extractTextFromHtml(convertMarkdownToHtml(s), false);
                    if (!text.getString().trim().isEmpty()) {
                        y += imageHeight;
                        if (imageHeight > 0) {
                            x = 140;
                        }
                        imageHeight = 0;
                    }
                    s=text.getString();
                    if (count == 0) {
                        putLinkButtons(text, x, y, (int) ((width-x-10) / scale), scale);
                    }
                    context.drawTextWrapped(textRenderer, text, (int) (x/(scale)), (int) (y/scale + scrollAmount / scale), (int) ((width-x-10) / scale), 0xFFFFFF);
                    if (scale > 1) {
                        context.getMatrices().scale(1/scale, 1/scale, 1.0f);
                    }
                    int wrappedSize = textRenderer.getWrappedLinesHeight(text, (int) ((width-x-10) / scale));
                    scale = 1f;
                    if (!s.trim().isEmpty()) {
                        y += wrappedSize;
                    }
                    s = "";
                } else if (((body.charAt(i) == '-' && body.charAt(i+1) != '-') || (body.charAt(i) == '*' && (i == body.length()-1 || body.charAt(i+1) != '*'))) && (s.replace(" ", "").replace("\n", "").isEmpty())) {
                    x = 160;
                } if (body.startsWith("---", i) || body.startsWith("\n\n", i)) {
                    x = 140;
                    y += imageHeight;
                    imageHeight = 0;
                }

                if (body.charAt(i) != '#' || !s.replace(" ", "").replace("\n", "").isEmpty()) {
                    s += body.charAt(i);
                }
            }
            context.getMatrices().scale(1/scale, 1/scale, 1.0f);
            context.getMatrices().translate(0, 0, -1000);
            renderDarkening(context, 130, scrollAmount + ((TabButtonWidget) tabNavigationWidget.children().get(0)).getHeight()-10, width, y + imageHeight);
            context.drawTexture(
                    RenderLayer::getGuiTextured, VERTICAL_SEPARATOR_TEXTURE, 130, scrollAmount + ((TabButtonWidget) tabNavigationWidget.children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, y + imageHeight, 2, 32
            );
            if (count != -1) {
                count++;
            }
        }
    }


    public void putLinkButtons(MutableText text, int x, int y, int width, double scale) {
        boolean isLink = false;
        int j = 0;
        int l = 0;
        int k = 0;
        int numLines = 0;
        int siblingIndex = 0;
        int m = 1;
        for (StringVisitable visitableText : textRenderer.getTextHandler().wrapLines(text, width, Style.EMPTY)) {
            String textString = visitableText.getString();
            String s = "";
            MutableText t = Text.literal("");
            int lastIndex = 0;
            for (int i = 0; i < textString.length(); i++) {


                if (linkIndexes.contains(k)) {
                    isLink = true;
                } else if (isLink && (linkLengths.get(l) == j || i == textString.length() - 1)) {
                    String link = linkUrls.get(l);
                    ButtonWidget buttonWidget = ButtonWidget.builder(Text.empty(), button -> {
                        ConfirmLinkScreen.open(this, link, false);
                    }).build();
                    MutableText o = Text.literal("");
                    MutableText p = Text.literal("");
                    if (text.getSiblings().get(siblingIndex).getStyle().isBold()) {
                        o.append(t);
                        o.append(Text.literal(textString.substring(lastIndex, i - s.length())).setStyle(Style.EMPTY.withBold(true)));
                    } else {
                        o.append(t);
                        o.append(Text.literal(textString.substring(lastIndex, i - s.length())));
                    }
                    if (linkLengths.get(l) != j) {
                        p.append(Text.literal(String.valueOf(textString.charAt(i))));
                    }
                    if (text.getSiblings().get(siblingIndex).getStyle().isBold()) {
                        p.append(Text.literal(s).setStyle(Style.EMPTY.withBold(true)));
                    } else {
                        p.append(Text.literal(s));
                    }

                    buttonWidget.setPosition((int) (x + textRenderer.getWidth(o) * scale), y + 9 * numLines);
                    originalY.add(y + 9 * numLines);
                    this.addSelectableChild(buttonWidget);
                    buttonWidget.setDimensions((int) (textRenderer.getWidth(p) * scale), (int) (10 * scale));
                    linkButtons.add(buttonWidget);
                    if (linkLengths.get(l) == j) {
                        isLink = false;
                        j = 0;
                        l++;
                    }
                    s = "";
                }
                if (siblingIndex < text.getSiblings().size() && m >= text.getSiblings().get(siblingIndex).getString().length() && s.isEmpty()) {
                    m = 1;
                    if (text.getSiblings().get(siblingIndex).getStyle().isBold()) {
                        t.append(Text.literal(textString.substring(lastIndex, i)).setStyle(Style.EMPTY.withBold(true)));
                    } else {
                        t.append(Text.literal(textString.substring(lastIndex, i)));
                    }
                    lastIndex = i;
                    siblingIndex++;
                }
                if (isLink) {
                    s += textString.charAt(i);
                    j++;
                }

                k++;
                m++;
            }
            numLines++;
            k++;
            m++;
        }
    }

    public void createClickableImageButtons(int x, int y, int width, int height, int imageIndex) {
        String link = clickableImageLinks.get(imageIndex);
        ButtonWidget button = ButtonWidget.builder(Text.of(""), button1 -> {
            ConfirmLinkScreen.open(this, link, false);
        }).build();
        button.setDimensions(width, height);
        this.addSelectableChild(button);
        button.setPosition(x, y);
        linkButtons.add(button);
        originalY.add(y);
    }




}
