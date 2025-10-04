package neelesh.easy_install.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import neelesh.easy_install.*;
import neelesh.easy_install.gui.tab.DescriptionTab;
import neelesh.easy_install.gui.tab.GalleryTab;
import neelesh.easy_install.gui.tab.TabNavigationMixinInterface;
import neelesh.easy_install.gui.tab.VersionsTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
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
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static net.minecraft.client.gui.screen.world.CreateWorldScreen.TAB_HEADER_BACKGROUND_TEXTURE;


public class ProjectScreen extends Screen implements MarkdownScreenInterface {
    private ProjectInfo projectInfo;
    private final Identifier iconTextureId;
    private int maxY;
    private ArrayList<GalleryImage> galleryImages = new ArrayList<>();
    private VersionsTab versionsTab;
    private boolean initialized;
    private Screen prevScreen;
    private boolean filteredByGameVersion;
    private final ButtonWidget installButton = ButtonWidget.builder(Text.of("Install"), button -> {
        Thread thread = new Thread(() -> {
            projectInfo.setInstalling(true);
            if (!projectInfo.isUpdated()) {
                Thread thread2 = new Thread(() -> {
                    EasyInstallClient.deleteOldFiles(projectInfo.getProjectType(), projectInfo.getLatestHash());
                });
                thread2.start();
            }
            EasyInstallClient.downloadVersion(projectInfo.getSlug(), projectInfo.getProjectType(), ((ProjectBrowser) prevScreen).isFilteredByGameVersion());
            MinecraftClient.getInstance().send(() -> {
                projectInfo.setInstalled(true);
                projectInfo.setInstalling(false);
                versionsTab.setInitialized(false);
            });
        });
        thread.start();
    }).build();

