package neelesh.easy_install.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class VersionPickerWidget extends ObjectSelectionList<VersionPickerWidget.Entry> {
   private Screen parent;

   private ArrayList<Entry> entries;

    public VersionPickerWidget(Screen parent, Minecraft minecraftClient, int width, int height, int y, int itemHeight) {
        super(minecraftClient, width, height, y, itemHeight);
        this.parent = parent;
        entries = new ArrayList<>();
    }

    @Environment(EnvType.CLIENT)
    public static class Entry extends ObjectSelectionList.Entry<VersionPickerWidget.Entry> implements AutoCloseable {
        private final StringWidget widget;

        public Entry(Component text, Font textRenderer) {
            this.widget = new StringWidget(text, textRenderer);
        }

        public Component getNarration() {
            return this.widget.getMessage();
        }

        public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            this.widget.setPosition(this.getContentXMiddle() - this.widget.getWidth() / 2, this.getContentYMiddle() - this.widget.getHeight() / 2);
            this.widget.extractRenderState(context, mouseX, mouseY, deltaTicks);
        }

        @Override
        public void close() throws Exception {

        }
    }


    public Entry getEntry(int index) {
        return this.entries.get(index);
    }

    public ArrayList<Entry> getEntries() {
        return new ArrayList<>(entries);
    }



    public Entry addEntry(String versionName) {
        Entry entry = new Entry(Component.nullToEmpty(versionName), parent.getFont());
        this.entries.add(entry);
        super.addEntry(entry);
        return entry;
    }

    @Override protected int scrollBarX() {
        return this.getRight() - 6;
    }

}
