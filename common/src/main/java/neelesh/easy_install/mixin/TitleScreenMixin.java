package neelesh.easy_install.mixin;


import dev.architectury.injectables.annotations.ExpectPlatform;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.ProjectType;
import neelesh.easy_install.gui.screen.ProjectBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
	private Button buttonWidget;

	protected TitleScreenMixin(Component title) {
		super(title);
	}


	@Inject(method = "init", at = @At("TAIL"))
	public void init(CallbackInfo ci) {
		buttonWidget = new Button.Builder(Component.nullToEmpty("Add Mods"), button -> {
			ProjectBrowser modBrowser = new ProjectBrowser(this, ProjectType.MOD);
			Minecraft.getInstance().setScreen(modBrowser);
		}).build();
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void addCustomButton(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (buttonWidget != null) {
			buttonWidget.setHeight(15);
			buttonWidget.setWidth(80);
			buttonWidget.setPosition(font.width(EasyInstallClient.getModLoaderDisplayText()) + 10, height-15);
			buttonWidget.render(context, mouseX, mouseY, delta);
			this.addWidget(buttonWidget);
		}
	}

}






