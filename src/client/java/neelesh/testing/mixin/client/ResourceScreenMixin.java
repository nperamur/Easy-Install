package neelesh.testing.mixin.client;

import neelesh.testing.ProjectBrowser;
import neelesh.testing.ProjectType;
import neelesh.testing.TestingClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(PackScreen.class)
public class ResourceScreenMixin extends Screen {

    //Shows for: Current Version/ All Versions
    //Tooltip:
    //[Current Version] - Lists all projects available for this version of Minecraft
    //All Versions - Lists all projects available regardless of the version of Minecraft. Use as your own risk.


    @Shadow
    private final Path file;
    private ProjectType projectType;
    private ProjectBrowser browser;
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
            TestingClient.setDataPackTempDir(file);
            this.buttonText = "Add data packs";
        }
        this.browser = new ProjectBrowser(this, projectType);
        this.buttonWidget = new ButtonWidget.Builder(Text.of(buttonText), button -> {
            TestingClient.search("", projectType);
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
