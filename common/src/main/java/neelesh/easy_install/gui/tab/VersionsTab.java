package neelesh.easy_install.gui.tab;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.Version;
import neelesh.easy_install.gui.screen.ProjectScreen;
import neelesh.easy_install.gui.screen.VersionDetailsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static neelesh.easy_install.gui.screen.ProjectScreen.VERTICAL_SEPARATOR_TEXTURE;

public class VersionsTab extends GridLayoutTab implements Renderable {
    private Version[] versions;
    private Button[] versionButtons;
    private boolean initialized;
    private ProjectScreen projectScreen;
    private PlainTextButton[] versionDetailButtons;

    private Version projectInstallVersion;

    public VersionsTab(Component title, ProjectScreen projectScreen) {
        super(title);
        this.projectScreen = projectScreen;
        Thread thread = new Thread(() -> {
            String response = EasyInstallClient.getVersions(projectScreen.getProjectInfo().getSlug(), projectScreen.getProjectInfo().getProjectType(), projectScreen.isFilteredByGameVersion());
            if (!projectScreen.isFilteredByGameVersion()) {
                Thread thread2 = new Thread(() -> {
                    String response2 = EasyInstallClient.getVersions(projectScreen.getProjectInfo().getSlug(), projectScreen.getProjectInfo().getProjectType(), true);
                    if (response2 == null || JsonParser.parseString(response2).getAsJsonArray().isEmpty()) {
                        return;
                    }
                    JsonObject jsonObject = JsonParser.parseString(response2).getAsJsonArray().get(0).getAsJsonObject();
                    try {
                        this.projectInstallVersion = EasyInstallClient.createVersion(jsonObject, projectScreen.getProjectInfo().getProjectType());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                });
                thread2.start();
            }
            JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
            versions = new Version[jsonArray.size()];
            versionButtons = new Button[jsonArray.size()];
            versionDetailButtons = new PlainTextButton[jsonArray.size()];
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
                Minecraft.getInstance().submit(() -> {
                    versionButtons[finalI] = Button.builder(Component.nullToEmpty("Install"), buttonWidget -> {
                        Thread t = new Thread(() -> {
                            @Nullable Version updatedVersion = projectScreen.getUpdatedVersion();
                            boolean case1 = this.projectInstallVersion != null && this.projectInstallVersion.getHash().equals(versions[finalI].getHash()) && projectScreen.getProjectInfo().isUpdated();
                            boolean case2 = updatedVersion != null && versions[finalI].getHash().equals(updatedVersion.getHash()) && !projectScreen.getProjectInfo().isUpdated();


                            Minecraft.getInstance().schedule(() -> {
                                versionButtons[finalI].active = false;
                                versionButtons[finalI].setMessage(Component.nullToEmpty("Installed"));
                                if (case1 || case2) {
                                    projectScreen.getProjectInfo().setInstalling(true);
                                }
                                versionButtons[finalI].setMessage(Component.nullToEmpty("Installing"));
                            });
                            versions[finalI].download(true);
                            Minecraft.getInstance().schedule(() -> {
                                if (case1 || case2) {
                                    projectScreen.getProjectInfo().setInstalling(false);
                                    projectScreen.getProjectInfo().setInstalled(true);
                                }
                                versionButtons[finalI].active = false;
                                versionButtons[finalI].setMessage(Component.nullToEmpty("Installed"));
                                initialized = false;
                            });
                            EasyInstallClient.checkStatus(projectScreen.getProjectInfo().getProjectType());
                        });
                        t.start();
                    }).build();
                    versionButtons[finalI].setSize(55, 14);
                    projectScreen.addSelectableChild(versionButtons[finalI]);

                    if (versions[finalI] != null) {
                        versionDetailButtons[finalI] = new PlainTextButton(140, finalI * 40 + projectScreen.getScrollAmount(), projectScreen.getFont().width(versions[finalI].getName()), 9, Component.nullToEmpty(versions[finalI].getName()), button -> {
                            Minecraft.getInstance().gui.setScreen(new VersionDetailsScreen(versions[finalI], projectScreen));
                        }, projectScreen.getFont());
                    }
                    projectScreen.addSelectableChild(versionDetailButtons[finalI]);
                });
            }
        });
        thread.start();

    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (versions == null) {
            return;
        }
        projectScreen.renderMenuBackground(context, 131, projectScreen.getScrollAmount() + ((TabButton) projectScreen.getTabNavigationWidget().children().get(0)).getHeight()-10, projectScreen.width, versions.length * 40 + 10);

        for (int i = 0; i < versions.length; i++) {
            if (versions[i] == null || versionDetailButtons[i] == null) {
                break;
            }
            if (projectScreen.getProjectInfo().isInstalling() && versionButtons.length != 0) {
                @Nullable Version updatedVersion = projectScreen.getUpdatedVersion();
                boolean case1 = this.projectInstallVersion != null && this.projectInstallVersion.getHash().equals(versions[i].getHash()) && projectScreen.getProjectInfo().isUpdated();
                boolean case2 = updatedVersion != null && versions[i].getHash().equals(updatedVersion.getHash()) && !projectScreen.getProjectInfo().isUpdated();
                if (case1 || case2) {
                    versionButtons[i].active = false;
                    versionButtons[i].setMessage(Component.nullToEmpty("Installing"));
                }

            }

            versionDetailButtons[i].setPosition(140, i * 40 + projectScreen.getScrollAmount() + 20);
            versionDetailButtons[i].extractRenderState(context, mouseX, mouseY, delta);
            //context.drawText(projectScreen.getTextRenderer(), Text.of(versions[i].getName()), 140, i * 40 + projectScreen.getScrollAmount() + 20, Colors.WHITE, true);
            ChatFormatting formatting;
            formatting = switch(versions[i].getVersionType()) {
                case "release" -> ChatFormatting.GREEN;
                case "beta" -> ChatFormatting.GOLD;
                case "alpha" -> ChatFormatting.RED;
                default -> null;
            };
            context.text(projectScreen.getFont(), Component.literal("•" + versions[i].getVersionType()).withStyle(formatting), 140, i * 40 + projectScreen.getScrollAmount() + 30, CommonColors.WHITE, true);
            context.text(projectScreen.getFont(), Component.nullToEmpty(versions[i].getVersionNumber()), 140 + projectScreen.getFont().width("•" + versions[i].getVersionType()) + 8, i * 40 + projectScreen.getScrollAmount() + 30, CommonColors.WHITE, true);
            context.text(projectScreen.getFont(), Component.nullToEmpty(String.format("%,d", versions[i].getNumDownloads()) + " downloads"), projectScreen.width - projectScreen.getFont().width(String.format("%,d", versions[i].getNumDownloads()) + " downloads") - 8, i * 40 + projectScreen.getScrollAmount() + 36, CommonColors.WHITE, true);


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
                        Minecraft.getInstance().schedule(() -> {
                            if (versions[finalI].getHash().equals(hash)) {
                                versionButtons[finalI].active = false;
                                versionButtons[finalI].setMessage(Component.nullToEmpty("Installed"));
                            } else {
                                versionButtons[finalI].active = true;
                                versionButtons[finalI].setMessage(Component.nullToEmpty("Install"));
                            }
                        });
                    }
                });

            } else if (!initialized) {
                versionButtons[i].active = true;
                versionDetailButtons[i].active = true;
                versionButtons[i].setMessage(Component.nullToEmpty("Install"));

            }
            versionButtons[i].setPosition(projectScreen.width - versionButtons[i].getWidth() - 10, i * 40 + 20 + projectScreen.getScrollAmount());
            versionButtons[i].extractRenderState(context, mouseX, mouseY, delta);
        }
        initialized = true;
        context.blit(
                RenderPipelines.GUI_TEXTURED, VERTICAL_SEPARATOR_TEXTURE, 131, projectScreen.getScrollAmount() + ((TabButton) projectScreen.getTabNavigationWidget().children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, versions.length * 40 + 10, 2, 32
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
        for (Button versionButton : versionButtons) {
            if (versionButton == null) {
                continue;
            }
            versionButton.active = !versionButton.getMessage().getString().equals("Installed") && !versionButton.getMessage().getString().equals("Installing") && active;
        }

        for (Button versionDetailsButton : versionDetailButtons) {
            if (versionDetailsButton == null) {
                continue;
            }
            versionDetailsButton.active = active;
        }
    }
}