package neelesh.easy_install.gui.widget;

import org.spongepowered.asm.mixin.Unique;

public interface ButtonWidgetInterface {
    @Unique
    void setDimensions(int width, int height);
}
