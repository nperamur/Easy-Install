package neelesh.easy_install.gui.tab;

import neelesh.easy_install.MarkdownRenderer;
import neelesh.easy_install.gui.screen.ProjectScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;

import static neelesh.easy_install.gui.screen.ProjectScreen.VERTICAL_SEPARATOR_TEXTURE;

public class DescriptionTab extends GridLayoutTab implements Renderable {
    private ProjectScreen projectScreen;
    private MarkdownRenderer markdownRenderer;

    public DescriptionTab(Component title, ProjectScreen projectScreen) {
        super(title);
        this.projectScreen = projectScreen;
        this.markdownRenderer = new MarkdownRenderer(projectScreen.getProjectInfo().getBody(), 140, 30, projectScreen.width, projectScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {

        projectScreen.renderMenuBackground(context, 131, projectScreen.getScrollAmount() + ((TabButton) projectScreen.getTabNavigationWidget().children().get(0)).getHeight()-10, projectScreen.width, markdownRenderer.getMaxY());
        projectScreen.setMaxY(markdownRenderer.getMaxY());

        markdownRenderer.setEndX(projectScreen.width);
        markdownRenderer.render(context, projectScreen.getScrollAmount());
        context.blit(
                RenderPipelines.GUI_TEXTURED, VERTICAL_SEPARATOR_TEXTURE, 131, projectScreen.getScrollAmount() + ((TabButton) projectScreen.getTabNavigationWidget().children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, markdownRenderer.getMaxY(), 2, 32
        );

        }

    public void setLinksActive(boolean active) {
        markdownRenderer.setLinksActive(active);
    }

    public void refreshLinkPositions() {
        markdownRenderer.refreshLinkPositions();

    }

}
