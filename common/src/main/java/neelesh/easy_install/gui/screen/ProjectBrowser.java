package neelesh.easy_install.gui.screen;

import neelesh.easy_install.*;
import neelesh.easy_install.gui.widget.PressableTextWidgetShadowless;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class ProjectBrowser extends Screen {
    private Identifier[] ICON_TEXTURE_ID = new Identifier[100];
    private static final Identifier SCROLLER_TEXTURE = Identifier.withDefaultNamespace("widget/scroller");
    public static final Identifier SCROLLER_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("widget/scroller_background");
    private ProjectInfo[] INFO = EasyInstallClient.getProjectInformation();
    private double scrollAmount = 0;
    private EditBox searchBox;
    private Button[] installButtons = new Button[100];
    private Button[] projectScreenButtons = new Button[100];
    private PressableTextWidgetShadowless[] authors = new PressableTextWidgetShadowless[100];
    private Screen prevScreen;
    private static Thread t;
    private static Thread searchThread;
    private final ProjectType projectType;
    private final Button doneButton = Button.builder(Component.nullToEmpty("Done"), button -> {
        minecraft.gui.setScreen(prevScreen);
    }).build();
    private Button backButton;
    private Button nextButton;
    private Button firstPage;
    private Button lastPage;
    private Button[] pageButtons = new Button[3];
    private CycleButton<Integer> showPerPage;
    private CycleButton<String> sortButton;
    private int pageNumber;
    private static boolean showingFilterOptions = false;
    private static int firstRowY = 35;
    private boolean isScrolling;
    private HashSet<String> categories = new HashSet<String>();
    private boolean initialized;
    private final Identifier FILTER_TEXTURE = Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "textures/gui/filter_icon.png");
    private final Identifier UPDATE_TEXTURE = Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "textures/gui/update_icon.png");
    public static final Identifier SETTINGS_TEXTURE = Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "textures/gui/settings.png");

    private final Button filtersButton = Button.builder(Component.nullToEmpty(""), button -> {
        showingFilterOptions = !showingFilterOptions;
    }).build();
    private Button updateScreenButton;
    private Button categoriesButton;
    private boolean filteredByGameVersion;
    private Button settingsButton;
    private ArrayList<Version> updatedVersions;

    private Environment environment = null;

    public ProjectBrowser(Screen parent, ProjectType projectType) {
        super(Component.literal(""));
        EasyInstallClient.setNumUpdates(0);
        filtersButton.setTooltip(Tooltip.create(Component.nullToEmpty("Show Filter Options")));
        EasyInstallClient.search("", projectType);
        prevScreen = parent;
        this.projectType = projectType;
        for (int i = 0; i < 100; i++) {
            ICON_TEXTURE_ID[i] = Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "icon" + i);
        }
        this.pageNumber = 0;
        this.filteredByGameVersion = true;
        EasyInstallClient.resetTargetUpdateVersion();

    }

    @Override
    protected void init() {
        super.init();
        showPerPage = CycleButton.<Integer>builder(integer -> Component.nullToEmpty(String.valueOf(integer)), EasyInstallClient::getRowsOnPage).withValues(5, 10, 15, 20, 50, 100).create(55, 22, 105, 18, Component.nullToEmpty("Show per page"), (button, value) -> {
            pageNumber = 0;
            EasyInstallClient.setRowsOnPage(value);
            search(searchBox.getValue());
            scrollAmount = 0;
        });
//        showPerPage.setValue(EasyInstallClient.getRowsOnPage());
        sortButton = CycleButton.<String>builder(Component::nullToEmpty, EasyInstallClient::getSortMethod).withValues("Relevance", "Downloads", "Follows", "Newest", "Updated").create(165, 22, 90, 18, Component.nullToEmpty("Sort"), (button, value) -> {
            pageNumber = 0;
            EasyInstallClient.setSortMethod(value);
            search(searchBox.getValue());
            scrollAmount = 0;
        });
//        sortButton.setValue(EasyInstallClient.getSortMethod());
        categoriesButton = Button.builder(Component.nullToEmpty("Select Categories"), button -> {
            Minecraft.getInstance().gui.setScreen(new CategoryScreen(this, projectType));
        }).build();
        categoriesButton.setSize(100, 18);
        categoriesButton.setPosition(260, 22);
        this.addWidget(categoriesButton);
        settingsButton = Button.builder(Component.nullToEmpty(""), button -> {
            Minecraft.getInstance().gui.setScreen(new SettingsScreen(this, this.projectType));
        }).build();
        settingsButton.setSize(20, 20);
        settingsButton.setPosition(width - 30, 0);
        settingsButton.setTooltip(Tooltip.create(Component.nullToEmpty("Settings")));
        this.addWidget(settingsButton);
        updateScreenButton = Button.builder(Component.nullToEmpty(""), button -> {
            minecraft.gui.setScreen(new UpdateScreen(projectType, this));
        }).build();
        updateScreenButton.setSize(20, 20);
        updateScreenButton.setPosition(width - 55, 0);
        updateScreenButton.setTooltip(Tooltip.create(Component.nullToEmpty("See All Updates")));
        for (int i = 0; i < EasyInstallClient.getRowsOnPage(); i++) {
            int finalI = i;
            minecraft.execute(() -> {
                DynamicTexture texture = new DynamicTexture(() -> "", new NativeImage(1, 1, false));
                texture.getPixels().setPixel(0, 0, 0x00000000);
                texture.upload();
                minecraft.getTextureManager().register(ICON_TEXTURE_ID[finalI], texture);
            });
        }
        this.addWidget(updateScreenButton);
        this.addWidget(showPerPage);
        this.addWidget(sortButton);
        String text;
        if (searchBox != null) {
            text = searchBox.getValue();
            searchBox = new EditBox(font, 55, 0, width / 3, 20, Component.literal("Search"));
            searchBox.setValue(text);
        } else {
            searchBox = new EditBox(font, 55, 0, width / 3, 20, Component.literal("Search"));
        }
        this.searchBox.setHint(Component.literal("Search...").withColor(CommonColors.LIGHT_GRAY));
        if (!initialized) {
            search(searchBox.getValue());
            initialized = true;
        } else {
            Thread thread = new Thread(() -> EasyInstallClient.checkStatus(projectType));
            thread.start();
            Thread thread2 = new Thread(this::loadIcons);
            thread2.start();
            Thread thread3 = new Thread(() -> this.updatedVersions = EasyInstallClient.getUpdatedVersions(projectType));
            thread3.start();


        }
        doneButton.setPosition(width / 3 + 65, 0);
        doneButton.setWidth(130);
        backButton = Button.builder(Component.nullToEmpty("<"), button -> {
            pageNumber--;
            search(searchBox.getValue());

        }).build();
        backButton.setSize(20, 20);
        nextButton = Button.builder(Component.nullToEmpty(">"), button -> {
            pageNumber++;
            search(searchBox.getValue());
        }).build();
        firstPage = Button.builder(Component.nullToEmpty("1"), button -> {
            pageNumber = 0;
            search(searchBox.getValue());
        }).build();
        firstPage.setSize(20, 20);
        lastPage = Button.builder(Component.nullToEmpty(String.valueOf(EasyInstallClient.getTotalPages())), button -> {
            pageNumber = EasyInstallClient.getTotalPages() - 1;
            search(searchBox.getValue());
        }).build();
        lastPage.setSize(20, 20);
        nextButton.setSize(20, 20);
        for (int i = 1; i < 4; i++) {
            Button pageButton = Button.builder(Component.nullToEmpty(String.valueOf(i)), button -> {
                pageNumber = Integer.parseInt(button.getMessage().getString()) - 1;
                if (pageNumber > 1) {
                    int index = List.of(pageButtons).indexOf(button);
                    Button temp = pageButtons[index];
                    pageButtons[index] = pageButtons[1];
                    pageButtons[1] = temp;
                }
                search(searchBox.getValue());
            }).build();
            pageButton.setSize(20, 20);
            this.addWidget(pageButton);
            pageButtons[i - 1] = pageButton;
        }
        filtersButton.setSize(20, 20);
        filtersButton.setPosition(width / 3 + 205, 0);
        //        versionButton.setPosition(width/6 + 75, 20);
        this.addWidget(searchBox);
        this.addWidget(doneButton);
        this.addWidget(backButton);
        this.addWidget(nextButton);
        this.addWidget(firstPage);
        this.addWidget(lastPage);
        this.addWidget(filtersButton);
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            Button buttonWidget = Button.builder(Component.nullToEmpty("Install"), button -> {
                INFO[finalI].setInstalling(true);
                Thread thread = new Thread(() -> {
                    if (!INFO[finalI].isUpdated()) {
                        Thread thread2 = new Thread(() -> {
                            EasyInstallClient.deleteOldFiles(projectType, INFO[finalI].getLatestHash());
                        });
                        thread2.start();
                        for (Version version : updatedVersions) {
                            if (version.getId().equals(INFO[finalI].getId())) {
                                version.download(false);
                            }
                        }
                    } else {
                        EasyInstallClient.downloadVersion(INFO[finalI].getSlug(), projectType, filteredByGameVersion);
                    }
                    INFO[finalI].setInstalling(false);
                    INFO[finalI].setInstalled(true);
                    t = new Thread(() -> EasyInstallClient.checkStatus(projectType));
                    t.start();
                    Thread thread2 = new Thread(() -> this.updatedVersions = EasyInstallClient.getUpdatedVersions(projectType));
                    thread2.start();

                });
                thread.start();
            }).build();
            buttonWidget.setSize(52, 14);

            installButtons[i] = buttonWidget;
            installButtons[i].setX(width - 70);
            Button projectButtonWidget = Button.builder(Component.nullToEmpty("More Info"), button -> {
                minecraft.gui.setScreen(new ProjectScreen(this, INFO[finalI], this.updatedVersions));
            }).build();
            projectButtonWidget.setSize(60, 14);
            projectScreenButtons[i] = projectButtonWidget;
            projectScreenButtons[i].setX(width - 150);

            this.addWidget(buttonWidget);
            this.addWidget(projectButtonWidget);

        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (showingFilterOptions) {
            firstRowY = 55;
        } else {
            firstRowY = 35;
        }
        updateScreenButton.visible = EasyInstallClient.getNumUpdates() >= 1;
        context.enableScissor(0, firstRowY - 14, width, height);
        extractMenuBackground(context);
        for (int i = 0; i < EasyInstallClient.getNumRows(); i++) {
            try {
                context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE_ID[i], 0, firstRowY + (int) scrollAmount + i * 50, 0, 0, 40, 40, 40, 40);
                context.text(font, INFO[i].getTitle(), 55, firstRowY + (int) scrollAmount + i * 50, CommonColors.WHITE, false);
                context.text(font, "by ", 75 + font.width(INFO[i].getTitle()), firstRowY + (int) scrollAmount + i * 50, CommonColors.WHITE, false);
                int finalI = i;
                if (children().contains(authors[i])) {
                    removeWidget(authors[i]);
                }
                authors[i] = new PressableTextWidgetShadowless(0, 0, font.width(Component.translationArg(Component.nullToEmpty(INFO[i].getAuthor()))), 9, Component.literal(INFO[i].getAuthor()), button -> {
                    minecraft.gui.setScreen(new ProfileScreen(INFO[finalI].getAuthor(), this));
                }, font);
                authors[i].setPosition(75 + font.width(INFO[i].getTitle() + "by "), firstRowY + (int) scrollAmount + i * 50);
                authors[i].extractRenderState(context, mouseX, mouseY, delta);
                this.addWidget(authors[i]);
                context.textWithWordWrap(font, FormattedText.of(INFO[i].getDescription().replace("\n", "")), 55, firstRowY + (int) scrollAmount + i * 50 + 15, width - 65, CommonColors.WHITE, false);
                installButtons[i].setY(firstRowY + (int) scrollAmount + i * 50 - 3);
                if (INFO[i].isInstalling()) {
                    installButtons[i].setMessage(Component.nullToEmpty("Installing"));
                } else if (INFO[i].isInstalled()) {
                    installButtons[i].setMessage(Component.nullToEmpty("Installed"));
                } else if (INFO[i].isUpdated()) {
                    installButtons[i].setMessage(Component.nullToEmpty("Install"));
                } else {
                    installButtons[i].setMessage(Component.nullToEmpty("Update"));
                }
                installButtons[i].active = !INFO[i].isInstalled() && !INFO[i].isInstalling();
                installButtons[i].extractRenderState(context, mouseX, mouseY, delta);
                projectScreenButtons[i].setY(firstRowY + (int) scrollAmount + i * 50 - 3);
                projectScreenButtons[i].extractRenderState(context, mouseX, mouseY, delta);


            } catch (NullPointerException ignored) {

            }
        }
        int scrollBarHeight = Math.max(35, (int) (Math.pow(height - firstRowY + 13, 2) / (EasyInstallClient.getNumRows() * 50 + 35)));
        double scrollBarY = firstRowY - 13;
        if (-50 * EasyInstallClient.getNumRows() - firstRowY + height - 35 < 0) {
            scrollBarY = scrollAmount * (height - firstRowY + 13 - scrollBarHeight) / (-50 * EasyInstallClient.getNumRows() - firstRowY + height - 35) + firstRowY - 13;
        }
        if (scrollBarHeight < height - firstRowY + 13) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND_TEXTURE, width - 6, 0, 6, EasyInstallClient.getNumRows() * 50 + 100);
            context.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_TEXTURE, width - 6, (int) scrollBarY, 6, scrollBarHeight);
        }
        for (int i = 0; i < 100; i++) {
            projectScreenButtons[i].visible = i < showPerPage.getValue() && projectScreenButtons[i].getY() > firstRowY - 26;
            installButtons[i].visible = i < showPerPage.getValue() && installButtons[i].getY() > firstRowY - 26;
            if (authors[i] != null){
                authors[i].visible = i < showPerPage.getValue() && authors[i].getY() > firstRowY - 26;
            }
        }
        backButton.extractRenderState(context, mouseX, mouseY, delta);
        backButton.active = pageNumber != 0;
        if (EasyInstallClient.getTotalPages() <= 5) {
            backButton.setPosition(width / 2 - 60 + 12 * (3 - EasyInstallClient.getTotalPages()), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
            nextButton.setPosition(width / 2 + 40 - 12 * (3 - EasyInstallClient.getTotalPages()), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
            firstPage.setPosition(width / 2 - 35 + 12 * (3 - EasyInstallClient.getTotalPages()), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
            lastPage.setPosition(width / 2 + 15 - 12 * (3 - EasyInstallClient.getTotalPages()), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
        } else {
            backButton.setPosition(width / 2 - 110, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
            nextButton.setPosition(width / 2 + 90, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
            firstPage.setPosition(width / 2 - 85, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
            lastPage.setPosition(width / 2 + 65, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
        }
        lastPage.visible = EasyInstallClient.getTotalPages() > 1;
        nextButton.extractRenderState(context, mouseX, mouseY, delta);
        nextButton.active = pageNumber != EasyInstallClient.getTotalPages() - 1;
        for (int i = 0; i < pageButtons.length; i++) {
            if (pageButtons[i] != null) {
                if (EasyInstallClient.getTotalPages() <= 5 || pageNumber <= 1) {
                    pageButtons[i].setMessage(Component.nullToEmpty(String.valueOf(i + 2)));
                    pageButtons[i].active = i != pageNumber - 1;
                } else if (pageNumber < EasyInstallClient.getTotalPages() - 2) {
                    pageButtons[i].setMessage(Component.nullToEmpty(String.valueOf(pageNumber + i)));
                    pageButtons[i].active = i != 1;
                } else if (pageNumber >= EasyInstallClient.getTotalPages() - 2) {

                    pageButtons[i].setMessage(Component.nullToEmpty(String.valueOf(EasyInstallClient.getTotalPages() + i - 3)));
                    pageButtons[i].active = i != 3 - EasyInstallClient.getTotalPages() + pageNumber + 1;
                }


                if (EasyInstallClient.getTotalPages() <= 5) {
                    pageButtons[i].setPosition(width / 2 - 85 + (int) (12.5 * (5 - EasyInstallClient.getTotalPages())) + 25 * (i + 2), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
                } else if ((pageNumber < EasyInstallClient.getTotalPages() - 3 && pageNumber >= 3)) {
                    pageButtons[i].setPosition(width / 2 - 85 + 25 * (i + 2), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
                    context.text(font, "—", width / 2 - 53, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50 + 7, CommonColors.WHITE, true);
                    context.text(font, "—", width / 2 + 46, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50 + 7, CommonColors.WHITE, true);
                } else if (pageNumber < 3) {
                    pageButtons[i].setPosition(width / 2 - 110 + 25 * (i + 2), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
                    context.text(font, "—", width / 2 + 29 + 5 * i, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50 + 7, CommonColors.WHITE, true);
                } else {
                    pageButtons[i].setPosition(width / 2 - 60 + 25 * (i + 2), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
                    context.text(font, "—", width / 2 - 46 + 5 * i, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50 + 7, CommonColors.WHITE, true);
                }
                pageButtons[i].visible = Integer.parseInt(pageButtons[i].getMessage().getString()) < EasyInstallClient.getTotalPages() && Integer.parseInt(pageButtons[i].getMessage().getString()) > 1;
                pageButtons[i].extractRenderState(context, mouseX, mouseY, delta);
            }
        }
        lastPage.active = pageNumber != EasyInstallClient.getTotalPages() - 1;
        lastPage.setMessage(Component.nullToEmpty(String.valueOf(EasyInstallClient.getTotalPages())));
        lastPage.extractRenderState(context, mouseX, mouseY, delta);
        firstPage.active = pageNumber != 0;
        firstPage.extractRenderState(context, mouseX, mouseY, delta);
        context.disableScissor();
        context.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.HEADER_SEPARATOR, 0, firstRowY - 15, 0, 0, width, 2, width, 2);
        showPerPage.visible = showingFilterOptions;
        showPerPage.extractRenderState(context, mouseX, mouseY, delta);
        sortButton.visible = showingFilterOptions;
        sortButton.extractRenderState(context, mouseX, mouseY, delta);
        filtersButton.extractRenderState(context, mouseX, mouseY, delta);
        searchBox.extractRenderState(context, mouseX, mouseY, delta);
        doneButton.extractRenderState(context, mouseX, mouseY, delta);
        updateScreenButton.extractRenderState(context, mouseX, mouseY, delta);
        categoriesButton.visible = showingFilterOptions;
        categoriesButton.extractRenderState(context, mouseX, mouseY, delta);
        settingsButton.extractRenderState(context, mouseX, mouseY, delta);
        context.blit(RenderPipelines.GUI_TEXTURED, FILTER_TEXTURE, filtersButton.getX() + 2, filtersButton.getY() + 2, 0, 0, 16, 16, 16, 16);
        context.blit(RenderPipelines.GUI_TEXTURED, SETTINGS_TEXTURE, settingsButton.getX() + 2, settingsButton.getY() + 2, -0.5f, 0, 16, 16, 16, 16);

        if (EasyInstallClient.getNumUpdates() >= 1) {
            context.blit(RenderPipelines.GUI_TEXTURED, UPDATE_TEXTURE, updateScreenButton.getX() + 3, updateScreenButton.getY() + 3, 0, 0, 14, 14, 14, 14);
        }
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollAmount + verticalAmount * 13 <= 0 && scrollAmount + verticalAmount * 13 >= -50 * EasyInstallClient.getNumRows() - (double) firstRowY + height - 35) {
            scrollAmount += verticalAmount * 13;
        } else if (scrollAmount + verticalAmount * 13 < -50 * EasyInstallClient.getNumRows() - (double) firstRowY + height - 35 && scrollAmount != 0) {
            scrollAmount = -50 * EasyInstallClient.getNumRows() - firstRowY + height - 35;
        } else if (scrollAmount + verticalAmount * 13 > 0) {
            scrollAmount = 0;
        }
        return true;
    }

    public void loadIcons() {
        int numberOfThreads = Runtime.getRuntime().availableProcessors() / 2 + 2;
        try (ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads)) {
            for (int i = 0; i < EasyInstallClient.getRowsOnPage(); i++) {
                int finalI = i;
                if (INFO[i] == null) {
                    break;
                }
                if (!Thread.currentThread().isInterrupted()) {
                    ImageLoader.loadPlaceholder(ICON_TEXTURE_ID[i]);
                    Thread thread = Thread.currentThread();
                    executorService.submit(() -> ImageLoader.loadIcon(INFO[finalI], ICON_TEXTURE_ID[finalI], thread));
                } else {
                    executorService.shutdownNow();
                    return;
                }
            }
            executorService.shutdown();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        String s = searchBox.getValue();
        boolean keyPressed = super.keyPressed(input);
        if (!s.equals(searchBox.getValue())) {
            pageNumber = 0;
            search(searchBox.getValue());
        }
        return keyPressed;
    }


    @Override
    public boolean charTyped(CharacterEvent input) {
        boolean charTyped = super.charTyped(input);
        if (searchBox.isHoveredOrFocused() && input.isAllowedChatCharacter()) {
            pageNumber = 0;
            search(searchBox.getValue());
        }
        return charTyped;
    }

    private void search(String query) {
        if (searchThread != null) {
            searchThread.interrupt();
        }
        if (t != null) {
            t.interrupt();
        }
        searchThread = new Thread(() -> {
            EasyInstallClient.search(query, projectType, pageNumber * EasyInstallClient.getRowsOnPage(), categories, filteredByGameVersion, environment);
            if (scrollAmount >= 0 || -50 * EasyInstallClient.getNumRows() - firstRowY + height - 35 >= 0) {
                scrollAmount = 0;
            } else if (scrollAmount < -50 * EasyInstallClient.getNumRows() - firstRowY + height - 35) {
                scrollAmount = -50 * EasyInstallClient.getNumRows() - firstRowY + height - 35;
            }
            if (!searchThread.isInterrupted()) {
                if (t != null) {
                    t.interrupt();
                }
                t = new Thread(() -> {
                    EasyInstallClient.checkStatus(this.projectType);
                });
                t.start();

                Thread thread2 = new Thread(() -> updatedVersions = EasyInstallClient.getUpdatedVersions(this.projectType));
                thread2.start();
                loadIcons();
            }
        });
        searchThread.start();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int scrollBarHeight = Math.max(35, (int) (Math.pow(height - firstRowY + 13, 2) / (EasyInstallClient.getNumRows() * 50 + 35)));
        double scrollBarY;
        if (-50 * EasyInstallClient.getNumRows() - firstRowY + height - 35 < 0) {
            scrollBarY = scrollAmount * (height - firstRowY + 13 - scrollBarHeight) / (-50 * EasyInstallClient.getNumRows() - firstRowY + height - 35) + firstRowY - 13;
        } else {
            scrollBarY = firstRowY - 13;
        }
        if (click.x() >= width - 6 && click.y() >= scrollBarY && click.y() <= scrollBarY + scrollBarHeight) {
            this.isScrolling = true;
        }
        return super.mouseClicked(click, doubled);
    }


    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (this.isScrolling && click.y() > firstRowY - 13) {
            double scrollBarHeight = Math.max(35.0, (Math.pow(height - firstRowY + 13, 2) / (EasyInstallClient.getNumRows() * 50 + 35)));
            double scrollDeltaY = (-50 * EasyInstallClient.getNumRows() - firstRowY + height - 35) * offsetY / (height - firstRowY + 13 - scrollBarHeight);

            if (scrollAmount + scrollDeltaY <= 0 && scrollAmount + scrollDeltaY >= -50 * EasyInstallClient.getNumRows() - firstRowY + height - 35) {
                scrollAmount += scrollDeltaY;
            } else if (scrollAmount + scrollDeltaY > 0) {
                scrollAmount = 0;
            } else if (scrollAmount + scrollDeltaY < -50 * EasyInstallClient.getNumRows() - firstRowY + height - 35 && scrollAmount != 0) {
                scrollAmount = -50 * EasyInstallClient.getNumRows() - firstRowY + height - 35;
            }
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        this.isScrolling = false;
        return super.mouseReleased(click);
    }

    public void addFilterCategory(String category) {
        categories.add(category);
    }

    public void removeFilterCategory(String category) {
        categories.remove(category);
    }

    public HashSet<String> getCategories() {
        return this.categories;
    }

    public void setPage(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public void clearCategories() {
        categories.clear();
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isFilteredByGameVersion() {
        return filteredByGameVersion;
    }

    public void setFilteredByGameVersion(boolean isFiltered) {
        this.filteredByGameVersion = isFiltered;
    }


    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public Environment getEnvironment() {
        return this.environment;
    }
}