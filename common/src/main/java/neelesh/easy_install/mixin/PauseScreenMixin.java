package neelesh.easy_install.mixin;


import neelesh.easy_install.ProjectType;
import neelesh.easy_install.gui.screen.ProjectBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }
    private Button buttonWidget = new Button.Builder(Component.nullToEmpty("\uD83D\uDCE5 Add Mods"), button -> {
        ProjectBrowser modBrowser = new ProjectBrowser(this, ProjectType.MOD);
        Minecraft.getInstance().setScreen(modBrowser);
    }).build();


    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void addButton(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        buttonWidget.setSize(65, 20);
        buttonWidget.setPosition(width/2 + 115, height / 4 + 56);
        buttonWidget.extractRenderState(context, mouseX, mouseY, delta);
        this.addWidget(buttonWidget);
    }
}
