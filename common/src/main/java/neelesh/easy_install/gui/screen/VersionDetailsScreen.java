package neelesh.easy_install.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import neelesh.easy_install.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import static neelesh.easy_install.gui.screen.ProjectScreen.VERTICAL_SEPARATOR_TEXTURE;

public class VersionDetailsScreen extends Screen implements MarkdownScreenInterface {
    private Version version;
    private MarkdownRenderer markdownRenderer;
    private double scrollAmount = 0;
    private Identifier[] dependencyIconIds;
    private String[] dependencyNames;
    private String[] dependencyTypes;
    private ButtonWidget doneButton;
    private JsonArray gameVersions;

    public VersionDetailsScreen(Version version, Screen parent) {
        super(Text.of("Version Details"));
        this.version = version;
        doneButton = ButtonWidget.builder(Text.of("Done"), button -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).build();
        this.addSelectableChild(doneButton);
        JsonArray gameVersions = version.getGameVersions();
        ArrayList<String> versionNumbers = EasyInstallClient.getReleaseVersionNumbers();
        Collections.reverse(versionNumbers);
        int n = 0;
        int j = 0;
        int blockSize = 0;
        while (n < gameVersions.size() && j < versionNumbers.size()) {
            int cmp;
            try {
                cmp = compareMinecraftVersions(gameVersions.get(n).getAsString(), versionNumbers.get(j));
            } catch (NumberFormatException e) {
                if (blockSize > 1) {
                    String str = gameVersions.get(n - 1).getAsString();
                    for (int i = 1; i < blockSize; i++) {
                        gameVersions.remove(n - i);
                    }
                    gameVersions.set(n - blockSize, new JsonPrimitive(gameVersions.get(n - blockSize).getAsString() + " - " + str));
                    n -= blockSize - 1;
                }
                n++;
                blockSize = 0;
                continue;
            }
            if (cmp != 0 || (n == gameVersions.size() - 1)) {
                if (cmp == 0 && n == gameVersions.size() - 1) {
                    blockSize++;
                }
                if (blockSize > 1 && n == gameVersions.size() - 1) {
                    String str = gameVersions.get(n).getAsString();
                    for (int i = 0; i < blockSize - 1; i++) {
                        gameVersions.remove(n - i);
                    }
                    gameVersions.set(n - blockSize + 1, new JsonPrimitive(gameVersions.get(n - blockSize + 1).getAsString() + " - " + str));
                    n -= blockSize - 1;
                } else if (blockSize > 1){
                    String str = gameVersions.get(n - 1).getAsString();
                    for (int i = 1; i < blockSize; i++) {
                        gameVersions.remove(n - i);
                    }
                    gameVersions.set(n - blockSize, new JsonPrimitive(gameVersions.get(n - blockSize).getAsString() + " - " + str));
                    n -= blockSize - 1;
                }
                blockSize = 0;
            }
            if (cmp < 0) {
                n++;
            } else if (cmp > 0) {
                j++;
            } else {
                blockSize++;
                n++;
                j++;
            }
        }

        this.gameVersions = gameVersions;
        Thread thread = new Thread(() -> {
            JsonArray dependencies = version.getDependencies();
            dependencyIconIds = new Identifier[dependencies.size()];
            dependencyNames = new String[dependencies.size()];
            dependencyTypes = new String[dependencies.size()];
            for (int i = 0; i < dependencies.size(); i++) {
                String projectId = dependencies.get(i).getAsJsonObject().get("project_id").getAsString();
                JsonObject jsonObject = EasyInstallClient.getProject(projectId);
                dependencyIconIds[i] = Identifier.of(EasyInstall.MOD_ID, "dependency_" + i);
                dependencyNames[i] = jsonObject.get("title").getAsString();
                dependencyTypes[i] = dependencies.get(i).getAsJsonObject().get("dependency_type").getAsString();
                try {
                    ImageLoader.loadPlaceholder(dependencyIconIds[i]);
                    ImageLoader.loadImage(URI.create(jsonObject.get("icon_url").getAsString()).toURL(), dependencyIconIds[i], MinecraftClient.getInstance());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }

            }
        });
        thread.start();
    }

    @Override
    protected void init() {
        super.init();
        this.addSelectableChild(doneButton);
        doneButton.setDimensions(width / 3 - 20, 20);
        doneButton.setPosition(width * 2 / 3 + 17, height - 20);
        if (version.getChangelog() != null) {
            int height = (int) (textRenderer.getWrappedLinesHeight(Text.of(version.getName()), (int) (width * 2 / (3 * 1.5))) * 1.5);
            markdownRenderer = new MarkdownRenderer(version.getChangelog(), 5, 35 + height, this.width * 2 / 3, this);
            markdownRenderer.refreshLinkPositions();
        }

    }

    @Override
    public <T extends Element & Selectable> T addSelectableChild(T child) {
        return super.addSelectableChild(child);
    }

