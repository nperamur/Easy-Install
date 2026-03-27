package neelesh.easy_install.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class PressableTextWidgetShadowless extends Button {
    private final Font textRenderer;
    private final MutableComponent text;
    private final MutableComponent hoverText;

    public PressableTextWidgetShadowless(int x, int y, int width, int height, MutableComponent text, OnPress onPress, Font textRenderer) {
        super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
        this.textRenderer = textRenderer;
        this.text = text;
        this.hoverText = ComponentUtils.mergeStyles(text.copy(), Style.EMPTY.withUnderlined(true));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        MutableComponent text = this.isHoveredOrFocused() ? this.hoverText : this.text;
        context.text(this.textRenderer, text, this.getX(), this.getY(), 16777215 | Mth.ceil(this.alpha * 255.0F) << 24, false);

    }



}

