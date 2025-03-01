package neelesh.easy_install.gui.tab;

import neelesh.easy_install.MarkdownRenderer;
import neelesh.easy_install.gui.screen.ProjectScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;

import java.util.ArrayList;

import static neelesh.easy_install.gui.screen.ProjectScreen.VERTICAL_SEPARATOR_TEXTURE;

public class DescriptionTab extends GridScreenTab implements Drawable {
    private ProjectScreen projectScreen;
    private MarkdownRenderer markdownRenderer;

    public DescriptionTab(Text title, ProjectScreen projectScreen) {
        super(title);
        this.projectScreen = projectScreen;
        this.markdownRenderer = new MarkdownRenderer(projectScreen.getProjectInfo().getBody(), 140, 30, projectScreen.width, projectScreen);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().translate(0, 0, 100);
        markdownRenderer.setEndX(projectScreen.width);
        markdownRenderer.render(context, projectScreen.getScrollAmount());
        context.getMatrices().translate(0, 0, -100);
        projectScreen.renderDarkening(context, 131, projectScreen.getScrollAmount() + ((TabButtonWidget) projectScreen.getTabNavigationWidget().children().get(0)).getHeight()-10, projectScreen.width, markdownRenderer.getMaxY());
        context.drawTexture(
                RenderLayer::getGuiTextured, VERTICAL_SEPARATOR_TEXTURE, 131, projectScreen.getScrollAmount() + ((TabButtonWidget) projectScreen.getTabNavigationWidget().children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, markdownRenderer.getMaxY(), 2, 32
        );
        projectScreen.setMaxY(markdownRenderer.getMaxY());
    }

    public void setLinksActive(boolean active) {
        markdownRenderer.setLinksActive(active);
    }

    public void refreshLinkPositions() {
        markdownRenderer.refreshLinkPositions();

    }

}