    @Override
    public void removeChild(Element e) {
        super.remove(e);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderDarkening(context);
        context.getMatrices().translate(0, 0, 1);
        doneButton.render(context, mouseX, mouseY, delta);
        context.getMatrices().translate(0, 0, -1);
        context.getMatrices().scale(1.5f, 1.5f, 1);
        context.drawWrappedText(textRenderer, Text.of(version.getName()), 3, 5 + (int) (scrollAmount / 1.5), (int) (width * 2 / (3 * 1.5)), Colors.WHITE, true);
        context.getMatrices().scale(1 / 1.5f, 1 / 1.5f, 1);
        if (markdownRenderer != null) {
            context.getMatrices().scale(1.2f, 1.2f, 1);
            int height = (int) (textRenderer.getWrappedLinesHeight(Text.of(version.getName()), (int) (width * 2 / (3 * 1.5))) * 1.5);
            context.drawText(textRenderer, Text.of("Changelog"), 4, 15 + (int) (height / 1.2) + (int) (scrollAmount / 1.2), Colors.WHITE, true);
            context.getMatrices().scale(1 / 1.2f, 1 / 1.2f, 1);
            markdownRenderer.render(context, + (int) scrollAmount);

            context.drawTexture(
                    RenderLayer::getGuiTextured, VERTICAL_SEPARATOR_TEXTURE, width * 2 / 3 + 10, 0, 0.0F, 0.0F, 2, this.height, 2, 32
            );


            Formatting formatting;
            formatting = switch(version.getVersionType()) {
                case "release" -> Formatting.GREEN;
                case "beta" -> Formatting.GOLD;
                case "alpha" -> Formatting.RED;
                default -> null;
            };
            context.drawText(textRenderer, Text.of("Release Type:"), width * 2 / 3 + 20, 10, Colors.WHITE, false);
            context.drawText(textRenderer, Text.literal("•" + version.getVersionType()).formatted(formatting), width * 2 / 3 + 20, 20, 0xFFFFFF, true);

            context.drawText(textRenderer, Text.of("Version Number:"), width * 2 / 3 + 20, 35, Colors.WHITE, false);
            context.drawText(textRenderer, version.getVersionNumber(), width * 2 / 3 + 20, 45, Colors.WHITE, true);

            context.drawText(textRenderer, Text.of("Downloads:"), width * 2 / 3 + 20, 60, Colors.WHITE, false);
            context.drawText(textRenderer, Text.of(String.format("%,d", version.getNumDownloads())), width * 2 / 3 + 20, 70, Colors.WHITE, true);

            context.drawText(textRenderer, Text.of("File Size:"), width * 2 / 3 + 20, 85, Colors.WHITE, false);
            String text = formatFileSize();
            context.drawText(textRenderer, Text.of(text), width * 2 / 3 + 20, 95, Colors.WHITE, true);


            context.drawText(textRenderer, Text.of("Game Versions:"), width * 2 / 3 + 20, 110, Colors.WHITE, false);
            for (int i = 0; i < gameVersions.size(); i++) {
                JsonElement gameVersion = gameVersions.get(i);
                String str = gameVersion.getAsString();
                if (!gameVersion.equals(gameVersions.get(gameVersions.size() - 1))) {
                    str += ",";
                }
                context.drawText(textRenderer, Text.of(str), width * 2 / 3 + 20, 120 + 10 * i, Colors.WHITE, true);

            }

        }
        if (dependencyIconIds != null) {
            if (dependencyIconIds.length > 0) {
                context.getMatrices().scale(1.2f, 1.2f, 1);
                context.drawText(textRenderer, Text.of("Dependencies"), 4, (int) (markdownRenderer.getMaxY() / 1.2 + scrollAmount / 1.2), Colors.WHITE, true);
                context.getMatrices().scale(1 / 1.2f, 1 / 1.2f, 1);
            }
            for (int i = 0; i < dependencyIconIds.length; i++) {
                if (dependencyIconIds[i] != null) {
                    context.drawTexture(RenderLayer::getGuiTextured, dependencyIconIds[i], 4, i * 40 + 20 + (int) scrollAmount + markdownRenderer.getMaxY(), 0, 0, 30, 30, 30, 30);
                    context.drawText(textRenderer, Text.of(dependencyNames[i]), 40, i * 40 + 20 + (int) scrollAmount + markdownRenderer.getMaxY(), Colors.WHITE, true);
                    context.drawText(textRenderer, Text.of(StringUtils.capitalize(dependencyTypes[i])), 40, i * 40 + 32 + (int) scrollAmount + markdownRenderer.getMaxY(), Colors.ALTERNATE_WHITE, true);
                }
            }
        }
    }

    private String formatFileSize() {
        String text;
        if (version.getFileSize() > 1000000000) {
            text = String.format("%.2f", (double) version.getFileSize() / 1000000000) + " GB";
        } else if (version.getFileSize() > 1000000) {
            text = String.format("%.2f", (double) version.getFileSize() / 1000000) + " MB";
        } else if (version.getFileSize() > 1000) {
            text = String.format("%.2f", (double) version.getFileSize() / 1000) + " KB";
        } else {
            text = (double) version.getFileSize() + " bytes";
        }
        return text;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollAmount + verticalAmount * 13 < 0 && scrollAmount + verticalAmount * 13 > -markdownRenderer.getMaxY() + height - dependencyNames.length * 40 - 30) {
            scrollAmount += verticalAmount * 13;
        } else if (scrollAmount + verticalAmount * 13 >= 0) {
            scrollAmount = 0;
        } else if (scrollAmount + verticalAmount * 13 <= -markdownRenderer.getMaxY() + height - dependencyNames.length * 40 - 30 && scrollAmount != 0) {
            scrollAmount = -markdownRenderer.getMaxY() + height - dependencyNames.length * 40 - 30;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }


    private int compareMinecraftVersions(String v1, String v2) throws NumberFormatException {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

}
