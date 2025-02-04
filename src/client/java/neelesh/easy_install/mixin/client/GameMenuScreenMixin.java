package neelesh.easy_install.mixin.client;


import neelesh.easy_install.gui.screen.ProjectBrowser;
import neelesh.easy_install.ProjectType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }
    private ButtonWidget buttonWidget = new ButtonWidget.Builder(Text.of("\uD83D\uDCE5 Add Mods"), button -> {
        ProjectBrowser modBrowser = new ProjectBrowser(this, ProjectType.MOD);
        MinecraftClient.getInstance().setScreen(modBrowser);
    }).build();


    @Inject(method = "render", at = @At("TAIL"))
    private void addButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        buttonWidget.setDimensions(65, 20);
        buttonWidget.setPosition(width/2 + 115, height / 4 + 56);
        buttonWidget.render(context, mouseX, mouseY, delta);
        this.addSelectableChild(buttonWidget);
    }
}
