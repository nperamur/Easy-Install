package neelesh.easy_install.gui.screen;

import neelesh.easy_install.EasyInstallJsonHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 33);
    private CycleButton<ConfigButtonPlacement> titleScreenButtonPlacement;
    private Button doneButton;

    public ConfigScreen() {
        super(Component.nullToEmpty("Config Screen"));
    }

    public ConfigScreen(Screen screen) {
        super(Component.nullToEmpty("Easy Install Config"));
        doneButton = Button.builder(Component.nullToEmpty("Done"), button -> {
            minecraft.gui.setScreen(screen);
        }).build();

    }

    @Override
    protected void init() {
        super.init();
        layout.addTitleHeader(this.title, this.font);
        titleScreenButtonPlacement = CycleButton.<ConfigButtonPlacement>builder(configButtonPlacement -> Component.nullToEmpty(configButtonPlacement.getType()),
                EasyInstallJsonHandler.getButtonPlacementConfig()).withValues(ConfigButtonPlacement.IN_BUTTON_ROW, ConfigButtonPlacement.FOOTER).create(Component.nullToEmpty("Title Screen Button"),
                (button, value) -> {
            EasyInstallJsonHandler.setButtonPlacementConfig(value.getType());
        });
        titleScreenButtonPlacement.setWidth(Button.BIG_WIDTH);
        titleScreenButtonPlacement.setPosition(width / 2 - titleScreenButtonPlacement.getWidth() / 2, 50);
        this.addRenderableWidget(titleScreenButtonPlacement);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.vertical().spacing(4));
        doneButton.setWidth(Button.BIG_WIDTH);
        footer.addChild(doneButton);
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.enableScissor(0, layout.getY() + layout.getHeaderHeight(), width, layout.getY() + layout.getHeaderHeight() + layout.getContentHeight());
        extractMenuBackground(graphics);
        graphics.disableScissor();
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR, this.layout.getX(), this.layout.getHeaderHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, this.layout.getX(), this.height - this.layout.getFooterHeight(), 0.0F, 0.0F, this.width, 2, 32, 2);
    }


}
