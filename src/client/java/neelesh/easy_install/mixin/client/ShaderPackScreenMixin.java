package neelesh.easy_install.mixin.client;


import neelesh.easy_install.gui.screen.ProjectBrowser;
import neelesh.easy_install.ProjectType;
import net.irisshaders.iris.gui.element.ShaderPackOptionList;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderPackScreen.class)
public class ShaderPackScreenMixin extends Screen {
    private ButtonWidget buttonWidget = new ButtonWidget.Builder(Text.of("Add Shaders"), button -> {
        ProjectBrowser browser = new ProjectBrowser(this, ProjectType.SHADER);
        MinecraftClient.getInstance().setScreen(browser);
    }).build();

    @Shadow
    private boolean optionMenuOpen = false;

    @Shadow
    private boolean guiHidden = false;

    @Shadow
    private @Nullable ShaderPackOptionList shaderOptionList = null;

    protected ShaderPackScreenMixin(Text title) {
        super(title);
    }


    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        buttonWidget.setHeight(15);
        buttonWidget.setWidth(80);
        buttonWidget.setPosition(width/2-155, 10);
        addSelectableChild(buttonWidget);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void addCustomButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!(this.optionMenuOpen && this.shaderOptionList != null) && !guiHidden) {
            buttonWidget.render(context, mouseX, mouseY, delta);
        }

    }

}
