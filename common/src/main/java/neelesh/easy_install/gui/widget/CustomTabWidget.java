package neelesh.easy_install.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import neelesh.easy_install.EasyInstall;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.util.Identifier;

public class CustomTabWidget extends TabButtonWidget {
    private static final Identifier TEXTURE = new Identifier(EasyInstall.MOD_ID, "textures/gui/custom_tab_widget.png");

    public CustomTabWidget(TabManager tabManager, Tab tab, int width, int height) {
        super(tabManager, tab, width, height);
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.enableBlend();
        context.drawNineSlicedTexture(TEXTURE, this.getX(), this.getY(), this.width, this.height, 2, 2, 2, 0, 130, 24, 0, this.getTextureV());
        RenderSystem.disableBlend();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int i = this.active ? -1 : -6250336;
        this.drawMessage(context, textRenderer, i);
        if (this.isCurrentTab()) {
            this.drawCurrentTabLine(context, textRenderer, i);
        }

    }

    private void drawCurrentTabLine(DrawContext context, TextRenderer textRenderer, int color) {
        int i = Math.min(textRenderer.getWidth(this.getMessage()), this.getWidth() - 4);
        int j = this.getX() + (this.getWidth() - i) / 2;
        int k = this.getY() + this.getHeight() - 2;
        context.fill(j, k, j + i, k + 1, color);
    }
}
