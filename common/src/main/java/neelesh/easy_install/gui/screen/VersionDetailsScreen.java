package neelesh.easy_install.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import neelesh.easy_install.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
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
    private Button doneButton;
    private JsonArray gameVersions;

    public VersionDetailsScreen(Version version, Screen parent) {
        super(Component.nullToEmpty("Version Details"));
        this.version = version;
        doneButton = Button.builder(Component.nullToEmpty("Done"), button -> {
            Minecraft.getInstance().setScreen(parent);
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
                dependencyIconIds[i] = Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "dependency_" + i);
                dependencyNames[i] = jsonObject.get("title").getAsString();
                dependencyTypes[i] = dependencies.get(i).getAsJsonObject().get("dependency_type").getAsString();
                try {
                    ImageLoader.loadPlaceholder(dependencyIconIds[i]);
                    ImageLoader.loadImage(URI.create(jsonObject.get("icon_url").getAsString()).toURL(), dependencyIconIds[i], Minecraft.getInstance());
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
        doneButton.setSize(width / 3 - 20, 20);
        doneButton.setPosition(width * 2 / 3 + 17, height - 20);
        if (version.getChangelog() != null) {
            int height = (int) (font.wordWrapHeight(Component.nullToEmpty(version.getName()), (int) (width * 2 / (3 * 1.5))) * 1.5);
            markdownRenderer = new MarkdownRenderer(version.getChangelog(), 5, 35 + height, this.width * 2 / 3, this);
            markdownRenderer.refreshLinkPositions();
        }

    }


    @Override
    public <T extends GuiEventListener & NarratableEntry> T addSelectableChild(T child) {
        return super.addWidget(child);
    }

    @Override
    public void removeChild(GuiEventListener e) {
        super.removeWidget(e);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        extractMenuBackground(context);
//        context.getMatrices().translate(0, 0, 1);
//        context.getMatrices().translate(0, 0, -1);
        context.pose().scale(1.5f, 1.5f);
        context.textWithWordWrap(font, Component.nullToEmpty(version.getName()), 3, 5 + (int) (scrollAmount / 1.5), (int) (width * 2 / (3 * 1.5)), CommonColors.WHITE, true);
        context.pose().scale(1 / 1.5f, 1 / 1.5f);
        if (markdownRenderer != null) {
            context.pose().scale(1.2f, 1.2f);
            int height = (int) (font.wordWrapHeight(Component.nullToEmpty(version.getName()), (int) (width * 2 / (3 * 1.5))) * 1.5);
            context.text(font, Component.nullToEmpty("Changelog"), 4, 15 + (int) (height / 1.2) + (int) (scrollAmount / 1.2), CommonColors.WHITE, true);
            context.pose().scale(1 / 1.2f, 1 / 1.2f);
            markdownRenderer.render(context, + (int) scrollAmount);

            context.blit(
                    RenderPipelines.GUI_TEXTURED, VERTICAL_SEPARATOR_TEXTURE, width * 2 / 3 + 10, 0, 0.0F, 0.0F, 2, this.height, 2, 32
            );


            ChatFormatting formatting;
            formatting = switch(version.getVersionType()) {
                case "release" -> ChatFormatting.GREEN;
                case "beta" -> ChatFormatting.GOLD;
                case "alpha" -> ChatFormatting.RED;
                default -> null;
            };
            context.text(font, Component.nullToEmpty("Release Type:"), width * 2 / 3 + 20, 10, CommonColors.WHITE, false);
            context.text(font, Component.literal("•" + version.getVersionType()).withStyle(formatting), width * 2 / 3 + 20, 20, CommonColors.WHITE, true);

            context.text(font, Component.nullToEmpty("Version Number:"), width * 2 / 3 + 20, 35, CommonColors.WHITE, false);
            context.text(font, version.getVersionNumber(), width * 2 / 3 + 20, 45, CommonColors.WHITE, true);

            context.text(font, Component.nullToEmpty("Downloads:"), width * 2 / 3 + 20, 60, CommonColors.WHITE, false);
            context.text(font, Component.nullToEmpty(String.format("%,d", version.getNumDownloads())), width * 2 / 3 + 20, 70, CommonColors.WHITE, true);

            context.text(font, Component.nullToEmpty("File Size:"), width * 2 / 3 + 20, 85, CommonColors.WHITE, false);
            String text = formatFileSize();
            context.text(font, Component.nullToEmpty(text), width * 2 / 3 + 20, 95, CommonColors.WHITE, true);


            context.text(font, Component.nullToEmpty("Game Versions:"), width * 2 / 3 + 20, 110, CommonColors.WHITE, false);
            for (int i = 0; i < gameVersions.size(); i++) {
                JsonElement gameVersion = gameVersions.get(i);
                String str = gameVersion.getAsString();
                if (!gameVersion.equals(gameVersions.get(gameVersions.size() - 1))) {
                    str += ",";
                }
                context.text(font, Component.nullToEmpty(str), width * 2 / 3 + 20, 120 + 10 * i, CommonColors.WHITE, true);

            }

        }
        if (dependencyIconIds != null) {
            if (dependencyIconIds.length > 0) {
                context.pose().scale(1.2f, 1.2f);
                context.text(font, Component.nullToEmpty("Dependencies"), 4, (int) (markdownRenderer.getMaxY() / 1.2 + scrollAmount / 1.2), CommonColors.WHITE, true);
                context.pose().scale(1 / 1.2f, 1 / 1.2f);
            }
            for (int i = 0; i < dependencyIconIds.length; i++) {
                if (dependencyIconIds[i] != null) {
                    context.blit(RenderPipelines.GUI_TEXTURED, dependencyIconIds[i], 4, i * 40 + 20 + (int) scrollAmount + markdownRenderer.getMaxY(), 0, 0, 30, 30, 30, 30);
                    context.text(font, Component.nullToEmpty(dependencyNames[i]), 40, i * 40 + 20 + (int) scrollAmount + markdownRenderer.getMaxY(), CommonColors.WHITE, true);
                    context.text(font, Component.nullToEmpty(StringUtils.capitalize(dependencyTypes[i])), 40, i * 40 + 32 + (int) scrollAmount + markdownRenderer.getMaxY(), CommonColors.LIGHTER_GRAY, true);
                }
            }
        }

        doneButton.extractRenderState(context, mouseX, mouseY, delta);
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
