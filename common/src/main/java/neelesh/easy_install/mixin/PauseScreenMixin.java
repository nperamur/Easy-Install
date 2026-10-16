package neelesh.easy_install.mixin;


import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import neelesh.easy_install.EasyInstall;
import neelesh.easy_install.ProjectType;
import neelesh.easy_install.gui.screen.ProjectBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PauseScreen.class, priority=Integer.MAX_VALUE)
public class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }
    private Button buttonWidget = new Button.Builder(Component.nullToEmpty("\uD83D\uDCE5 Add Mods"), button -> {
        ProjectBrowser modBrowser = new ProjectBrowser(this, ProjectType.MOD);
        Minecraft.getInstance().gui.setScreen(modBrowser);
    }).build();



    @Inject(
            method = "createPauseMenu()V",
            at = @At(
                    value = "TAIL"
            ),
            order = Integer.MAX_VALUE
    )
    private void addButton(CallbackInfo ci, @Local LocalRef<LinearLayout> iconButtonRow, @Local LocalRef<GridLayout> gridLayout) {
        buttonWidget.setSize(65, 20);
        iconButtonRow.get().arrangeElements();
        if (iconButtonRow.get().getWidth() + buttonWidget.getWidth() + 4 < 204) {
            iconButtonRow.get().addChild(buttonWidget);
        } else {
            buttonWidget.setPosition(width/2 + 106, height / 4 + 56);
            this.addWidget(buttonWidget);
        }
        gridLayout.get().arrangeElements();
        FrameLayout.alignInRectangle(gridLayout.get(), 0, 0, this.width, this.height, 0.5F, 0.25F);
        gridLayout.get().visitWidgets(this::addRenderableWidget);

    }
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderButton(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        buttonWidget.extractRenderState(context, mouseX, mouseY, delta);
    }





}
