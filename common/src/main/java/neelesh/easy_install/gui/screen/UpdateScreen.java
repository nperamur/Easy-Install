package neelesh.easy_install.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import neelesh.easy_install.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

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
    private ArrayList<ButtonWidget> installButtons = new ArrayList<>();
    private ArrayList<PressableTextWidget> versionDetailButtons = new ArrayList<>();
    private ButtonWidget updateAll = ButtonWidget.builder(Text.of("Update All"), button -> {}).size(0, 0).build();
    private ButtonWidget doneButton;
    private double scrollAmount;
    private Screen parent;
    private ExecutorService fileWriteScheduler = Executors.newSingleThreadExecutor();


    protected UpdateScreen(ProjectType projectType, Screen parent) {
        super(Text.of("Update Screen"));
        this.parent = parent;
        this.updateAll.visible = false;
        Thread thread = new Thread(() -> {
            ArrayList<Version> versionTemp = EasyInstallClient.getUpdatedVersions(projectType);
            JsonArray projectIds = new JsonArray();
            for (int i = 0; i < versionTemp.size(); i++) {
                projectIds.add(versionTemp.get(i).getId());
                ICON_TEXTURE_ID.add(Identifier.of(EasyInstall.MOD_ID, "update_icon" + i));
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
                            MinecraftClient.getInstance().execute(() -> {
                                titles.set(finalI, jsonArray.get(finalX).getAsJsonObject().get("title").getAsString());

                                versionDetailButtons.add(new PressableTextWidget(140, (int) (finalI * 40 + scrollAmount), textRenderer.getWidth(versionTemp.get(finalI).getName()), 9, Text.of(versionTemp.get(finalI).getName()), button -> {
                                    MinecraftClient.getInstance().setScreen(new VersionDetailsScreen(versionTemp.get(finalI), this));
                                }, textRenderer));
                                this.addSelectableChild(versionDetailButtons.get(finalI));
                                Thread thread2 = new Thread(() -> {
                                    try {
                                        ImageLoader.loadPlaceholder(ICON_TEXTURE_ID.get(finalI));
                                        ImageLoader.loadImage(URI.create(jsonArray.get(finalX).getAsJsonObject().get("icon_url").getAsString()).toURL(), ICON_TEXTURE_ID.get(finalI), client);
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
            MinecraftClient.getInstance().execute(() -> {
                this.versions = versionTemp;
                this.scrollAmount = 0;
                for (int i = 0; i < versions.size(); i++) {
                    int finalI = i;
                    installButtons.add(ButtonWidget.builder(Text.of("Update"), button -> {
                        updateVersion(projectType, versions.get(finalI));
                        button.visible = false;
                    }).build());
                    installButtons.get(i).setDimensions(60, 18);
                    installButtons.get(i).setPosition(width - 70, i * 50 + 30);
                    this.addSelectableChild(installButtons.get(i));

                }
                this.addSelectableChild(doneButton);

                updateAll = ButtonWidget.builder(Text.of("Update All"), button -> {
                    for (Version version : versions) {
                        updateVersion(projectType, version);
                    }
                    button.visible = false;
                    button.setFocused(false);
                }).build();
                updateAll.setDimensions(60, 18);
                updateAll.setPosition(width - 70, 2);
                this.addSelectableChild(updateAll);
            });

        });
        thread.start();


    }

    @Override
    protected void init() {
        super.init();

        doneButton = ButtonWidget.builder(Text.of("Done"), button -> MinecraftClient.getInstance().setScreen(parent)).build();
        this.addSelectableChild(doneButton);
        this.addSelectableChild(updateAll);
        for (int i = 0; i < versions.size(); i++) {
            this.addSelectableChild(installButtons.get(i));
        }

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.setFocused(null);
        renderDarkening(context);
        doneButton.setDimensions(80, 18);
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
        context.drawText(textRenderer, updateText, width / 2 - textRenderer.getWidth(updateText)/2, 10 + (int) scrollAmount, Colors.WHITE, true);
        int i = 0;
        while(i < versions.size()) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE_ID.get(i), 0, i * 50 + 30 + (int) scrollAmount, 0, 0, 40, 40, 40, 40);
            context.getMatrices().scale(1.5f, 1.5f);
            context.drawText(textRenderer, titles.get(i), (int) (50 / 1.5), (int) ((i * 50 + 30) / 1.5 + scrollAmount / 1.5), Colors.WHITE, true);
//            context.getMatrices().scale((float) 2 / 3, (float) 2 / 3, (float) 2 / 3);
            context.getMatrices().scale((float) 2 / 3, (float) 2 / 3);
            //context.drawText(textRenderer, versions.get(i).getName(), 50, i * 50 + 45 +  (int) scrollAmount, Colors.WHITE, true);
            int finalI = i;
            this.remove(versionDetailButtons.get(i));
            versionDetailButtons.set(i, new PressableTextWidget(140, (int) (i * 40 + scrollAmount), textRenderer.getWidth(versions.get(i).getName()), 9, Text.of(versions.get(i).getName()), button -> {
                MinecraftClient.getInstance().setScreen(new VersionDetailsScreen(versions.get(finalI), this));
            }, textRenderer));
            this.addSelectableChild(versionDetailButtons.get(i));
            versionDetailButtons.get(i).setPosition(50, i * 50 + 45 + (int) scrollAmount);
            versionDetailButtons.get(i).render(context, mouseX, mouseY, delta);
            Formatting formatting;

            formatting = switch (versions.get(i).getVersionType()) {
                case "release" -> Formatting.GREEN;
                case "beta" -> Formatting.GOLD;
                case "alpha" -> Formatting.RED;
                default -> null;
            };
            context.drawText(textRenderer, Text.literal("•" + versions.get(i).getVersionType()).formatted(formatting), 50, i * 50 + 55 + (int) scrollAmount, Colors.WHITE, true);
            context.drawText(textRenderer, Text.of(versions.get(i).getVersionNumber()), 50 + textRenderer.getWidth("•" + versions.get(i).getVersionType()) + 8, i * 50 + 55 + (int) scrollAmount, Colors.WHITE, true);
            context.drawText(textRenderer, Text.of(String.format("%,d", versions.get(i).getNumDownloads()) + " downloads"), width - textRenderer.getWidth(String.format("%,d", versions.get(i).getNumDownloads()) + " downloads") - 8, installButtons.get(i).getY() + installButtons.get(i).getHeight() + 2, Colors.WHITE, true);

            installButtons.get(i).render(context, mouseX, mouseY, delta);
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
        updateAll.render(context, mouseX, mouseY, delta);
        doneButton.render(context, mouseX, mouseY, delta);

    }



    public void updateVersion(ProjectType projectType, Version version) {
        Thread thread = new Thread(() -> {
            version.download();
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
    public void close() {
        super.close();
        fileWriteScheduler.shutdown();
    }

    @Override
    public void removed() {
        super.removed();
        fileWriteScheduler.shutdown();
    }




}
