package neelesh.easy_install.gui.screen;

import com.google.gson.JsonObject;
import neelesh.easy_install.EasyInstall;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.ImageLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.resources.Identifier;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class ProfileScreen extends Screen {
    private Identifier avatarId;
    private String userName;
    private String bio;
    private Button doneButton;

    protected ProfileScreen(String name, Screen parent) {
        super(Component.nullToEmpty("Profile Screen"));
        doneButton = Button.builder(Component.nullToEmpty("Done"), button -> {
            minecraft.gui.setScreen(parent);
        }).build();
        this.userName = name;
        Thread thread = new Thread(() -> {
            JsonObject userProfile = EasyInstallClient.getUserProfile(name);
            if (userProfile != null) {
                try {
                    this.bio = userProfile.get("bio").getAsString().replace("\n", " ");
                } catch (Exception e) {
                    this.bio = "A Modrinth creator";
                }
                try {
                    URL avatarUrl = URI.create(userProfile.get("avatar_url").getAsString()).toURL();
                    Identifier avatarId = Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "avatar");
                    ImageLoader.loadPlaceholder(avatarId);
                    ImageLoader.loadImage(avatarUrl, avatarId, Minecraft.getInstance());
                    this.avatarId = avatarId;
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();

    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.pose().scale(1.4f, 1.4f);
        context.text(font, userName, 50, 5, CommonColors.WHITE, true);
        context.pose().scale(1 / 1.4f, 1 / 1.4f);
        if (avatarId != null) {
            context.blit(RenderPipelines.GUI_TEXTURED, avatarId, 10, 5, 0, 0, 50, 50, 50, 50, CommonColors.WHITE);
        }
        if (bio != null) {
            context.textWithWordWrap(font, Component.nullToEmpty(bio), 70, 25, this.width - 70, CommonColors.WHITE, false);
        }
        doneButton.setPosition(width / 2 - doneButton.getWidth() / 2, height - 25);
        doneButton.extractRenderState(context, mouseX, mouseY, delta);
    }



    @Override
    protected void init() {
        super.init();
        this.addWidget(doneButton);
    }
}
