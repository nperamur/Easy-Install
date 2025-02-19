package neelesh.easy_install.gui.screen;

import neelesh.easy_install.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectBrowser extends Screen {
    //Filter By Versions Button/Feature: Experimental and may or may not be implemented
    //Right now the code is commented out. Here's the plan:
    //Shows for: Current Version/ All Versions
    //Tooltip:
    //[Current Version] - Lists all projects available for this version of Minecraft
    //All Versions - Lists all projects available regardless of the version of Minecraft. Use as your own risk.

    private Identifier[] ICON_TEXTURE_ID = new Identifier[100];
    private static final Identifier SCROLLER_TEXTURE = Identifier.ofVanilla("widget/scroller");
    public static final Identifier SCROLLER_BACKGROUND_TEXTURE = Identifier.ofVanilla("widget/scroller_background");
    private ProjectInfo[] INFO = EasyInstallClient.getProjectInformation();
    private double scrollAmount = 0;
    private TextFieldWidget searchBox;
    private ButtonWidget[] installButtons = new ButtonWidget[100];
    private ButtonWidget[] projectScreenButtons = new ButtonWidget[100];
    private Screen prevScreen;
    private static Thread t;
    private static Thread searchThread;
    private final ProjectType projectType;
    private final ButtonWidget doneButton = ButtonWidget.builder(Text.of("Done"), button -> {
        client.setScreen(prevScreen);
    }).build();
    private ButtonWidget backButton;
    private ButtonWidget nextButton;
    private ButtonWidget firstPage;
    private ButtonWidget lastPage;
    private ButtonWidget[] pageButtons = new ButtonWidget[3];
    private CyclingButtonWidget<Integer> showPerPage;
    private CyclingButtonWidget<String> sortButton;
    private int pageNumber;
    private static boolean showingFilterOptions = false;
    private static int firstRowY = 35;
    private boolean isScrolling;
    private HashSet<String> categories = new HashSet<String>();
    private boolean initialized;
//    private boolean displayingCurrentVersion;
    //    private ButtonWidget versionButton;
    private final Identifier FILTER_TEXTURE = Identifier.of(EasyInstall.MOD_ID, "textures/gui/filter_icon.png");
    private final Identifier UPDATE_TEXTURE = Identifier.of(EasyInstall.MOD_ID, "textures/gui/update_icon.png");
    private final ButtonWidget filtersButton = ButtonWidget.builder(Text.of(""), button -> {
        showingFilterOptions = !showingFilterOptions;
        //versionButton.visible = showingFilterOptions;
    }).build();
    private ButtonWidget updateScreenButton;
    private ButtonWidget categoriesButton;


    public ProjectBrowser(Screen parent, ProjectType projectType) {
        super(Text.literal(""));
        EasyInstallClient.setNumUpdates(0);
        filtersButton.setTooltip(Tooltip.of(Text.of("Show Filter Options")));
        EasyInstallClient.search("", projectType);
        prevScreen = parent;
        this.projectType = projectType;
        for (int i = 0; i < 100; i++) {
            ICON_TEXTURE_ID[i] = Identifier.of(EasyInstall.MOD_ID, "icon" + i);
        }
        this.pageNumber = 0;

    }

    @Override
    protected void init() {
        super.init();
        showPerPage = CyclingButtonWidget.<Integer>builder(value -> Text.of(String.valueOf(value))).values(5, 10, 15, 20, 50, 100).build(60, 22, 105, 18, Text.of("Show per page"), (button, value) -> {
            pageNumber = 0;
            EasyInstallClient.setRowsOnPage(value);
            search(searchBox.getText());
            scrollAmount = 0;
        });
        showPerPage.setValue(EasyInstallClient.getRowsOnPage());
        sortButton = CyclingButtonWidget.<String>builder(Text::of).values("Relevance", "Downloads", "Follows", "Newest", "Updated").build(170, 22, 90, 18, Text.of("Sort"), (button, value) -> {
            pageNumber = 0;
            EasyInstallClient.setSortMethod(value);
            search(searchBox.getText());
            scrollAmount = 0;
        });
        sortButton.setValue(EasyInstallClient.getSortMethod());
        categoriesButton = ButtonWidget.builder(Text.of("Select Categories"), button -> {
            MinecraftClient.getInstance().setScreen(new CategoryScreen(this, projectType));
        }).build();
        categoriesButton.setDimensions(100, 18);
        categoriesButton.setPosition(265, 22);
        this.addSelectableChild(categoriesButton);
        updateScreenButton = ButtonWidget.builder(Text.of(""), button -> {
            client.setScreen(new UpdateScreen(projectType, this));
        }).build();
        updateScreenButton.setDimensions(20, 20);
        updateScreenButton.setPosition(width - 30, 0);
        updateScreenButton.setTooltip(Tooltip.of(Text.of("See All Updates")));
        for (int i = 0; i < EasyInstallClient.getRowsOnPage(); i++) {
            int finalI = i;
            client.execute(() -> {
                NativeImageBackedTexture texture = new NativeImageBackedTexture(new NativeImage(1, 1, false));
                texture.getImage().setColorArgb(0, 0, 0x00000000);
                texture.upload();
                client.getTextureManager().registerTexture(ICON_TEXTURE_ID[finalI], texture);
            });
        }

        //        }



        //        versionButton = ButtonWidget.builder(Text.of("Versions: 1.21.3"), button -> {
        //            if (displayingCurrentVersion) {
        //                button.setMessage(Text.of("Versions: All"));
        //                displayingCurrentVersion = false;
        //            } else {
        //                button.setMessage(Text.of("Versions: 1.21.3"));
        //                displayingCurrentVersion = true;
        //            }
        //            Thread thread = new Thread(() -> {
        //                TestingClient.search(searchBox.getText(), projectType);
        //                if (t != null) {
        //                    t.interrupt();
        //                }
        //                t = new Thread(() -> {
        //                    TestingClient.updateInstalled(this.projectType);
        //                });
        //                t.start();
        //                loadIcons();
        //            });
        //            thread.start();
        //
        //        }).build();
        //        displayingCurrentVersion = true;
        //        this.addSelectableChild(versionButton);
        //        versionButton.setDimensions(90, 20);
        this.addSelectableChild(updateScreenButton);
        this.addSelectableChild(showPerPage);
        this.addSelectableChild(sortButton);
        String text;
        if (searchBox != null) {
            text = searchBox.getText();
            searchBox = new TextFieldWidget(textRenderer, 60, 0, width / 3, 20, Text.literal("Search"));
            searchBox.setText(text);
        } else {
            searchBox = new TextFieldWidget(textRenderer, 60, 0, width / 3, 20, Text.literal("Search"));
        }
        this.searchBox.setPlaceholder(Text.of("Search..."));
        if (!initialized) {
            search(searchBox.getText());
            initialized = true;
        } else {
            Thread thread = new Thread(() -> EasyInstallClient.checkStatus(projectType));
            thread.start();
            Thread thread2 = new Thread(this::loadIcons);
            thread2.start();


        }

        doneButton.setPosition(width / 3 + 70, 0);
        doneButton.setWidth(130);
        backButton = ButtonWidget.builder(Text.of("<"), button -> {
            pageNumber--;
            search(searchBox.getText());

        }).build();
        backButton.setDimensions(20, 20);
        nextButton = ButtonWidget.builder(Text.of(">"), button -> {
            pageNumber++;
            search(searchBox.getText());
        }).build();
        firstPage = ButtonWidget.builder(Text.of("1"), button -> {
            pageNumber = 0;
            search(searchBox.getText());
        }).build();
        firstPage.setDimensions(20, 20);
        lastPage = ButtonWidget.builder(Text.of(String.valueOf(EasyInstallClient.getTotalPages())), button -> {
            pageNumber = EasyInstallClient.getTotalPages() - 1;
            search(searchBox.getText());
        }).build();
        lastPage.setDimensions(20, 20);
        nextButton.setDimensions(20, 20);
        for (int i = 1; i < 4; i++) {
            ButtonWidget pageButton = ButtonWidget.builder(Text.of(String.valueOf(i)), button -> {
                pageNumber = Integer.parseInt(button.getMessage().getString()) - 1;
                if (pageNumber > 1) {
                    int index = List.of(pageButtons).indexOf(button);
                    ButtonWidget temp = pageButtons[index];
                    pageButtons[index] = pageButtons[1];
                    pageButtons[1] = temp;
                }
                search(searchBox.getText());
            }).build();
            pageButton.setDimensions(20, 20);
            this.addSelectableChild(pageButton);
            pageButtons[i - 1] = pageButton;
        }
        filtersButton.setDimensions(20, 20);
        filtersButton.setPosition(width / 3 + 210, 0);
        //        versionButton.setPosition(width/6 + 75, 20);
        this.addSelectableChild(searchBox);
        this.addSelectableChild(doneButton);
        this.addSelectableChild(backButton);
        this.addSelectableChild(nextButton);
        this.addSelectableChild(firstPage);
        this.addSelectableChild(lastPage);
        this.addSelectableChild(filtersButton);
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            ButtonWidget buttonWidget = ButtonWidget.builder(Text.of("Install"), button -> {
                INFO[finalI].setInstalling(true);
                Thread thread = new Thread(() -> {
                    if (!INFO[finalI].isUpdated()) {
                        Thread thread2 = new Thread(() -> {
                            HashMap<String, String> oldHashes = EasyInstallClient.getOldHashes();
                            File dir = new File(EasyInstallClient.getDir(projectType));
                            File[] files = dir.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    try {
                                        String hash = EasyInstallClient.createFileHash(file.toPath());
                                        if (oldHashes.containsKey(hash) && oldHashes.get(hash).equals(INFO[finalI].getLatestHash())) {
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
                    EasyInstallClient.downloadVersion(INFO[finalI].getSlug(), projectType);
                    INFO[finalI].setInstalling(false);
                    INFO[finalI].setInstalled(true);
                    t = new Thread(() -> {
                        EasyInstallClient.checkStatus(projectType);
                    });
                    t.start();
                });
                thread.start();
            }).build();
            buttonWidget.setDimensions(52, 14);
            installButtons[i] = buttonWidget;
            installButtons[i].setX(width - 70);
            ButtonWidget projectButtonWidget = ButtonWidget.builder(Text.of("More Info"), button -> {

                client.setScreen(new ProjectScreen(this, INFO[finalI]));

            }).build();

            projectButtonWidget.setDimensions(60, 14);
            projectScreenButtons[i] = projectButtonWidget;
            projectScreenButtons[i].setX(width - 150);
            this.addSelectableChild(buttonWidget);
            this.addSelectableChild(projectButtonWidget);

        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (showingFilterOptions) {
            firstRowY = 55;
        } else {
            firstRowY = 35;
        }
        updateScreenButton.visible = EasyInstallClient.getNumUpdates() >= 1;
        context.enableScissor(0, firstRowY - 14, width, height);
        renderDarkening(context);
        for (int i = 0; i < EasyInstallClient.getNumRows(); i++) {
            try {
                context.drawTexture(RenderLayer::getGuiTextured, ICON_TEXTURE_ID[i], 0, firstRowY + (int) scrollAmount + i * 50, 0, 0, 40, 40, 40, 40);
                context.drawText(textRenderer, INFO[i].getTitle(), 60, firstRowY + (int) scrollAmount + i * 50, 0xFFFFFF, false);
                context.drawText(textRenderer, "by " + INFO[i].getAuthor(), 60 + textRenderer.getWidth(INFO[i].getTitle()) + 20, firstRowY + (int) scrollAmount + i * 50, 0xFFFFFF, false);
                context.drawWrappedText(textRenderer, StringVisitable.plain(INFO[i].getDescription().replace("\n", "")), 60, firstRowY + (int) scrollAmount + i * 50 + 15, width - 70, 0xFFFFFF, false);
                installButtons[i].render(context, mouseX, mouseY, delta);
                installButtons[i].setY(firstRowY + (int) scrollAmount + i * 50 - 3);
                if (INFO[i].isInstalling()) {
                    installButtons[i].setMessage(Text.of("Installing"));
                } else if (INFO[i].isInstalled()) {
                    installButtons[i].setMessage(Text.of("Installed"));
                } else if (INFO[i].isUpdated()) {
                    installButtons[i].setMessage(Text.of("Install"));
                } else {
                    installButtons[i].setMessage(Text.of("Update"));
                }
                installButtons[i].active = !INFO[i].isInstalled() && !INFO[i].isInstalling();

                projectScreenButtons[i].render(context, mouseX, mouseY, delta);
                projectScreenButtons[i].setY(firstRowY + (int) scrollAmount + i * 50 - 2);
            } catch (NullPointerException ignored) {

            }
        }
        int scrollBarHeight = Math.max(35, (int) (Math.pow(height - firstRowY + 13, 2) / (EasyInstallClient.getNumRows() * 50 + 35)));
        double scrollBarY = firstRowY - 13;
        if (-50 * EasyInstallClient.getNumRows() - firstRowY + height - 35 < 0) {
            scrollBarY = scrollAmount * (height - firstRowY + 13 - scrollBarHeight) / (-50 * EasyInstallClient.getNumRows() - firstRowY + height - 35) + firstRowY - 13;
        }
        if (scrollBarHeight < height - firstRowY + 13) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, SCROLLER_BACKGROUND_TEXTURE, width - 6, 0, 6, EasyInstallClient.getNumRows() * 50 + 100);
            context.drawGuiTexture(RenderLayer::getGuiTextured, SCROLLER_TEXTURE, width - 6, (int) scrollBarY, 6, scrollBarHeight);
        }
        for (int i = 0; i < 100; i++) {
            projectScreenButtons[i].visible = i < showPerPage.getValue() && projectScreenButtons[i].getY() > firstRowY - 26;
            installButtons[i].visible = i < showPerPage.getValue() && installButtons[i].getY() > firstRowY - 26;
        }
        backButton.render(context, mouseX, mouseY, delta);
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
        nextButton.render(context, mouseX, mouseY, delta);
        nextButton.active = pageNumber != EasyInstallClient.getTotalPages() - 1;
        for (int i = 0; i < pageButtons.length; i++) {
            if (pageButtons[i] != null) {
                if (EasyInstallClient.getTotalPages() <= 5 || pageNumber <= 1) {
                    pageButtons[i].setMessage(Text.of(String.valueOf(i + 2)));
                    pageButtons[i].active = i != pageNumber - 1;
                } else if (pageNumber < EasyInstallClient.getTotalPages() - 2) {
                    pageButtons[i].setMessage(Text.of(String.valueOf(pageNumber + i)));
                    pageButtons[i].active = i != 1;
                } else if (pageNumber >= EasyInstallClient.getTotalPages() - 2) {

                    pageButtons[i].setMessage(Text.of(String.valueOf(EasyInstallClient.getTotalPages() + i - 3)));
                    pageButtons[i].active = i != 3 - EasyInstallClient.getTotalPages() + pageNumber + 1;
                }


                if (EasyInstallClient.getTotalPages() <= 5) {
                    pageButtons[i].setPosition(width / 2 - 85 + (int) (12.5 * (5 - EasyInstallClient.getTotalPages())) + 25 * (i + 2), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
                } else if ((pageNumber < EasyInstallClient.getTotalPages() - 3 && pageNumber >= 3)) {
                    pageButtons[i].setPosition(width / 2 - 85 + 25 * (i + 2), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
                    context.drawText(textRenderer, "—", width / 2 - 53, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50 + 7, 0xFFFFFF, true);
                    context.drawText(textRenderer, "—", width / 2 + 46, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50 + 7, 0xFFFFFF, true);
                } else if (pageNumber < 3) {
                    pageButtons[i].setPosition(width / 2 - 110 + 25 * (i + 2), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
                    context.drawText(textRenderer, "—", width / 2 + 29 + 5 * i, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50 + 7, 0xFFFFFF, true);
                } else {
                    pageButtons[i].setPosition(width / 2 - 60 + 25 * (i + 2), firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50);
                    context.drawText(textRenderer, "—", width / 2 - 46 + 5 * i, firstRowY + (int) scrollAmount + EasyInstallClient.getNumRows() * 50 + 7, 0xFFFFFF, true);
                }
                pageButtons[i].visible = Integer.parseInt(pageButtons[i].getMessage().getString()) < EasyInstallClient.getTotalPages() && Integer.parseInt(pageButtons[i].getMessage().getString()) > 1;
                pageButtons[i].render(context, mouseX, mouseY, delta);
            }
        }
        lastPage.active = pageNumber != EasyInstallClient.getTotalPages() - 1;
        lastPage.setMessage(Text.of(String.valueOf(EasyInstallClient.getTotalPages())));
        lastPage.render(context, mouseX, mouseY, delta);
        firstPage.active = pageNumber != 0;
        firstPage.render(context, mouseX, mouseY, delta);
        //        if (showingFilterOptions) {
        //            //versionButton.render(context, mouseX, mouseY, delta);
        //        }
        context.disableScissor();
        context.drawTexture(RenderLayer::getGuiTextured, CreateWorldScreen.HEADER_SEPARATOR_TEXTURE, 0, firstRowY - 15, 0, 0, width, 2, width, 2);
        showPerPage.visible = showingFilterOptions;
        showPerPage.render(context, mouseX, mouseY, delta);
        sortButton.visible = showingFilterOptions;
        sortButton.render(context, mouseX, mouseY, delta);
        filtersButton.render(context, mouseX, mouseY, delta);
        searchBox.render(context, mouseX, mouseY, delta);
        doneButton.render(context, mouseX, mouseY, delta);
        updateScreenButton.render(context, mouseX, mouseY, delta);
        categoriesButton.visible = showingFilterOptions;
        categoriesButton.render(context, mouseX, mouseY, delta);
        context.drawTexture(RenderLayer::getGuiTextured, FILTER_TEXTURE, filtersButton.getX() + 2, filtersButton.getY() + 2, 0, 0, 16, 16, 16, 16);
        if (EasyInstallClient.getNumUpdates() >= 1) {
            context.drawTexture(RenderLayer::getGuiTextured, UPDATE_TEXTURE, updateScreenButton.getX() + 3, updateScreenButton.getY() + 3, 0, 0, 14, 14, 14, 14);
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
                    client.execute(() -> {
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(new NativeImage(1, 1, false));
                        texture.getImage().setColorArgb(0, 0, 0x00000000);
                        texture.upload();
                        client.getTextureManager().registerTexture(ICON_TEXTURE_ID[finalI], texture);
                    });
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        String s = searchBox.getText();
        boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
        if (!s.equals(searchBox.getText())) {
            pageNumber = 0;
            search(searchBox.getText());
        }
        return keyPressed;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean charTyped = super.charTyped(chr, modifiers);
        if (searchBox.isSelected() && StringHelper.isValidChar(chr)) {
            pageNumber = 0;
            search(searchBox.getText());
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
            EasyInstallClient.search(query, projectType, pageNumber * EasyInstallClient.getRowsOnPage(), categories);
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
                loadIcons();
            }
        });
        searchThread.start();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int scrollBarHeight = Math.max(35, (int) (Math.pow(height - firstRowY + 13, 2) / (EasyInstallClient.getNumRows() * 50 + 35)));
        double scrollBarY;
        if (-50 * EasyInstallClient.getNumRows() - firstRowY + height - 35 < 0) {
            scrollBarY = scrollAmount * (height - firstRowY + 13 - scrollBarHeight) / (-50 * EasyInstallClient.getNumRows() - firstRowY + height - 35) + firstRowY - 13;
        } else {
            scrollBarY = firstRowY - 13;
        }
        if (mouseX >= width - 6 && mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarHeight) {
            this.isScrolling = true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isScrolling && mouseY > firstRowY - 13) {
            double scrollBarHeight = Math.max(35.0, (Math.pow(height - firstRowY + 13, 2) / (EasyInstallClient.getNumRows() * 50 + 35)));
            double scrollDeltaY = (-50 * EasyInstallClient.getNumRows() - firstRowY + height - 35) * deltaY / (height - firstRowY + 13 - scrollBarHeight);

            if (scrollAmount + scrollDeltaY <= 0 && scrollAmount + scrollDeltaY >= -50 * EasyInstallClient.getNumRows() - firstRowY + height - 35) {
                scrollAmount += scrollDeltaY;
            } else if (scrollAmount + scrollDeltaY > 0) {
                scrollAmount = 0;
            } else if (scrollAmount + scrollDeltaY < -50 * EasyInstallClient.getNumRows() - firstRowY + height - 35 && scrollAmount != 0) {
                scrollAmount = -50 * EasyInstallClient.getNumRows() - firstRowY + height - 35;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
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
}