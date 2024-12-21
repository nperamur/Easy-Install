package neelesh.testing;

import com.terraformersmc.modmenu.gui.widget.LegacyTexturedButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static neelesh.testing.Testing.MOD_ID;

public class ProjectBrowser extends Screen {
    private final Identifier[] ICON_TEXTURE_ID = new Identifier[TestingClient.ROWS_ON_PAGE];
    private int scrollAmount = 0;
    private TextFieldWidget searchBox;
    private ButtonWidget[] installButtons = new ButtonWidget[TestingClient.ROWS_ON_PAGE];
    private final ButtonWidget[] projectScreenButtons = new ButtonWidget[TestingClient.ROWS_ON_PAGE];
    private Screen prevScreen;
    private static Thread t;
    private static Thread searchThread;
    private final ButtonWidget doneButton = ButtonWidget.builder(Text.of("Done"), button -> {
        client.setScreen(prevScreen);
    }).build();
    private final ModInfo[] INFO = TestingClient.getModInformation();
    private final ProjectType projectType;
    private static int n = 0;
    //private boolean showingFilterOptions;
    //private boolean displayingCurrentVersion;
    /**
    private ButtonWidget versionButton;
    private final Identifier FILTER_TEXTURE = Identifier.of(Testing.MOD_ID,"textures/gui/filter_icon.png");
    private final ButtonWidget filtersButton = ButtonWidget.builder(Text.of(""), button -> {
        if (!showingFilterOptions) {
            showingFilterOptions = true;
            this.addSelectableChild(versionButton);
        } else {
            showingFilterOptions = false;
            this.remove(versionButton);
        }
    }).build();
     */
    public ProjectBrowser(Screen parent, ProjectType projectType) {
        super(Text.literal(""));
        prevScreen = parent;
        this.projectType = projectType;
        for (int i = 0; i < TestingClient.ROWS_ON_PAGE; i++) {
            ICON_TEXTURE_ID[i] = Identifier.of(MOD_ID, "icon"+i);
        }
    }


    public ProjectBrowser(Screen parent) {
        this(parent, ProjectType.RESOURCE_PACK);
    }

