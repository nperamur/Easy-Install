package neelesh.easy_install.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.Blaze3D;
import neelesh.easy_install.*;
import neelesh.easy_install.gui.tab.DescriptionTab;
import neelesh.easy_install.gui.tab.GalleryTab;
import neelesh.easy_install.gui.tab.TabNavigationMixinInterface;
import neelesh.easy_install.gui.tab.VersionsTab;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.TAB_HEADER_BACKGROUND;


public class ProjectScreen extends Screen implements MarkdownScreenInterface {
    private ProjectInfo projectInfo;
    private final Identifier iconTextureId;
    private int maxY;
    private ArrayList<GalleryImage> galleryImages = new ArrayList<>();
    private VersionsTab versionsTab;
    private boolean initialized;
    private Screen prevScreen;
    private boolean filteredByGameVersion;
    private ArrayList<Version> updatedVersions;
    private InstallationState installationState;
    private final Button installButton = Button.builder(Component.nullToEmpty("Install"), button -> {
        Thread thread = new Thread(() -> {
            this.updatedVersions = EasyInstallClient.getUpdatedVersions(projectInfo.getProjectType());
            boolean downloadSuccessful = false;
            if (installationState.needsUpdate(projectInfo.getId())) {
                installationState.setProjectStatus(projectInfo.getId(), ProjectStatus.INSTALLING_UPDATE);
                String versionDownloadedHash = null;
                for (Version version : updatedVersions) {
                    if (version.getId().equals(projectInfo.getId())) {
                        downloadSuccessful = version.download(false);
                        versionDownloadedHash = version.getHash();
                        break;
                    }
                }
                if (downloadSuccessful) {
                    EasyInstallClient.deleteOldFiles(projectInfo.getProjectType(), versionDownloadedHash);
                }
            } else {
                installationState.setProjectStatus(projectInfo.getId(), ProjectStatus.INSTALLING_REGULAR);
                downloadSuccessful = EasyInstallClient.downloadVersion(projectInfo.getSlug(), projectInfo.getProjectType(), ((ProjectBrowser) prevScreen).isFilteredByGameVersion());
            }
            boolean finalDownloadSuccessful = downloadSuccessful;
            Minecraft.getInstance().schedule(() -> {
                if (finalDownloadSuccessful) {
                    installationState.setProjectStatus(projectInfo.getId(),  ProjectStatus.INSTALLED);
                } else {
                    installationState.removeProject(projectInfo.getId());
                    minecraft.gui.toastManager().addToast(
                            new SystemToast(
                                    SystemToast.SystemToastId.WORLD_ACCESS_FAILURE,
                                    Component.literal("Download Failed").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                                    Component.literal("Could not download project: " + projectInfo.getTitle()).withStyle(ChatFormatting.WHITE)
                            )
                    );
                }
                versionsTab.setInitialized(false);
            });
            EasyInstallClient.checkStatus(projectInfo.getProjectType());
            Thread thread3 = new Thread(() -> this.updatedVersions = EasyInstallClient.getUpdatedVersions(projectInfo.getProjectType()));
            thread3.start();
        });
        thread.start();
    }).build();

