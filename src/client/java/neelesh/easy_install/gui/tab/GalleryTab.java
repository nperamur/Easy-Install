package neelesh.easy_install.gui.tab;

import neelesh.easy_install.GalleryImage;
import neelesh.easy_install.ImageLoader;
import neelesh.easy_install.gui.screen.ProjectScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static neelesh.easy_install.gui.screen.ProjectScreen.VERTICAL_SEPARATOR_TEXTURE;

public class GalleryTab extends GridScreenTab implements Drawable {
    private ArrayList<GalleryImage> galleryImages;
    private ProjectScreen projectScreen;
    public GalleryTab(Text title, ProjectScreen projectScreen) {
        super(title);
        this.galleryImages = projectScreen.getGalleryImages();
        int numberOfThreads = 5;
        Thread thread = new Thread(() -> {
            try (ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads)) {
                    for (int i = 0; i < galleryImages.size(); i++) {
                        int finalI = i;
                        executorService.submit(() -> galleryImages.get(finalI).setImage(ImageLoader.loadImage(galleryImages.get(finalI).getUrl(), galleryImages.get(finalI).getId(), MinecraftClient.getInstance())));
                    }
                    executorService.shutdown();
                }
            });
        thread.start();

        this.projectScreen = projectScreen;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().translate(0, 0, 100);
        int y = 30;
        int prevHeight = 0;
        for (int i = 0; i < galleryImages.size(); i++) {
            if (galleryImages.get(i).getImage() == null) {
                continue;
            }
            double imageSize = (double) (projectScreen.width-130)/2.3;
            double v = Math.pow(-1, i + 1) * (imageSize / 2 + 4);
            float titleSize = 1.2f;

            context.getMatrices().scale(titleSize, titleSize, 1f);
            context.drawTextWrapped(projectScreen.getTextRenderer(), StringVisitable.plain(galleryImages.get(i).getTitle()), (int) ((135 + ((double) (projectScreen.width - 130)/2 + v) - imageSize/2)/titleSize), (int) ((int) (y + 5 + projectScreen.getScrollAmount() + imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight())/titleSize),(int) (imageSize/titleSize), 0xFFFFFF);
            float titleHeight = (projectScreen.getTextRenderer().getWrappedLinesHeight(StringVisitable.plain(galleryImages.get(i).getTitle()), (int) (imageSize/titleSize)) * titleSize);
            context.getMatrices().scale(1.0f/titleSize, 1.0f/titleSize, 1f);
            context.drawTextWrapped(projectScreen.getTextRenderer(), StringVisitable.plain(galleryImages.get(i).getDescription()), 135 + ((int)((double) (projectScreen.width - 130)/2 + v)  - (int) imageSize/2), (int) (y + titleHeight + 8 + projectScreen.getScrollAmount() + (imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight())),(int) imageSize, 0xFFFFFF);
            context.drawTexture(RenderLayer::getGuiTextured, galleryImages.get(i).getId(), 135 + ((int)((double) (projectScreen.width - 130)/2 + v) - (int) imageSize/2), y + projectScreen.getScrollAmount(), 0, 0, (int) imageSize, (int)(imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight()), (int) imageSize, (int)(imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight()));
            float descriptionHeight = (projectScreen.getTextRenderer().getWrappedLinesHeight(StringVisitable.plain(galleryImages.get(i).getDescription()), (int) imageSize));
            if (!galleryImages.get(i).getTitle().trim().isEmpty()) {
                titleHeight += 8;
            }
            if (!galleryImages.get(i).getDescription().trim().isEmpty()) {
                descriptionHeight += 5;
            }
            if (i % 2 == 1 || i == galleryImages.size() - 1) {
                y += Math.max(prevHeight, (int) (descriptionHeight + titleHeight + 8 + (imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight())));
                prevHeight = 0;
            } else {
                prevHeight = (int) (titleHeight + 8 + descriptionHeight + (imageSize/galleryImages.get(i).getImage().getWidth() * galleryImages.get(i).getImage().getHeight()));
            }

        }
        context.getMatrices().translate(0, 0, -100);
        projectScreen.renderDarkening(context, 131, projectScreen.getScrollAmount() + ((TabButtonWidget) projectScreen.getTabNavigationWidget().children().get(0)).getHeight()-10, projectScreen.width, y);
        context.drawTexture(
                RenderLayer::getGuiTextured, VERTICAL_SEPARATOR_TEXTURE, 131, projectScreen.getScrollAmount() + ((TabButtonWidget) projectScreen.getTabNavigationWidget().children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, y, 2, 32
        );
        projectScreen.setMaxY(y);
    }
}