    @Override
    protected void init() {
        super.init();
        //showingFilterOptions = false;
//        if (n == 0) {
//            loadIcons();
//        } else {
        for (int i = 0; i < TestingClient.ROWS_ON_PAGE; i++) {
            int finalI = i;
            client.execute(() -> {
                NativeImageBackedTexture texture = new NativeImageBackedTexture(new NativeImage(1, 1, false));
                texture.getImage().setColorArgb(0, 0, 0x00000000);
                texture.upload();
                client.getTextureManager().registerTexture(ICON_TEXTURE_ID[finalI], texture);
            });
        }
            Thread thread = new Thread(() -> {
                loadIcons();
            });
            thread.start();

//        }
        if (t != null) {
            t.interrupt();
        }
        t = new Thread(() -> {
            TestingClient.updateInstalled(projectType);
        });
        t.start();
        /**
        versionButton = ButtonWidget.builder(Text.of("Shows for: 1.21.3"), button -> {
            if (displayingCurrentVersion) {
                button.setMessage(Text.of("Shows for: All Versions"));
                displayingCurrentVersion = false;
            } else {
                button.setMessage(Text.of("Shows for: 1.21.3"));
                displayingCurrentVersion = true;
            }
            Thread thread = new Thread(() -> {
                TestingClient.search(getSearchBox().getText(), projectType, displayingCurrentVersion);
                if (t != null) {
                    t.interrupt();
                }
                t = new Thread(() -> {
                    TestingClient.updateInstalled(this.projectType);
                });
                t.start();
                loadIcons();
            });
            thread.start();

        }).build();
         */
        //displayingCurrentVersion = true;
        //versionButton.setMessage(Text.of("Shows for: 1.21.3"));
        searchBox = new TextFieldWidget(textRenderer, width/10 + 15, 0, width/3, 20, Text.literal("Search"));
        doneButton.setPosition(width/10 + width/3 + 20, 0);
        doneButton.setWidth(140);
        //filtersButton.setDimensions(20, 20);
        //filtersButton.setPosition(width/10 + width/3 + 165, 0);
        //versionButton.setPosition(width/6 + 75, 20);
        this.addSelectableChild(searchBox);
        this.addSelectableChild(doneButton);
        //this.addSelectableChild(filtersButton);
        for (int i = 0; i < TestingClient.ROWS_ON_PAGE; i++) {
            int finalI = i;
            ButtonWidget buttonWidget = ButtonWidget.builder(Text.of("Install"), button -> {
                Thread thread2 = new Thread(() -> {
                    TestingClient.downloadVersion(INFO[finalI].getSlug(), projectType);
                    t = new Thread(() -> {
                        TestingClient.updateInstalled(projectType);
                    });
                });
                thread2.start();
            }).build();
            buttonWidget.setWidth(52);
            buttonWidget.setHeight(12);
            installButtons[i] = buttonWidget;
            installButtons[i].setX(width-60);
            int finalI1 = i;
            ButtonWidget projectButtonWidget = ButtonWidget.builder(Text.of("More Info"), button -> {
                client.setScreen(new ProjectScreen(this, INFO[finalI1]));
            }).build();

            projectButtonWidget.setWidth(60);
            projectButtonWidget.setHeight(12);
            projectScreenButtons[i] = projectButtonWidget;
            projectScreenButtons[i].setX(width-140);
            this.addSelectableChild(buttonWidget);
            this.addSelectableChild(projectButtonWidget);

        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        for (int i = 0; i < TestingClient.ROWS_ON_PAGE; i++) {
            try {
                if (INFO[i] != null) {
                    context.drawTexture(RenderLayer::getGuiTextured, ICON_TEXTURE_ID[i], 0, height / 8 + scrollAmount + i * 50, 0, 0, 40, 40, 40, 40);
                    context.drawText(textRenderer, INFO[i].getTitle(), width / 10 + 15, height / 8 + scrollAmount + i * 50, 0xFFFFFF, false);
                    context.drawText(textRenderer, "by " + INFO[i].getAuthor(), width / 10 + INFO[i].getTitle().length() * 6 + 20, height / 8 + scrollAmount + i * 50, 0xFFFFFF, false);
                    context.drawTextWrapped(textRenderer, StringVisitable.plain(INFO[i].getDescription()), width / 10 + 15, height / 8 + scrollAmount + i * 50 + 15, width * 7 / 8, 0xFFFFFF);
                    installButtons[i].render(context, mouseX, mouseY, delta);
                    installButtons[i].setY(height / 8 + scrollAmount + i * 50);
                    installButtons[i].active = !INFO[i].isInstalled();
                    if (!installButtons[i].active) {
                        installButtons[i].setMessage(Text.of("Installed"));
                    } else {
                        installButtons[i].setMessage(Text.of("Install"));
                    }
                    projectScreenButtons[i].render(context, mouseX, mouseY, delta);
                    projectScreenButtons[i].setY(height / 8 + scrollAmount + i * 50);
                }
            } catch(NullPointerException ignored) {
                System.out.println("Bad");
            }
        }
        searchBox.render(context, mouseX, mouseY, delta);
        doneButton.render(context, mouseX, mouseY, delta);
        //if (showingFilterOptions) {
            //versionButton.render(context, mouseX, mouseY, delta);
        //}
        //filtersButton.render(context, mouseX, mouseY, delta);
        //context.drawTexture(RenderLayer::getGuiTextured, FILTER_TEXTURE, filtersButton.getX() + 2, filtersButton.getY() + 2, 0, 0, 16, 16, 16, 16);
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollAmount + verticalAmount * 12 < 0 && scrollAmount + verticalAmount * 12 > - 48 * TestingClient.ROWS_ON_PAGE) {
            scrollAmount += (int) (verticalAmount*12);
        }
        return true;
    }

    public void loadIcons() {
        int numberOfThreads = 12;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        for (int i = 0; i < TestingClient.ROWS_ON_PAGE; i++) {
            //This is texture when it is loading...
            if (INFO[i]==null) {
                break;
            }
            int finalI = i;
            if (!Thread.currentThread().isInterrupted()) {
                client.execute(()-> {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(new NativeImage(1, 1, false));
                    texture.getImage().setColorArgb(0, 0, 0x00000000);
                    texture.upload();
                    client.getTextureManager().registerTexture(ICON_TEXTURE_ID[finalI], texture);
                });
                executorService.submit(() -> IconManager.loadIcon(INFO[finalI], ICON_TEXTURE_ID[finalI], client));

            }
        }
        executorService.shutdown();
    }

    public TextFieldWidget getSearchBox() {
        return searchBox;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (getSearchBox().isSelected()) {
            if (searchThread != null) {
                searchThread.interrupt();
            }
            if (t != null) {
                t.interrupt();
            }
            searchThread = new Thread(() -> {
                TestingClient.search(getSearchBox().getText(), projectType);
                if (!searchThread.isInterrupted()) {
                    if (t != null) {
                        t.interrupt();
                    }
                    t = new Thread(() -> {
                        TestingClient.updateInstalled(this.projectType);
                    });
                    t.start();
                    loadIcons();
                }
            });
            searchThread.start();

        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
