package neelesh.easy_install.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import neelesh.easy_install.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
public class UpdateScreen extends Screen {

    private ArrayList<Version> versions = new ArrayList<>();
    private ArrayList<String> titles = new ArrayList<>();
    private ArrayList<Identifier> ICON_TEXTURE_ID = new ArrayList<>();
    private ArrayList<Button> installButtons = new ArrayList<>();
    private ArrayList<PlainTextButton> versionDetailButtons = new ArrayList<>();
    private Button updateAll = Button.builder(Component.nullToEmpty("Update All"), button -> {}).size(0, 0).build();
    private Button doneButton;
    private double scrollAmount;
    private Screen parent;
    private ExecutorService fileWriteScheduler = Executors.newSingleThreadExecutor();


    protected UpdateScreen(ProjectType projectType, Screen parent) {
        super(Component.nullToEmpty("Update Screen"));
        this.parent = parent;
        this.updateAll.visible = false;
        Thread thread = new Thread(() -> {
            ArrayList<Version> versionTemp = EasyInstallClient.getUpdatedVersions(projectType);
            JsonArray projectIds = new JsonArray();
            for (int i = 0; i < versionTemp.size(); i++) {
                projectIds.add(versionTemp.get(i).getId());
                ICON_TEXTURE_ID.add(Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "update_icon" + i));
                ImageLoader.loadPlaceholder(ICON_TEXTURE_ID.get(i));
                titles.add("");
            }
            try {
                URL url = URI.create("https://api.modrinth.com/v2/projects?ids=" + URLEncoder.encode(projectIds.toString(), StandardCharsets.UTF_8)).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String response = reader.lines().collect(Collectors.joining("\n"));
                        JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                        for (int i = 0; i < versionTemp.size(); i++) {
                            int x = 0;
                            for (int j = 0; j < jsonArray.size(); j++) {
                                if (jsonArray.get(j).getAsJsonObject().get("id").getAsString().equals(versionTemp.get(i).getId())) {
                                    x = j;
                                    break;
                                }
                            }
                            int finalX = x;
                            int finalI = i;
                            Minecraft.getInstance().execute(() -> {
                                titles.set(finalI, jsonArray.get(finalX).getAsJsonObject().get("title").getAsString());

                                versionDetailButtons.add(new PlainTextButton(140, (int) (finalI * 40 + scrollAmount), font.width(versionTemp.get(finalI).getName()), 9, Component.nullToEmpty(versionTemp.get(finalI).getName()), button -> {
                                    Minecraft.getInstance().setScreen(new VersionDetailsScreen(versionTemp.get(finalI), this));
                                }, font));
                                this.addWidget(versionDetailButtons.get(finalI));
                                Thread thread2 = new Thread(() -> {
                                    try {
                                        ImageLoader.loadPlaceholder(ICON_TEXTURE_ID.get(finalI));
                                        ImageLoader.loadImage(URI.create(jsonArray.get(finalX).getAsJsonObject().get("icon_url").getAsString()).toURL(), ICON_TEXTURE_ID.get(finalI), minecraft);
                                    } catch (MalformedURLException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                                thread2.start();
                            });
                        }
                    }

                }
                connection.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Minecraft.getInstance().execute(() -> {
                this.versions = versionTemp;
                this.scrollAmount = 0;
                for (int i = 0; i < versions.size(); i++) {
                    int finalI = i;
                    installButtons.add(Button.builder(Component.nullToEmpty("Update"), button -> {
                        updateVersion(projectType, versions.get(finalI));
                        button.visible = false;
                    }).build());
                    installButtons.get(i).setSize(60, 18);
                    installButtons.get(i).setPosition(width - 70, i * 50 + 30);
                    this.addWidget(installButtons.get(i));

                }
                this.addWidget(doneButton);

                updateAll = Button.builder(Component.nullToEmpty("Update All"), button -> {
                    for (Version version : versions) {
                        updateVersion(projectType, version);
                    }
                    button.visible = false;
                    button.setFocused(false);
                }).build();
                updateAll.setSize(60, 18);
                updateAll.setPosition(width - 70, 2);
                this.addWidget(updateAll);
            });

        });
        thread.start();


    }

    @Override
    protected void init() {
        super.init();

        doneButton = Button.builder(Component.nullToEmpty("Done"), button -> Minecraft.getInstance().setScreen(parent)).build();
        this.addWidget(doneButton);
        this.addWidget(updateAll);
        for (int i = 0; i < versions.size(); i++) {
            this.addWidget(installButtons.get(i));
        }

    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        this.setFocused(null);
        extractMenuBackground(context);
        doneButton.setSize(80, 18);
        doneButton.setPosition(0, 0);
        updateAll.visible = !versions.isEmpty() && updateAll.visible;
        for (int i = 0; i < installButtons.size(); i++) {
            installButtons.get(i).setPosition(width - 70, i * 50 + 30 + (int) scrollAmount);
        }
        updateAll.setPosition(width - 70, 2 + (int) scrollAmount);
        String updateText;
        if (versions.size() != 1) {
            updateText = versions.size() + " Updates Available!";
        } else {
            updateText = versions.size() + " Update Available!";
        }
        context.text(font, updateText, width / 2 - font.width(updateText)/2, 10 + (int) scrollAmount, CommonColors.WHITE, true);
        int i = 0;
        while(i < versions.size()) {
            context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE_ID.get(i), 0, i * 50 + 30 + (int) scrollAmount, 0, 0, 40, 40, 40, 40);
            context.pose().scale(1.5f, 1.5f);
            context.text(font, titles.get(i), (int) (50 / 1.5), (int) ((i * 50 + 30) / 1.5 + scrollAmount / 1.5), CommonColors.WHITE, true);
//            context.getMatrices().scale((float) 2 / 3, (float) 2 / 3, (float) 2 / 3);
            context.pose().scale((float) 2 / 3, (float) 2 / 3);
            int finalI = i;
            this.removeWidget(versionDetailButtons.get(i));
            versionDetailButtons.set(i, new PlainTextButton(140, (int) (i * 40 + scrollAmount), font.width(versions.get(i).getName()), 9, Component.nullToEmpty(versions.get(i).getName()), button -> {
                Minecraft.getInstance().setScreen(new VersionDetailsScreen(versions.get(finalI), this));
            }, font));
            this.addWidget(versionDetailButtons.get(i));
            versionDetailButtons.get(i).setPosition(50, i * 50 + 45 + (int) scrollAmount);
            versionDetailButtons.get(i).extractRenderState(context, mouseX, mouseY, delta);
            ChatFormatting formatting;

            formatting = switch (versions.get(i).getVersionType()) {
                case "release" -> ChatFormatting.GREEN;
                case "beta" -> ChatFormatting.GOLD;
                case "alpha" -> ChatFormatting.RED;
                default -> null;
            };
            context.text(font, Component.literal("•" + versions.get(i).getVersionType()).withStyle(formatting), 50, i * 50 + 55 + (int) scrollAmount, CommonColors.WHITE, true);
            context.text(font, Component.nullToEmpty(versions.get(i).getVersionNumber()), 50 + font.width("•" + versions.get(i).getVersionType()) + 8, i * 50 + 55 + (int) scrollAmount, CommonColors.WHITE, true);
            context.text(font, Component.nullToEmpty(String.format("%,d", versions.get(i).getNumDownloads()) + " downloads"), width - font.width(String.format("%,d", versions.get(i).getNumDownloads()) + " downloads") - 8, installButtons.get(i).getY() + installButtons.get(i).getHeight() + 2, CommonColors.WHITE, true);

            installButtons.get(i).extractRenderState(context, mouseX, mouseY, delta);
            if (!installButtons.get(i).visible) {
                installButtons.get(i).visible = true;
                installButtons.getLast().visible = false;
                installButtons.removeLast();
                versions.remove(i);
                titles.remove(i);
                ICON_TEXTURE_ID.remove(i);
                versionDetailButtons.getLast().visible = false;
                versionDetailButtons.removeLast();
            } else if (!updateAll.visible) {
                installButtons.get(i).visible = false;
                installButtons.remove(i);
                versions.remove(i);
                titles.remove(i);
                ICON_TEXTURE_ID.remove(i);
                versionDetailButtons.get(i).visible = false;
                versionDetailButtons.remove(i);
            } else {
                i++;
            }

            if (-EasyInstallClient.getNumUpdates() * 50 - 45 + height > 0) {
                scrollAmount = 0;
            } else if (scrollAmount != 0 && scrollAmount < -EasyInstallClient.getNumUpdates() * 50 - 45 + height) {
                scrollAmount = -EasyInstallClient.getNumUpdates() * 50 - 45 + height;
            }

        }
        updateAll.extractRenderState(context, mouseX, mouseY, delta);
        doneButton.extractRenderState(context, mouseX, mouseY, delta);

    }



    public void updateVersion(ProjectType projectType, Version version) {
        Thread thread = new Thread(() -> {
            version.download(false);
            EasyInstallClient.checkStatus(projectType);
        });
        thread.start();
        fileWriteScheduler.submit(() -> EasyInstallClient.deleteOldFiles(projectType, version.getHash()));
        EasyInstallClient.setNumUpdates(EasyInstallClient.getNumUpdates() - 1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollAmount + verticalAmount * 13 <= 0 && scrollAmount + verticalAmount * 13 >= -EasyInstallClient.getNumUpdates() * 50 - 45 + height) {
            scrollAmount += verticalAmount * 13;
        } else if (scrollAmount + verticalAmount * 13 < -EasyInstallClient.getNumUpdates() * 50 - 45 + height && scrollAmount != 0) {
            scrollAmount = -EasyInstallClient.getNumUpdates() * 50 - 45 + height;
        } else if (scrollAmount + verticalAmount * 13 > 0) {
            scrollAmount = 0;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        super.onClose();
        fileWriteScheduler.shutdown();
    }

    @Override
    public void removed() {
        super.removed();
        fileWriteScheduler.shutdown();
    }




}
