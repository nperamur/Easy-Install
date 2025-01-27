package neelesh.easy_install;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neelesh.easy_install.gui.tab.DescriptionTab;
import neelesh.easy_install.gui.tab.GalleryTab;
import neelesh.easy_install.gui.tab.VersionsTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import static net.minecraft.client.gui.screen.world.CreateWorldScreen.TAB_HEADER_BACKGROUND_TEXTURE;


public class ProjectScreen extends Screen {
    private ModInfo modInfo;
    private final Identifier iconTextureId;
    //make the image into an object
    private int maxY;
    private final ArrayList<ProjectImage> projectImages = new ArrayList<ProjectImage>();
    private ArrayList<GalleryImage> galleryImages;
    private VersionsTab versionsTab;
    private final ButtonWidget installButton = ButtonWidget.builder(Text.of("Install"), button -> {
        Thread thread = new Thread(() -> {
            if (!modInfo.isUpdated()) {
                Thread thread2 = new Thread(() -> {
                    HashMap<String, String> oldHashes = EasyInstallClient.getOldHashes();
                    File dir = new File(EasyInstallClient.getDir(modInfo.getProjectType()));
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            try {
                                String hash = EasyInstallClient.createFileHash(file.toPath());
                                if (oldHashes.containsKey(hash) && oldHashes.get(hash).equals(modInfo.getLatestHash())) {
                                    file.delete();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                thread2.start();
            }
            EasyInstallClient.downloadVersion(modInfo.getSlug(), modInfo.getProjectType());
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
    public static final Identifier VERTICAL_SEPARATOR_TEXTURE = Identifier.of(EasyInstall.MOD_ID,"textures/gui/vertical_separator.png");
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
            ((TabButtonWidget) tabNavigationWidget.children().get(i)).setX(131 + i * ((TabButtonWidget) tabNavigationWidget.children().get(i)).getWidth());
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

        if (!installButton.active) {
            installButton.setMessage(Text.of("Installed"));
        } else if (modInfo.isUpdated()) {
            installButton.setMessage(Text.of("Install"));
        } else {
            installButton.setMessage(Text.of("Update"));
        }
        if (scrollAmount < - maxY + height - 10 && maxY > height - 10) {
            scrollAmount = - maxY + height - 10;
        } else if (scrollAmount < - maxY + height - 10 && maxY <= height - 10) {
            scrollAmount = 20;
        }
    }


    @Override
    protected void init() {
        super.init();
        this.count = -1;
        installButton.setDimensions(52, 14);
        siteButton.setDimensions(55, 14);
        galleryImages = new ArrayList<>();

        this.addSelectableChild(doneButton);
        this.addSelectableChild(installButton);
        this.addSelectableChild(siteButton);
        Thread thread = new Thread(() -> {
            IconManager.loadIcon(modInfo, iconTextureId, client, Thread.currentThread());
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
                String response;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                    response = reader.lines().collect(Collectors.joining("\n"));
                }
                JsonObject jsonObject = (JsonObject) JsonParser.parseString(response);
                String body = jsonObject.get("body").getAsString();
                modInfo.setBody(body);
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
        this.galleryTab = new GalleryTab(Text.of("Gallery"), this);
        this.descriptionTab = new DescriptionTab(Text.of("Description"), this);
        this.versionsTab = new VersionsTab(Text.of("Versions"), this);
        if (!galleryImages.isEmpty()) {
            tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, this.width-131).tabs(descriptionTab, galleryTab, versionsTab).build();
        } else {
            tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, (this.width-131)).tabs(descriptionTab, versionsTab).build();

        }
        tabNavigationWidget.init();
        tabNavigationWidget.selectTab(0, false);
        this.addSelectableChild(tabNavigationWidget);
        System.out.println(modInfo.getBody());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scrollDelta = (int) (verticalAmount * 12);
        if (scrollAmount + scrollDelta <= 20 && scrollAmount + scrollDelta >= - maxY + height - 10) {
            scrollAmount += scrollDelta;
        } else if (scrollAmount + scrollDelta > 20) {
            scrollAmount = 20;
        } else if (scrollAmount + scrollDelta < - maxY + height - 10 && scrollAmount != 20) {
            scrollAmount = - maxY + height - 10;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void renderDarkening(DrawContext context) {
        context.drawTexture(RenderLayer::getGuiTextured, TAB_HEADER_BACKGROUND_TEXTURE, 0, 0, 0.0F, 0.0F, this.width, ((TabButtonWidget) this.tabNavigationWidget.children().get(0)).getHeight(), 16, 16);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        super.renderDarkening(context);

    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public int getScrollAmount() {
        return this.scrollAmount;
    }


    public void renderDarkening(DrawContext context, int x, int y, int width, int height) {
        super.renderDarkening(context, x, y, width, height);
    }

    public ModInfo getModInfo() {
        return this.modInfo;
    }

    public <T extends net.minecraft.client.gui.Element & Selectable> T addSelectableChild(T child) {
        return super.addSelectableChild(child);
    }

    public TabManager getTabManager() {
        return tabManager;
    }

    public TabNavigationWidget getTabNavigationWidget() {
        return tabNavigationWidget;
    }

    public ArrayList<GalleryImage> getGalleryImages() {
        return galleryImages;
    }

    public ArrayList<ProjectImage> getProjectImages() {
        return projectImages;
    }


}