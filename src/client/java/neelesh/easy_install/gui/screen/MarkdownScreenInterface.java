package neelesh.easy_install.gui.screen;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;

public interface MarkdownScreenInterface {
    <T extends net.minecraft.client.gui.Element & Selectable> T addSelectableChild(T child);
    void removeChild(Element e);
}
