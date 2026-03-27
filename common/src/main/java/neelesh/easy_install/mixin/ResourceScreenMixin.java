package neelesh.easy_install.mixin;

import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.ProjectType;
import neelesh.easy_install.gui.screen.ProjectBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(PackSelectionScreen.class)
public class ResourceScreenMixin extends Screen {
    @Shadow
    private final Path packDir;
    private ProjectType projectType;
    private String buttonText;
    private Button buttonWidget;
    protected ResourceScreenMixin(Component title, Path file) {
        super(title);
        this.packDir = file;
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void init(CallbackInfo ci) {
        if (getTitle().equals(Component.translatable("resourcePack.title"))) {
            this.projectType = ProjectType.RESOURCE_PACK;
            this.buttonText = "Add resource packs";
        } else {
            this.projectType = ProjectType.DATA_PACK;
            EasyInstallClient.setDataPackTempDir(packDir);
            this.buttonText = "Add data packs";
        }
        this.buttonWidget = new Button.Builder(Component.nullToEmpty(buttonText), button -> {
            ProjectBrowser browser = new ProjectBrowser(this, projectType);
            Minecraft.getInstance().setScreen(browser);
        }).build();
        buttonWidget.setHeight(15);
        buttonWidget.setWidth(110);
        buttonWidget.setPosition(width / 2 - 215, 0);
        this.addWidget(buttonWidget);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        buttonWidget.extractRenderState(context, mouseX, mouseY, delta);
    }
}
