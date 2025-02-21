package neelesh.easy_install.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import neelesh.easy_install.EasyInstall;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.ImageLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class ProfileScreen extends Screen {
    private Identifier avatarId;
    private String userName;
    private String bio;
    private ButtonWidget doneButton;
    private Identifier[] projectIconIds;

    protected ProfileScreen(String name, Screen parent) {
        super(Text.of("Profile Screen"));
        doneButton = ButtonWidget.builder(Text.of("Done"), button -> {
            client.setScreen(parent);
        }).build();
        this.addSelectableChild(doneButton);
        this.userName = name;
        Thread thread = new Thread(() -> {
            JsonObject userProfile = EasyInstallClient.getUserProfile(name);
            if (userProfile != null) {
                try {
                    this.bio = userProfile.get("bio").getAsString().replace("\n", "");
                } catch (Exception e) {
                    this.bio = "A Modrinth creator";
                }
                try {
                    URL avatarUrl = URI.create(userProfile.get("avatar_url").getAsString()).toURL();
                    Identifier avatarId = Identifier.of(EasyInstall.MOD_ID, "avatar");
                    ImageLoader.loadPlaceholder(avatarId);
                    ImageLoader.loadImage(avatarUrl, avatarId, MinecraftClient.getInstance());
                    this.avatarId = avatarId;
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.getMatrices().scale(1.4f, 1.4f, 1);
        context.drawText(textRenderer, userName, 50, 5, Colors.WHITE, true);
        context.getMatrices().scale(1 / 1.4f, 1 / 1.4f, 1);
        if (avatarId != null) {
            context.drawTexture(RenderLayer::getGuiTextured, avatarId, 10, 5, 0, 0, 50, 50, 50, 50, Colors.WHITE);
        }
        if (bio != null) {
            context.drawWrappedText(textRenderer, Text.of(bio), 70, 25, this.width - 70, Colors.WHITE, false);
        }
        doneButton.setPosition(width / 2 - doneButton.getWidth() / 2, height - 25);
        doneButton.render(context, mouseX, mouseY, delta);

        if (projectIconIds != null) {
            for (int i = 0; i < projectIconIds.length; i++) {
                if (projectIconIds[i] != null) {
                    context.drawTexture(RenderLayer::getGuiTextured, projectIconIds[i], 10, 65 + 50 * i, 0, 0, 45, 45, 45, 45, Colors.WHITE);
                }
            }
        }
    }
}
