package neelesh.easy_install.mixin.client;


import neelesh.easy_install.gui.screen.ProjectBrowser;
import neelesh.easy_install.ProjectType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
	private ButtonWidget buttonWidget = new ButtonWidget.Builder(Text.of("Add Mods"), button -> {
		ProjectBrowser modBrowser = new ProjectBrowser(this, ProjectType.MOD);
		MinecraftClient.getInstance().setScreen(modBrowser);
	}).build();

	protected TitleScreenMixin(Text title) {
		super(title);
	}


	@Inject(method = "render", at = @At("TAIL"))
	private void addCustomButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		buttonWidget.setHeight(15);
		buttonWidget.setWidth(80);
		buttonWidget.setPosition(175, height-15);
		buttonWidget.render(context, mouseX, mouseY, delta);
		this.addSelectableChild(buttonWidget);
	}






}






