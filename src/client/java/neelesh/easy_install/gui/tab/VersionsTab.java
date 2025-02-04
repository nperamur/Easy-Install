package neelesh.easy_install.gui.tab;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.gui.screen.ProjectScreen;
import neelesh.easy_install.Version;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.File;
import java.io.IOException;

import static neelesh.easy_install.gui.screen.ProjectScreen.VERTICAL_SEPARATOR_TEXTURE;

public class VersionsTab extends GridScreenTab implements Drawable {
    private Version[] versions;
    private ButtonWidget[] versionButtons;
    private boolean initialized;
    private ProjectScreen projectScreen;

    public VersionsTab(Text title, ProjectScreen projectScreen) {
        super(title);
        this.projectScreen = projectScreen;
        Thread thread = new Thread(() -> {
            String response = EasyInstallClient.getVersions(projectScreen.getProjectInfo().getSlug(), projectScreen.getProjectInfo().getProjectType());
            JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
            versions = new Version[jsonArray.size()];
            versionButtons = new ButtonWidget[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject versionInfo = jsonArray.get(i).getAsJsonObject();
                Version version;
                try {
                    version = EasyInstallClient.createVersion(versionInfo, projectScreen.getProjectInfo().getProjectType());
                    versions[i] = version;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int finalI = i;
                versionButtons[i] = ButtonWidget.builder(Text.of("Install"), buttonWidget -> {
                    Thread t = new Thread(() -> {
                        versions[finalI].download();
                        initialized = false;
                        EasyInstallClient.checkInstalled(projectScreen.getProjectInfo().getProjectType());
                    });
                    t.start();
                }).build();
                versionButtons[i].setDimensions(55, 14);
                projectScreen.addSelectableChild(versionButtons[i]);
            }
        });
        thread.start();

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (versions == null) {
            return;
        }
        for (int i = 0; i < versions.length; i++) {
            if (versions[i] == null) {
                break;
            }
            context.drawText(projectScreen.getTextRenderer(), Text.of(versions[i].getName()), 140, i * 40 + projectScreen.getScrollAmount() + 20, 0xFFFFFF, true);
            Formatting formatting;
            formatting = switch(versions[i].getVersionType()) {
                case "release" -> Formatting.GREEN;
                case "beta" -> Formatting.GOLD;
                case "alpha" -> Formatting.RED;
                default -> null;
            };
            context.drawText(projectScreen.getTextRenderer(), Text.literal("•" + versions[i].getVersionType()).formatted(formatting), 140, i * 40 + projectScreen.getScrollAmount() + 30, 0xFFFFFF, true);
            context.drawText(projectScreen.getTextRenderer(), Text.of(versions[i].getVersionNumber()), 140 + projectScreen.getTextRenderer().getWidth("•" + versions[i].getVersionType()) + 8, i * 40 + projectScreen.getScrollAmount() + 30, 0xFFFFFF, true);
//            context.drawText(projectScreen.getTextRenderer(), Text.of(String.format("%,d", versions[i].getNumDownloads()) + " downloads"), projectScreen.width, - projectScreen.getTextRenderer().getWidth(String.format("%,d", versions[i].getNumDownloads()) + " downloads") - 8, i * 40 + projectScreen.getScrollAmount() + 36, 0xFFFFFF, true);
            context.drawText(projectScreen.getTextRenderer(), Text.of(String.format("%,d", versions[i].getNumDownloads()) + " downloads"), projectScreen.width - projectScreen.getTextRenderer().getWidth(String.format("%,d", versions[i].getNumDownloads()) + " downloads") - 8, i * 40 + projectScreen.getScrollAmount() + 36, 0xFFFFFF, true);


            File file = new File(EasyInstallClient.getSavePath(projectScreen.getProjectInfo().getProjectType(), versions[i].getFilename()).toString());


            if (file.exists() && projectScreen.getTabManager().getCurrentTab() == this && !initialized) {
                String hash;
                try {
                    hash = EasyInstallClient.createFileHash(file.toPath());
                    System.out.println(hash);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (versions[i].getHash().equals(hash)) {
                    versionButtons[i].active = false;
                    versionButtons[i].setMessage(Text.of("Installed"));
                } else {
                    versionButtons[i].active = true;
                    versionButtons[i].setMessage(Text.of("Install"));
                }
            } else if (!initialized) {
                versionButtons[i].active = true;
                versionButtons[i].setMessage(Text.of("Install"));
            }
            versionButtons[i].setPosition(projectScreen.width - versionButtons[i].getWidth() - 10, i * 40 + 20 + projectScreen.getScrollAmount());
            versionButtons[i].render(context, mouseX, mouseY, delta);
        }
        initialized = true;
        projectScreen.renderDarkening(context, 131, projectScreen.getScrollAmount() + ((TabButtonWidget) projectScreen.getTabNavigationWidget().children().get(0)).getHeight()-10, projectScreen.width, versions.length * 40 + 10);
        context.drawTexture(
                RenderLayer::getGuiTextured, VERTICAL_SEPARATOR_TEXTURE, 131, projectScreen.getScrollAmount() + ((TabButtonWidget) projectScreen.getTabNavigationWidget().children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, versions.length * 40 + 10, 2, 32
        );
        projectScreen.setMaxY(versions.length * 40 + 10);
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}