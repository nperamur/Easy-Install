package neelesh.easy_install.gui.screen;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public interface MarkdownScreenInterface {
    <T extends GuiEventListener & NarratableEntry> T addSelectableChild(T child);
    void removeChild(GuiEventListener e);
}
