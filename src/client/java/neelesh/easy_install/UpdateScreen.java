package neelesh.easy_install;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
//TODO: FIX UPDATING, ADD SCROLLING, ADD DONE BUTTON
public class UpdateScreen extends Screen {
    private ArrayList<Version> versions;
    private ArrayList<String> titles;
    private ArrayList<Identifier> ICON_TEXTURE_ID;
    private ArrayList<ButtonWidget> installButtons;
    private ButtonWidget updateAll;
    private ButtonWidget doneButton;
    private double scrollAmount;
    private Screen parent;



    protected UpdateScreen(ProjectType projectType, Screen parent) {
        super(Text.of("Update Screen"));
        versions = EasyInstallClient.getUpdatedVersions(projectType);
        this.scrollAmount = 0;
        this.parent = parent;
        titles = new ArrayList<>();
        ICON_TEXTURE_ID = new ArrayList<>();
        doneButton = ButtonWidget.builder(Text.of("Done"), button -> MinecraftClient.getInstance().setScreen(parent)).build();
        installButtons = new ArrayList<>();
        for (int i = 0; i < versions.size(); i++) {
            ICON_TEXTURE_ID.add(Identifier.of(EasyInstall.MOD_ID, "update_icon" + i));
            int finalI = i;
            installButtons.add(ButtonWidget.builder(Text.of("Update"), button -> {
                updateVersion(projectType, finalI);
                button.visible = false;
            }).build());

        }
        updateAll = ButtonWidget.builder(Text.of("Update All"), button -> {
            for (int j = 0; j < versions.size(); j++) {
                updateVersion(projectType, j);
                button.visible = false;
            }
        }).build();

    }

    @Override
    protected void init() {
        super.init();
        this.addSelectableChild(doneButton);
        if (versions.isEmpty()) {
            return;
        }
        JsonArray projectIds = new JsonArray();
        for (int i = 0; i < versions.size(); i++) {
            projectIds.add(versions.get(i).getId());
            installButtons.get(i).setDimensions(60, 18);
            installButtons.get(i).setPosition(width - 70, i * 50 + 30);
            this.addSelectableChild(installButtons.get(i));
        }

        updateAll.setDimensions(60, 18);
        updateAll.setPosition(width - 70, 2);
        this.addSelectableChild(updateAll);
        try {
            URL url = URI.create("https://api.modrinth.com/v2/projects?ids=" + URLEncoder.encode(projectIds.toString(), StandardCharsets.UTF_8)).toURL();
            System.out.println(url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String response = reader.lines().collect(Collectors.joining("\n"));
                    JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                    for (int i = 0; i < versions.size(); i++) {
                        int x = 0;
                        for (int j = 0; j < jsonArray.size(); j++) {
                            if (jsonArray.get(j).getAsJsonObject().get("id").getAsString().equals(versions.get(i).getId())) {
                                x = j;
                                break;
                            }
                        }
                        titles.add(jsonArray.get(x).getAsJsonObject().get("title").getAsString());
                        int finalX = x;
                        int finalI = i;
                        Thread thread = new Thread(() -> {
                            try {
                                client.execute(() -> {
                                    NativeImageBackedTexture texture = new NativeImageBackedTexture(new NativeImage(1, 1, false));
                                    texture.getImage().setColorArgb(0, 0, 0x00000000);
                                    texture.upload();
                                    client.getTextureManager().registerTexture(ICON_TEXTURE_ID.get(finalI), texture);
                                });
                                IconManager.loadIcon(URI.create(jsonArray.get(finalX).getAsJsonObject().get("icon_url").getAsString()).toURL(), ICON_TEXTURE_ID.get(finalI), client);
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        thread.start();
                    }
                }

            }
            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
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
        context.drawText(textRenderer, updateText, width / 2 - textRenderer.getWidth(updateText)/2, 10 + (int) scrollAmount, 0xFFFFFF, true);
        int i = 0;
        while(i < versions.size()) {
            //context.drawText(textRenderer, versions.get(i).getName(), width / 2 - textRenderer.getWidth(updateText)/2, 20 + (i + 1) * 30, 0xFFFFFF, true);
            context.drawTexture(RenderLayer::getGuiTextured, ICON_TEXTURE_ID.get(i), 0, i * 50 + 30 + (int) scrollAmount, 0, 0, 40, 40, 40, 40);
            context.getMatrices().scale(1.5f, 1.5f, 1.5f);
            context.drawText(textRenderer, titles.get(i), (int) (50 / 1.5), (int) ((i * 50 + 30) / 1.5 + scrollAmount/1.5), 0xFFFFFF, true);
            context.getMatrices().scale((float) 2 / 3, (float) 2 / 3, (float) 2 / 3);
            context.drawText(textRenderer, versions.get(i).getName(), 50, i * 50 + 45 +  (int) scrollAmount, 0xFFFFFF, true);
            Formatting formatting;

            formatting = switch(versions.get(i).getVersionType()) {
                case "release" -> Formatting.GREEN;
                case "beta" -> Formatting.GOLD;
                case "alpha" -> Formatting.RED;
                default -> null;
            };
            context.drawText(textRenderer, Text.literal("•" + versions.get(i).getVersionType()).formatted(formatting), 50, i * 50 + 55 + (int) scrollAmount, 0xFFFFFF, true);
            context.drawText(textRenderer, Text.of(versions.get(i).getVersionNumber()), 50 + textRenderer.getWidth("•" + versions.get(i).getVersionType()) + 8, i * 50 + 55 + + (int) scrollAmount, 0xFFFFFF, true);
            context.drawText(textRenderer, Text.of(String.format("%,d", versions.get(i).getNumDownloads()) + " downloads"), width - textRenderer.getWidth(String.format("%,d", versions.get(i).getNumDownloads()) + " downloads") - 8, installButtons.get(i).getY() + installButtons.get(i).getHeight() + 2, 0xFFFFFF, true);

            installButtons.get(i).render(context, mouseX, mouseY, delta);
            if (!installButtons.get(i).visible) {
                installButtons.get(i).visible = true;
                installButtons.removeLast();
                versions.remove(i);
                titles.remove(i);
                ICON_TEXTURE_ID.remove(i);
            } else if (!updateAll.visible) {
                installButtons.remove(i);
                versions.remove(i);
                titles.remove(i);
                ICON_TEXTURE_ID.remove(i);
            } else {
                i++;
            }
        }
        context.getMatrices().translate(0, 0, 10);
        updateAll.render(context, mouseX, mouseY, delta);
        doneButton.render(context, mouseX, mouseY, delta);

    }



    public void updateVersion(ProjectType projectType, int index) {
        Thread thread = new Thread(() -> {
            versions.get(index).download();
            EasyInstallClient.checkInstalled(projectType);
        });
        thread.start();
        HashMap<String, String> oldHashes = EasyInstallClient.getOldHashes();
        File dir = new File(EasyInstallClient.getDir(projectType));
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    String hash = EasyInstallClient.createFileHash(file.toPath());
                    if (oldHashes.containsKey(hash) && oldHashes.get(hash).equals(versions.get(index).getHash())) {
                        file.delete();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollAmount + verticalAmount * 12 <= 0 && scrollAmount + verticalAmount * 12 >= -EasyInstallClient.getNumUpdates() * 50 - 45 + height) {
            scrollAmount += verticalAmount*12;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
