package neelesh.easy_install.gui.widget;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CustomTabNavigationWidget extends AbstractParentElement implements Drawable, Element, Selectable {

    private static final int DEFAULT_TAB_HEIGHT = 24;
    private static final int MAX_WIDTH = 400;
    private static final Text USAGE_NARRATION_TEXT = Text.translatable("narration.tab_navigation.usage");

    private final GridWidget grid;
    private final TabManager tabManager;
    private final ImmutableList<Tab> tabs;
    private final ImmutableList<TabButtonWidget> tabButtons;
    private int tabNavWidth;

    public CustomTabNavigationWidget(int width, TabManager tabManager, Iterable<Tab> tabs) {
        super();
        this.tabNavWidth = width;
        this.tabManager = tabManager;
        this.tabs = ImmutableList.copyOf(tabs);
        this.grid = new GridWidget(0, 0);
        this.grid.getMainPositioner().alignHorizontalCenter();

        ImmutableList.Builder<TabButtonWidget> builder = ImmutableList.builder();
        int i = 0;
        for (Tab tab : tabs) {
            builder.add(this.grid.add(new CustomTabWidget(tabManager, tab, 0, DEFAULT_TAB_HEIGHT), 0, i++));
        }

        this.tabButtons = builder.build();
    }

    public static Builder builder(TabManager tabManager, int width) {
        return new Builder(tabManager, width);
    }

    public void setWidth(int width) {
        this.tabNavWidth = width;
    }

    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (this.getFocused() != null) {
            this.getFocused().setFocused(focused);
        }
    }

    public void setFocused(@Nullable Element focused) {
        super.setFocused(focused);
        if (focused instanceof TabButtonWidget tabButton) {
            this.tabManager.setCurrentTab(tabButton.getTab(), true);
        }
    }

    @Nullable
    public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
        if (!this.isFocused()) {
            TabButtonWidget currentButton = this.getCurrentTabButton();
            if (currentButton != null) {
                return GuiNavigationPath.of(this, GuiNavigationPath.of(currentButton));
            }
        }
        return navigation instanceof GuiNavigation.Tab ? null : super.getNavigationPath(navigation);
    }

    public List<? extends Element> children() {
        return this.tabButtons;
    }

    public Selectable.SelectionType getType() {
        return this.tabButtons.stream()
                .map(ClickableWidget::getType)
                .max(Comparator.naturalOrder())
                .orElse(SelectionType.NONE);
    }

    public void appendNarrations(NarrationMessageBuilder builder) {
        Optional<TabButtonWidget> hoveredOrCurrent = this.tabButtons.stream()
                .filter(ClickableWidget::isHovered)
                .findFirst()
                .or(() -> Optional.ofNullable(this.getCurrentTabButton()));

        hoveredOrCurrent.ifPresent(button -> {
            this.appendNarrations(builder.nextMessage(), button);
            button.appendNarrations(builder);
        });

        if (this.isFocused()) {
            builder.put(NarrationPart.USAGE, USAGE_NARRATION_TEXT);
        }
    }

    protected void appendNarrations(NarrationMessageBuilder builder, TabButtonWidget button) {
        if (this.tabs.size() > 1) {
            int index = this.tabButtons.indexOf(button);
            if (index != -1) {
                builder.put(NarrationPart.POSITION,
                        Text.translatable("narrator.position.tab", index + 1, this.tabs.size()));
            }
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.tabNavWidth, 24, -16777216);
        context.drawTexture(CreateWorldScreen.HEADER_SEPARATOR_TEXTURE, 0,
                this.grid.getY() + this.grid.getHeight() - 2,
                0.0F, 0.0F, this.tabNavWidth, 2, 32, 2);

        for (TabButtonWidget tabButton : this.tabButtons) {
            tabButton.render(context, mouseX, mouseY, delta);
        }
    }

    public ScreenRect getNavigationFocus() {
        return this.grid.getNavigationFocus();
    }

    public void init() {
        int i = Math.min(MAX_WIDTH, this.tabNavWidth) - 28;
        int buttonWidth = MathHelper.roundUpToMultiple(i / this.tabs.size(), 2);

        for (TabButtonWidget tabButton : this.tabButtons) {
            tabButton.setWidth(buttonWidth);
        }

        this.grid.refreshPositions();
        this.grid.setX(MathHelper.roundUpToMultiple((this.tabNavWidth - i) / 2, 2));
        this.grid.setY(0);
    }

    public void selectTab(int index, boolean clickSound) {
        if (this.isFocused()) {
            this.setFocused(this.tabButtons.get(index));
        } else {
            this.tabManager.setCurrentTab(this.tabs.get(index), clickSound);
        }
    }


    private int getCurrentTabIndex() {
        Tab currentTab = this.tabManager.getCurrentTab();
        int index = this.tabs.indexOf(currentTab);
        return index != -1 ? index : -1;
    }

    @Nullable
    private TabButtonWidget getCurrentTabButton() {
        int index = getCurrentTabIndex();
        return index != -1 ? this.tabButtons.get(index) : null;
    }


    @Environment(EnvType.CLIENT)
    public static class Builder {
        private final int width;
        private final TabManager tabManager;
        private final List<Tab> tabs = new ArrayList<>();

        Builder(TabManager tabManager, int width) {
            this.tabManager = tabManager;
            this.width = width;
        }

        public Builder tabs(Tab... tabs) {
            Collections.addAll(this.tabs, tabs);
            return this;
        }

        public CustomTabNavigationWidget build() {
            return new CustomTabNavigationWidget(this.width, this.tabManager, this.tabs);
        }
    }
}