    private final Button siteButton = Button.builder(Component.nullToEmpty("Modrinth↗"), button -> {
        try {
            Blaze3D.openUri(new URI("https://modrinth.com/project/" + projectInfo.getSlug()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }).build();

    private final Button doneButton = Button.builder(Component.nullToEmpty("Done"), button -> {
        Minecraft.getInstance().gui.setScreen(this.prevScreen);
    }).build();
    private DescriptionTab descriptionTab;
    private Tab prevTab;

    private TabManager tabManager;
    private MenuTabBar tabNavigationWidget;
    private int scrollAmount = 15;
    public static final Identifier VERTICAL_SEPARATOR_TEXTURE = Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID,"textures/gui/vertical_separator.png");

    protected ProjectScreen(Screen parent, ProjectInfo projectInfo, InstallationState installationState, ArrayList<Version> updatedVersions) {
        super(Component.literal(projectInfo.getTitle()));
        this.projectInfo = projectInfo;
        this.updatedVersions = updatedVersions;
        iconTextureId = Identifier.parse("project_texture_id");
        this.prevScreen = parent;
        this.installationState = installationState;
        this.filteredByGameVersion = ((ProjectBrowser) parent).isFilteredByGameVersion();
    }

    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        doneButton.setWidth(110);
        doneButton.setHeight(17);
        doneButton.setPosition(10, height - doneButton.getHeight());
        doneButton.extractRenderState(context, mouseX, mouseY, delta);
        if (this.tabManager == null) return;
        if (tabManager.getCurrentTab() instanceof VersionsTab && tabManager.getCurrentTab() != prevTab) {
            ((VersionsTab) (tabManager.getCurrentTab())).setInitialized(false);
        }
        prevTab = tabManager.getCurrentTab();
        ((Renderable) tabManager.getCurrentTab()).extractRenderState(context, mouseX, mouseY, delta);
        descriptionTab.setLinksActive(tabManager.getCurrentTab() instanceof DescriptionTab);
        versionsTab.setActive(tabManager.getCurrentTab() instanceof VersionsTab);
        for (int i = 0; i < tabNavigationWidget.children().size(); i++) {
            if (tabNavigationWidget instanceof TabNavigationMixinInterface) {
                ((TabNavigationMixinInterface) tabNavigationWidget).setX(131);
                ((TabNavigationMixinInterface) tabNavigationWidget).setY(scrollAmount - 10);
            }
            ((TabButton) tabNavigationWidget.children().get(i)).extractRenderState(context, mouseX, mouseY, delta);
        }
        float titleSize = 1.4f;
        context.pose().scale(titleSize, titleSize);
        context.textWithWordWrap(font, FormattedText.of(projectInfo.getTitle()), (int) (10 /titleSize), 40, (int) (110/titleSize), CommonColors.WHITE, false);
        int wrappedHeight =  font.wordWrapHeight(FormattedText.of(projectInfo.getTitle()), (int) (110/titleSize));
        context.pose().scale(1/titleSize, 1/titleSize);
        context.blit(RenderPipelines.GUI_TEXTURED, iconTextureId, 10, 0, 0, 0, 50, 50, 50, 50);
        context.textWithWordWrap(font, FormattedText.of(projectInfo.getDescription()), 10, (int) (65 + wrappedHeight*titleSize), 110, CommonColors.WHITE, false);
        installButton.setPosition(10, (int) ((65 + font.wordWrapHeight(FormattedText.of(projectInfo.getDescription()), 110) + wrappedHeight * titleSize + 10)));
        siteButton.setPosition(65, (int) ((65 + font.wordWrapHeight(FormattedText.of(projectInfo.getDescription()), 110) + wrappedHeight * titleSize + 10)));
        installButton.extractRenderState(context, mouseX, mouseY, delta);
        siteButton.extractRenderState(context, mouseX, mouseY, delta);

        ProjectStatus projectStatus = installationState.getProjectStatus(projectInfo.getId());
        if (projectStatus == ProjectStatus.INSTALLING_REGULAR || projectStatus == ProjectStatus.INSTALLING_UPDATE) {
            installButton.setMessage(Component.nullToEmpty("Installing"));
        } else if (projectStatus == ProjectStatus.INSTALLED) {
            installButton.setMessage(Component.nullToEmpty("Installed"));
        } else if (projectStatus == ProjectStatus.NONE) {
            installButton.setMessage(Component.nullToEmpty("Install"));
        } else if (projectStatus == ProjectStatus.NEEDS_UPDATE) {
            installButton.setMessage(Component.nullToEmpty("Update"));
        }
        installButton.active = projectStatus != ProjectStatus.INSTALLED && projectStatus != ProjectStatus.INSTALLING_REGULAR
                                                            && projectStatus != ProjectStatus.INSTALLING_UPDATE;
        if (scrollAmount < - maxY + height - 10 && maxY > height - 10) {
            scrollAmount = - maxY + height - 10;
        } else if (scrollAmount < - maxY + height - 10 && maxY <= height - 10) {
            scrollAmount = 20;
        }
    }


    @Override
    protected void init() {
        super.init();
        installButton.setSize(52, 14);
        siteButton.setSize(55, 14);
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
            if (jsonObject == null) return;
            String body = jsonObject.get("body").getAsString();
            projectInfo.setBody(body);
            JsonArray gallery = jsonObject.get("gallery").getAsJsonArray();
            for (int i = 0; i < gallery.size(); i++) {
                try {
                    galleryImages.add(new GalleryImage(Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "gallery_image_" + i), URI.create(gallery.get(i).getAsJsonObject().get("url").getAsString()).toURL(), gallery.get(i).getAsJsonObject().get("description").getAsString()));
                } catch (UnsupportedOperationException e) {
                    try {
                        galleryImages.add(new GalleryImage(Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "gallery_image__" + i), URI.create(gallery.get(i).getAsJsonObject().get("url").getAsString()).toURL()));
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
            this.descriptionTab = new DescriptionTab(Component.nullToEmpty("Description"), this);
            initialized = true;
        }
        descriptionTab.refreshLinkPositions();
        GalleryTab galleryTab = new GalleryTab(Component.nullToEmpty("Gallery"), this);
        this.versionsTab = new VersionsTab(Component.nullToEmpty("Versions"), this);
        tabManager = new TabManager(this::addSelectableChild, this::removeWidget);
        if (!galleryImages.isEmpty()) {
            tabNavigationWidget = MenuTabBar.builder(this.tabManager, width - 131)
                    .addTabs(new Tab[]{descriptionTab, galleryTab, versionsTab}).build(); //width - 131
        } else {
            tabNavigationWidget = MenuTabBar.builder(this.tabManager, width - 131)
                    .addTabs(new Tab[]{descriptionTab, versionsTab}).build(); // width - 131

        }
        tabNavigationWidget.arrangeElements(width - 131);
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
    protected void extractMenuBackground(GuiGraphicsExtractor context) {
        if (this.tabNavigationWidget == null) return;
        context.blit(RenderPipelines.GUI_TEXTURED, TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, ((TabButton) this.tabNavigationWidget.children().getFirst()).getHeight(), 16, 16);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        super.extractMenuBackground(context);

    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public int getScrollAmount() {
        return this.scrollAmount;
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        super.extractMenuBackground(graphics, x, y, width, height);
    }

    public void renderMenuBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        extractMenuBackground(graphics, x, y, width, height);
    }


    public ProjectInfo getProjectInfo() {
        return this.projectInfo;
    }

    @Override
    public <T extends GuiEventListener & NarratableEntry> T addSelectableChild(T child) {
        if (child != null) {
            return super.addWidget(child);
        }
        return null;
    }

    public TabManager getTabManager() {
        return tabManager;
    }

    public MenuTabBar getTabNavigationWidget() {
        return tabNavigationWidget;
    }

    public ArrayList<GalleryImage> getGalleryImages() {
        return galleryImages;
    }

    @Override
    public void removeChild(GuiEventListener c) {
        this.removeWidget(c);
    }

    public boolean isFilteredByGameVersion() {
        return filteredByGameVersion;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public InstallationState getInstallationState() {
        return installationState;
    }

    public String getProjectId() {
        return projectInfo.getId();
    }

    @Nullable
    public Version getUpdatedVersion() {
        if (updatedVersions == null) return null;
        for (Version version : updatedVersions) {
            if (version.getId().equals(projectInfo.getId())) {
                return version;
            }
        }
        return null;
    }
}