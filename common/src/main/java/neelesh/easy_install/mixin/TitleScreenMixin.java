package neelesh.easy_install.mixin;


import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import neelesh.easy_install.EasyInstall;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.EasyInstallJsonHandler;
import neelesh.easy_install.ProjectType;
import neelesh.easy_install.gui.screen.ConfigButtonPlacement;
import neelesh.easy_install.gui.screen.ProjectBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
	private Button footerButtonWidget = new Button.Builder(Component.nullToEmpty("Add Mods"), button -> {
		ProjectBrowser modBrowser = new ProjectBrowser(this, ProjectType.MOD);
		Minecraft.getInstance().gui.setScreen((Screen) ((Object) modBrowser));
	}).build();
	private SpriteIconButton buttonWidget;
	private final Identifier DOWNLOAD_ICON = Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "download_icon");

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@ModifyVariable(
			method = "init",
			at = @At(value = "STORE", ordinal = 0),
			name = "numberOfButtons"
	)
	private int onSetNumberOfButtons(int originalValue) {
		return originalValue + (EasyInstallJsonHandler.getButtonPlacementConfig() == ConfigButtonPlacement.IN_BUTTON_ROW ? 1 : 0);
	}



	@WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;getHorizontalPosition(III)I"))
	private int replaceInlinedConstant(TitleScreen instance, int currentButton, int numberOfButtons, int buttonWidth, Operation<Integer> original, @Local(name = "numberOfButtons") int actualNumberOfButtons) {
		return original.call(instance, currentButton, actualNumberOfButtons, buttonWidth);
	}

	@Definition(id = "width", field = "Lnet/minecraft/client/gui/screens/TitleScreen;width:I")
	@Expression("this.width / 2 - 100")
	@Inject(method = "init", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 0), order = Integer.MAX_VALUE)
	private void addCustomButton(CallbackInfo ci, @Local(name="currentButton") LocalIntRef currentButton,
								  @Local(name="numberOfButtons") int numButtons, @Local(name="topPos") int topPos) {
//
		if (EasyInstallJsonHandler.getButtonPlacementConfig() == ConfigButtonPlacement.FOOTER) {
			footerButtonWidget.setHeight(15);
			footerButtonWidget.setWidth(80);
			footerButtonWidget.setPosition(font.width(EasyInstallClient.getModLoaderDisplayText()) + 5, height - 15);
			this.addRenderableWidget(footerButtonWidget);
		} else {
			buttonWidget = SpriteIconButton.builder(Component.literal("Add Mods"), (b) -> {
				ProjectBrowser modBrowser = new ProjectBrowser(this, ProjectType.MOD);
				Minecraft.getInstance().gui.setScreen((Screen) ((Object) modBrowser));
			}, false).sprite(DOWNLOAD_ICON, 16, 16).tooltip(Component.literal("Add Mods")).build();

			buttonWidget.setHeight(20);
			buttonWidget.setWidth(20);
			currentButton.set(currentButton.get() + 1);

			buttonWidget.setPosition(this.getHorizontalPosition(currentButton.get(), numButtons, 20), topPos);

			this.addRenderableWidget(buttonWidget);
		}




	}

	@Shadow
	private int getHorizontalPosition(final int currentButton, final int numberOfButtons, final int buttonWidth) {
		throw new RuntimeException();
	}


}






