package neelesh.easy_install.mixin;


import neelesh.easy_install.ProjectType;
import neelesh.easy_install.gui.screen.ProjectBrowser;
import net.irisshaders.iris.gui.element.ShaderPackOptionList;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderPackScreen.class)
public class ShaderPackScreenMixin extends Screen {
    private Button buttonWidget = new Button.Builder(Component.nullToEmpty("Add Shaders"), button -> {
        ProjectBrowser browser = new ProjectBrowser(this, ProjectType.SHADER);
        Minecraft.getInstance().setScreen(browser);
    }).build();

    @Shadow
    private boolean optionMenuOpen = false;

    @Shadow
    private boolean guiHidden = false;

    @Shadow
    private @Nullable ShaderPackOptionList shaderOptionList = null;

    protected ShaderPackScreenMixin(Component title) {
        super(title);
    }


    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        buttonWidget.setHeight(15);
        buttonWidget.setWidth(80);
        buttonWidget.setPosition(width/2-155, 10);
        addWidget(buttonWidget);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void addCustomButton(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!(this.optionMenuOpen && this.shaderOptionList != null) && !guiHidden) {
            buttonWidget.extractRenderState(context, mouseX, mouseY, delta);
        }

    }

}
