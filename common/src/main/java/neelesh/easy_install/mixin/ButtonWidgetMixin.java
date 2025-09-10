package neelesh.easy_install.mixin;

import neelesh.easy_install.gui.widget.ButtonWidgetInterface;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClickableWidget.class)

public class ButtonWidgetMixin implements ButtonWidgetInterface {
    @Shadow
    private int width;

    @Shadow
    private int height;

    @Unique
    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }

}