    private final ButtonWidget siteButton = ButtonWidget.builder(Text.of("Modrinth↗"), button -> {
        try {
            Util.getOperatingSystem().open(new URI("https://modrinth.com/project/" + projectInfo.getSlug()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }).build();

    private final ButtonWidget doneButton = ButtonWidget.builder(Text.of("Done"), button -> {
        MinecraftClient.getInstance().setScreen(this.prevScreen);
    }).build();
    private DescriptionTab descriptionTab;
    private Tab prevTab;

    private TabManager tabManager;
    private TabNavigationWidget tabNavigationWidget;
    private int scrollAmount = 15;
    public static final Identifier VERTICAL_SEPARATOR_TEXTURE = Identifier.of(EasyInstall.MOD_ID,"textures/gui/vertical_separator.png");
    protected ProjectScreen(Screen parent, ProjectInfo projectInfo) {
        super(Text.literal(projectInfo.getTitle()));
        this.projectInfo = projectInfo;
        iconTextureId = Identifier.of("project_texture_id");
        this.prevScreen = parent;
        this.filteredByGameVersion = ((ProjectBrowser) parent).isFilteredByGameVersion();
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
        descriptionTab.setLinksActive(tabManager.getCurrentTab() instanceof DescriptionTab);
        versionsTab.setActive(tabManager.getCurrentTab() instanceof VersionsTab);
        for (int i = 0; i < tabNavigationWidget.children().size(); i++) {
            if (tabNavigationWidget instanceof TabNavigationMixinInterface) {
                ((TabNavigationMixinInterface) tabNavigationWidget).setX(131);
                ((TabNavigationMixinInterface) tabNavigationWidget).setY(scrollAmount - 10);
            }
            ((TabButtonWidget) tabNavigationWidget.children().get(i)).render(context, mouseX, mouseY, delta);
        }
        float titleSize = 1.4f;
        context.getMatrices().scale(titleSize, titleSize);
        context.drawWrappedText(textRenderer, StringVisitable.plain(projectInfo.getTitle()), (int) (10 /titleSize), 40, (int) (110/titleSize), Colors.WHITE, false);
        int wrappedHeight =  textRenderer.getWrappedLinesHeight(StringVisitable.plain(projectInfo.getTitle()), (int) (110/titleSize));
        context.getMatrices().scale(1/titleSize, 1/titleSize);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, iconTextureId, 10, 0, 0, 0, 50, 50, 50, 50);
        context.drawWrappedText(textRenderer, StringVisitable.plain(projectInfo.getDescription()), 10, (int) (65 + wrappedHeight*titleSize), 110, Colors.WHITE, false);
        installButton.setPosition(10, (int) ((65 + textRenderer.getWrappedLinesHeight(StringVisitable.plain(projectInfo.getDescription()), 110) + wrappedHeight * titleSize + 10)));
        siteButton.setPosition(65, (int) ((65 + textRenderer.getWrappedLinesHeight(StringVisitable.plain(projectInfo.getDescription()), 110) + wrappedHeight * titleSize + 10)));
        installButton.render(context, mouseX, mouseY, delta);
        siteButton.render(context, mouseX, mouseY, delta);

        if (projectInfo.isInstalling()) {
            installButton.setMessage(Text.of("Installing"));
        } else if (projectInfo.isInstalled()) {
            installButton.setMessage(Text.of("Installed"));
        } else if (projectInfo.isUpdated()) {
            installButton.setMessage(Text.of("Install"));
        } else {
            installButton.setMessage(Text.of("Update"));
        }
        installButton.active = !projectInfo.isInstalled() && !projectInfo.isInstalling();
        if (scrollAmount < - maxY + height - 10 && maxY > height - 10) {
            scrollAmount = - maxY + height - 10;
        } else if (scrollAmount < - maxY + height - 10 && maxY <= height - 10) {
            scrollAmount = 20;
        }
    }


    @Override
    protected void init() {
        super.init();
        installButton.setDimensions(52, 14);
        siteButton.setDimensions(55, 14);
        this.addSelectableChild(doneButton);
        this.addSelectableChild(installButton);
        this.addSelectableChild(siteButton);
        Thread thread = new Thread(() -> {
            if (!initialized) {
                ImageLoader.loadPlaceholder(iconTextureId);
            }
            ImageLoader.loadIcon(projectInfo, iconTextureId, Thread.currentThread());
        });
        thread.start();
        if (!initialized) {
            JsonObject jsonObject = EasyInstallClient.getProject(this.projectInfo.getSlug());
            String body = jsonObject.get("body").getAsString();
            projectInfo.setBody(body);
            JsonArray gallery = jsonObject.get("gallery").getAsJsonArray();
            for (int i = 0; i < gallery.size(); i++) {
                try {
                    galleryImages.add(new GalleryImage(Identifier.of(EasyInstall.MOD_ID, "gallery_image_" + i), URI.create(gallery.get(i).getAsJsonObject().get("url").getAsString()).toURL(), gallery.get(i).getAsJsonObject().get("description").getAsString()));
                } catch (UnsupportedOperationException e) {
                    try {
                        galleryImages.add(new GalleryImage(Identifier.of(EasyInstall.MOD_ID, "gallery_image__" + i), URI.create(gallery.get(i).getAsJsonObject().get("url").getAsString()).toURL()));
                    } catch (MalformedURLException exception) {
                        throw new RuntimeException(exception);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                try {
                    galleryImages.get(i).setTitle(gallery.get(i).getAsJsonObject().get("title").getAsString());
                } catch (UnsupportedOperationException ignored) {

                }
            }
            this.descriptionTab = new DescriptionTab(Text.of("Description"), this);
            initialized = true;
        }
        descriptionTab.refreshLinkPositions();
        GalleryTab galleryTab = new GalleryTab(Text.of("Gallery"), this);
        this.versionsTab = new VersionsTab(Text.of("Versions"), this);
        tabManager = new TabManager(this::addSelectableChild, this::remove);
        if (!galleryImages.isEmpty()) {
            tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, width - 131).tabs(descriptionTab, galleryTab, versionsTab).build(); //width - 131
        } else {
            tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, width - 131).tabs(descriptionTab, versionsTab).build(); // width - 131

        }
        tabNavigationWidget.init();
        if (tabNavigationWidget instanceof TabNavigationMixinInterface) {
            ((TabNavigationMixinInterface) tabNavigationWidget).setButtonWidth((this.width - 130)/(tabNavigationWidget.children().size()));
        }
        tabNavigationWidget.selectTab(0, false);
        this.addSelectableChild(tabNavigationWidget);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scrollDelta = (int) (verticalAmount * 13);
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
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TAB_HEADER_BACKGROUND_TEXTURE, 0, 0, 0.0F, 0.0F, this.width, ((TabButtonWidget) this.tabNavigationWidget.children().getFirst()).getHeight(), 16, 16);
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

    public ProjectInfo getProjectInfo() {
        return this.projectInfo;
    }

    @Override
    public <T extends Element & Selectable> T addSelectableChild(T child) {
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

    @Override
    public void removeChild(Element c) {
        this.remove(c);
    }

    public boolean isFilteredByGameVersion() {
        return filteredByGameVersion;
    }

    public int getMaxY() {
        return this.maxY;
    }
}