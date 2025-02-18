package neelesh.easy_install.mixin.client;


import com.google.common.collect.ImmutableList;
import neelesh.easy_install.gui.tab.TabNavigationMixinInterface;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TabNavigationWidget.class)
public class TabNavigationMixin implements TabNavigationMixinInterface {
    @Shadow
    private final DirectionalLayoutWidget grid;

    @Shadow
    private final ImmutableList<TabButtonWidget> tabButtons;

    public TabNavigationMixin(ImmutableList<TabButtonWidget> tabButtons) {
        this.tabButtons = tabButtons;
        this.grid = DirectionalLayoutWidget.horizontal();
    }

    @Unique
    @Override
    public void setX(int x) {
        grid.setX(x);
    }

    @Unique
    @Override
    public void setY(int y) {
        grid.setY(y);
    }


    @Unique
    @Override
    public void setButtonWidth(int width) {
        for (TabButtonWidget tabButtonWidget : this.tabButtons) {
            tabButtonWidget.setWidth(width);
        }
        grid.refreshPositions();
    }
}
