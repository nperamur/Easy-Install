package neelesh.easy_install.mixin.client;

import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.gui.screen.ProjectBrowser;
import neelesh.easy_install.ProjectType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(PackScreen.class)
public class ResourceScreenMixin extends Screen {
    @Shadow
    private final Path file;
    private ProjectType projectType;
    private String buttonText;
    private ButtonWidget buttonWidget;
    protected ResourceScreenMixin(Text title, Path file) {
        super(title);
        this.file = file;
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void init(CallbackInfo ci) {
        if (getTitle().equals(Text.translatable("resourcePack.title"))) {
            this.projectType = ProjectType.RESOURCE_PACK;
            this.buttonText = "Add resource packs";
        } else {
            this.projectType = ProjectType.DATA_PACK;
            EasyInstallClient.setDataPackTempDir(file);
            this.buttonText = "Add data packs";
        }
        this.buttonWidget = new ButtonWidget.Builder(Text.of(buttonText), button -> {
            ProjectBrowser browser = new ProjectBrowser(this, projectType);
            MinecraftClient.getInstance().setScreen(browser);
        }).build();
        buttonWidget.setHeight(15);
        buttonWidget.setWidth(110);
        buttonWidget.setPosition(width / 2 - 215, 0);
        this.addSelectableChild(buttonWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        buttonWidget.render(context, mouseX, mouseY, delta);
    }
}
