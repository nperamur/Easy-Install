package neelesh.easy_install.gui.tab;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neelesh.easy_install.DownloadProgress;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.ProjectStatus;
import neelesh.easy_install.Version;
import neelesh.easy_install.gui.screen.ProjectScreen;
import neelesh.easy_install.gui.screen.VersionDetailsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.SystemToast;
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

import java.awt.*;
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
    private int prevProgressPadding = 0;
    private PlainTextButton[] versionDetailButtons;

    private Version projectInstallVersion;

    public VersionsTab(Component title, ProjectScreen projectScreen) {
        super(title);
        this.projectScreen = projectScreen;
        Thread thread = new Thread(() -> {
            String response = EasyInstallClient.getVersions(projectScreen.getProjectInfo().getSlug(), projectScreen.getProjectInfo().getProjectType(), projectScreen.isFilteredByGameVersion());
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
                            Minecraft.getInstance().schedule(() -> {
                                versionButtons[finalI].setMessage(Component.nullToEmpty("Installing"));
                            });
                            boolean needsUpdate = projectScreen.getInstallationState().needsUpdate(projectScreen.getProjectId());
                            boolean case1 = this.projectInstallVersion != null && this.projectInstallVersion.getHash().equals(versions[finalI].getHash()) && !needsUpdate;
                            boolean case2 = EasyInstallClient.containsUpdatedHash(versions[finalI].getHash());

                            Minecraft.getInstance().schedule(() -> {
                                versionButtons[finalI].active = false;
                                versionButtons[finalI].setMessage(Component.nullToEmpty("Installed"));
                                if (case1) {
                                    projectScreen.getInstallationState()
                                            .setProjectStatus(projectScreen.getProjectId(), ProjectStatus.INSTALLING_REGULAR);
                                }

                                if (case2) {
                                    projectScreen.getInstallationState()
                                            .setProjectStatus(projectScreen.getProjectId(), ProjectStatus.INSTALLING_UPDATE);
                                }
                            });
                            EasyInstallClient.setProjectIdHashMapping(versions[finalI].getProjectId(), versions[finalI].getHash());
                            boolean successful = versions[finalI].download(true);
                            EasyInstallClient.removeProjectIdHashMapping(versions[finalI].getProjectId());
                            Minecraft.getInstance().schedule(() -> {
                                if (case1 || case2) {
                                    if (successful) {
                                        projectScreen.getInstallationState()
                                                .setProjectStatus(projectScreen.getProjectId(), ProjectStatus.INSTALLED);
                                    } else {
                                        projectScreen.getInstallationState()
                                                .removeProject(projectScreen.getProjectId());
                                    }
                                }
                                versionButtons[finalI].active = !successful;
                                if (successful) {
                                    versionButtons[finalI].setMessage(Component.nullToEmpty("Installed"));
                                } else {
                                    versionButtons[finalI].setMessage(Component.nullToEmpty("Install"));
                                }
                                if (!successful) {
                                    Minecraft.getInstance().execute(() -> {
                                        Minecraft.getInstance().gui.toastManager().addToast(
                                                new SystemToast(
                                                        SystemToast.SystemToastId.WORLD_ACCESS_FAILURE,
                                                        Component.literal("Download Failed: " + projectScreen.getProjectInfo().getTitle()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                                                        Component.literal("Please check your internet connection and try again.").withStyle(ChatFormatting.WHITE)
                                                )
                                        );
                                    });
                                }
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
                            Minecraft.getInstance().gui.setScreen(new VersionDetailsScreen(versions[finalI], projectScreen, projectScreen.getProjectInfo().getProjectType()));
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
        projectScreen.renderMenuBackground(context, 131, projectScreen.getScrollAmount() + ((TabButton) projectScreen.getTabNavigationWidget().children().get(0)).getHeight()-10, projectScreen.width, versions.length * 40 + 10 + prevProgressPadding);

        int progressPadding = 0;
        for (int i = 0; i < versions.length; i++) {
            if (versions[i] == null || versionDetailButtons[i] == null) {
                break;
            }

            if (versionButtons.length != 0) {
                if (projectScreen.getInstallationState().isInstallingHash(versions[i].getHash())) {
                    versionButtons[i].active = false;
                    versionButtons[i].setMessage(Component.nullToEmpty("Installing"));
                }

            }
            int versionStartY = i * 40 + projectScreen.getScrollAmount() + 20 + progressPadding;
            versionDetailButtons[i].setPosition(140, versionStartY);
            //context.drawText(projectScreen.getTextRenderer(), Text.of(versions[i].getName()), 140, i * 40 + projectScreen.getScrollAmount() + 20, Colors.WHITE, true);
            ChatFormatting formatting;
            formatting = switch(versions[i].getVersionType()) {
                case "release" -> ChatFormatting.GREEN;
                case "beta" -> ChatFormatting.GOLD;
                case "alpha" -> ChatFormatting.RED;
                default -> null;
            };
            versionButtons[i].setPosition(projectScreen.width - versionButtons[i].getWidth() - 10, versionStartY);
            if (versionStartY < projectScreen.getHeight() && versionStartY > -50) {
                versionDetailButtons[i].extractRenderState(context, mouseX, mouseY, delta);
                context.text(projectScreen.getFont(), Component.literal("•" + versions[i].getVersionType()).withStyle(formatting), 140, versionStartY + 10, CommonColors.WHITE, true);
                context.text(projectScreen.getFont(), Component.nullToEmpty(versions[i].getVersionNumber()), 140 + projectScreen.getFont().width("•" + versions[i].getVersionType()) + 8, versionStartY + 10 + progressPadding, CommonColors.WHITE, true);
                context.text(projectScreen.getFont(), Component.nullToEmpty(String.format("%,d", versions[i].getNumDownloads()) + " downloads"), projectScreen.width - projectScreen.getFont().width(String.format("%,d", versions[i].getNumDownloads()) + " downloads") - 8, versionStartY + 16, CommonColors.WHITE, true);
                versionButtons[i].extractRenderState(context, mouseX, mouseY, delta);

            }

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

            DownloadProgress downloadProgress = EasyInstallClient.getDownloadProgressFromHash(versions[i].getHash(), projectScreen.getProjectInfo().getProjectType());


            if (downloadProgress != null && downloadProgress.isShowingProgress()) {
                float progress = downloadProgress.getProgress();
                int percentProgress = (int) (progress * 100);
                String progressText = String.valueOf(percentProgress) + "%";
                context.text(projectScreen.getFont(), progressText, projectScreen.width - 10 - projectScreen.getFont().width(progressText), (i + 1) * 40 + 12 + projectScreen.getScrollAmount() + progressPadding - projectScreen.getFont().lineHeight / 2, CommonColors.WHITE, true);


                drawProgressBar(context, 140, (i + 1) * 40 + 12 + projectScreen.getScrollAmount() + progressPadding, projectScreen.width - 156 - projectScreen.getFont().width(progressText), 2, progress);
                progressPadding += 7;
            }
        }

        prevProgressPadding = progressPadding;
        initialized = true;
        context.blit(
                RenderPipelines.GUI_TEXTURED, VERTICAL_SEPARATOR_TEXTURE, 131, projectScreen.getScrollAmount() + ((TabButton) projectScreen.getTabNavigationWidget().children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, versions.length * 40 + 10 + progressPadding, 2, 32
        );
        projectScreen.setMaxY(versions.length * 40 + 10 + progressPadding);
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

    private void drawProgressBar(final GuiGraphicsExtractor graphics, final int left, final int top, final int width, final int height, final float progress) {
        graphics.fill(left, top, left + width, top + height, new Color(0, 0, 0, 100).getRGB());
        graphics.fill(left, top, left + Math.round(progress * (float) width), top + height, new Color(0, 255, 0, 192).getRGB());
    }
}