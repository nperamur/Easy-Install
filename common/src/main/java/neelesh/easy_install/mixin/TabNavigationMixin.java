package neelesh.easy_install.mixin;


import com.google.common.collect.ImmutableList;
import neelesh.easy_install.gui.tab.TabNavigationMixinInterface;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TabNavigationBar.class)
public class TabNavigationMixin implements TabNavigationMixinInterface {
    @Shadow
    private final LinearLayout layout;

    @Shadow
    private final ImmutableList<TabButton> tabButtons;

    public TabNavigationMixin(ImmutableList<TabButton> tabButtons) {
        this.tabButtons = tabButtons;
        this.layout = LinearLayout.horizontal();
    }

    @Unique
    @Override
    public void setX(int x) {
        layout.setX(x);
    }

    @Unique
    @Override
    public void setY(int y) {
        layout.setY(y);
    }


    @Unique
    @Override
    public void setButtonWidth(int width) {
        for (TabButton tabButtonWidget : this.tabButtons) {
            tabButtonWidget.setWidth(width);
        }
        layout.arrangeElements();
    }
}
