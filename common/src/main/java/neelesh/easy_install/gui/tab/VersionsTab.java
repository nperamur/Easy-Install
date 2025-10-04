package neelesh.easy_install.gui.tab;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.Version;
import neelesh.easy_install.gui.screen.ProjectScreen;
import neelesh.easy_install.gui.screen.VersionDetailsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static neelesh.easy_install.gui.screen.ProjectScreen.VERTICAL_SEPARATOR_TEXTURE;

public class VersionsTab extends GridScreenTab implements Drawable {
    private Version[] versions;
    private ButtonWidget[] versionButtons;
    private boolean initialized;
    private ProjectScreen projectScreen;
    private PressableTextWidget[] versionDetailButtons;

    public VersionsTab(Text title, ProjectScreen projectScreen) {
        super(title);
        this.projectScreen = projectScreen;
        Thread thread = new Thread(() -> {
            String response = EasyInstallClient.getVersions(projectScreen.getProjectInfo().getSlug(), projectScreen.getProjectInfo().getProjectType(), projectScreen.isFilteredByGameVersion());
            JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
            versions = new Version[jsonArray.size()];
            versionButtons = new ButtonWidget[jsonArray.size()];
            versionDetailButtons = new PressableTextWidget[jsonArray.size()];
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
                MinecraftClient.getInstance().submit(() -> {
                    versionButtons[finalI] = ButtonWidget.builder(Text.of("Install"), buttonWidget -> {
                        Thread t = new Thread(() -> {
                            MinecraftClient.getInstance().send(() -> {
                                versionButtons[finalI].active = false;
                                versionButtons[finalI].setMessage(Text.of("Installed"));
                                if (finalI == 0) {
                                    projectScreen.getProjectInfo().setInstalling(true);
                                }
                                versionButtons[finalI].setMessage(Text.of("Installing"));
                            });
                            versions[finalI].download();
                            MinecraftClient.getInstance().send(() -> {
                                if (finalI == 0) {
                                    projectScreen.getProjectInfo().setInstalling(false);
                                    projectScreen.getProjectInfo().setInstalled(true);
                                }
                                versionButtons[finalI].active = false;
                                versionButtons[finalI].setMessage(Text.of("Installed"));
                                initialized = false;
                            });
                            EasyInstallClient.checkStatus(projectScreen.getProjectInfo().getProjectType());
                        });
                        t.start();
                    }).build();
                    versionButtons[finalI].setDimensions(55, 14);
                    projectScreen.addSelectableChild(versionButtons[finalI]);

                    versionDetailButtons[finalI] = new PressableTextWidget(140, finalI * 40 + projectScreen.getScrollAmount(), projectScreen.getTextRenderer().getWidth(versions[finalI].getName()), 9, Text.of(versions[finalI].getName()), button -> {
                        MinecraftClient.getInstance().setScreen(new VersionDetailsScreen(versions[finalI], projectScreen));
                    }, projectScreen.getTextRenderer());
                    projectScreen.addSelectableChild(versionDetailButtons[finalI]);
                });
            }
        });
        thread.start();

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (versions == null) {
            return;
        }
        projectScreen.renderDarkening(context, 131, projectScreen.getScrollAmount() + ((TabButtonWidget) projectScreen.getTabNavigationWidget().children().get(0)).getHeight()-10, projectScreen.width, versions.length * 40 + 10);

        if (projectScreen.getProjectInfo().isInstalling() && versionButtons.length != 0) {
            versionButtons[0].active = false;
            versionButtons[0].setMessage(Text.of("Installing"));
        }
        for (int i = 0; i < versions.length; i++) {
            if (versions[i] == null || versionDetailButtons[i] == null) {
                break;
            }
            versionDetailButtons[i].setPosition(140, i * 40 + projectScreen.getScrollAmount() + 20);
            versionDetailButtons[i].render(context, mouseX, mouseY, delta);
            //context.drawText(projectScreen.getTextRenderer(), Text.of(versions[i].getName()), 140, i * 40 + projectScreen.getScrollAmount() + 20, Colors.WHITE, true);
            Formatting formatting;
            formatting = switch(versions[i].getVersionType()) {
                case "release" -> Formatting.GREEN;
                case "beta" -> Formatting.GOLD;
                case "alpha" -> Formatting.RED;
                default -> null;
            };
            context.drawText(projectScreen.getTextRenderer(), Text.literal("•" + versions[i].getVersionType()).formatted(formatting), 140, i * 40 + projectScreen.getScrollAmount() + 30, Colors.WHITE, true);
            context.drawText(projectScreen.getTextRenderer(), Text.of(versions[i].getVersionNumber()), 140 + projectScreen.getTextRenderer().getWidth("•" + versions[i].getVersionType()) + 8, i * 40 + projectScreen.getScrollAmount() + 30, Colors.WHITE, true);
            context.drawText(projectScreen.getTextRenderer(), Text.of(String.format("%,d", versions[i].getNumDownloads()) + " downloads"), projectScreen.width - projectScreen.getTextRenderer().getWidth(String.format("%,d", versions[i].getNumDownloads()) + " downloads") - 8, i * 40 + projectScreen.getScrollAmount() + 36, Colors.WHITE, true);


            File file = new File(EasyInstallClient.getSavePath(projectScreen.getProjectInfo().getProjectType(), versions[i].getFilename()).toString());

            if (file.exists() && projectScreen.getTabManager().getCurrentTab() == this && !initialized) {
                int finalI = i;
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return EasyInstallClient.createFileHash(file.toPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).thenAcceptAsync(hash -> {
                    if (hash != null) {
                        MinecraftClient.getInstance().send(() -> {
                            if (versions[finalI].getHash().equals(hash)) {
                                versionButtons[finalI].active = false;
                                versionButtons[finalI].setMessage(Text.of("Installed"));
                            } else {
                                versionButtons[finalI].active = true;
                                versionButtons[finalI].setMessage(Text.of("Install"));
                            }
                        });
                    }
                });

            } else if (!initialized) {
                versionButtons[i].active = true;
                versionDetailButtons[i].active = true;
                versionButtons[i].setMessage(Text.of("Install"));

            }
            versionButtons[i].setPosition(projectScreen.width - versionButtons[i].getWidth() - 10, i * 40 + 20 + projectScreen.getScrollAmount());
            versionButtons[i].render(context, mouseX, mouseY, delta);
        }
        initialized = true;
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED, VERTICAL_SEPARATOR_TEXTURE, 131, projectScreen.getScrollAmount() + ((TabButtonWidget) projectScreen.getTabNavigationWidget().children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, versions.length * 40 + 10, 2, 32
        );
        projectScreen.setMaxY(versions.length * 40 + 10);
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }


    public void setActive(boolean active) {
        if (versionButtons == null) {
            return;
        }
        for (ButtonWidget versionButton : versionButtons) {
            if (versionButton == null) {
                continue;
            }
            versionButton.active = !versionButton.getMessage().getString().equals("Installed") && !versionButton.getMessage().getString().equals("Installing") && active;
        }

        for (ButtonWidget versionDetailsButton : versionDetailButtons) {
            if (versionDetailsButton == null) {
                continue;
            }
            versionDetailsButton.active = active;
        }
    }
}