package neelesh.easy_install.gui.screen;

import com.google.gson.JsonArray;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.ProjectType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.TreeMap;

public class CategoryScreen extends Screen {

    private TreeMap<String, ArrayList<CheckboxWidget>> checkBoxes;
    private ProjectType projectType;
    private double scrollAmount;
    private ProjectBrowser browser;
    private ButtonWidget doneButton;
    private int maxY;
    private ButtonWidget clearButton;
    private JsonArray tags;

    public CategoryScreen(ProjectBrowser browser, ProjectType projectType) {
        super(Text.of("Filter Categories"));
        this.browser = browser;
        this.projectType = projectType;
        tags = EasyInstallClient.getCategoryTags(projectType);
    }


    @Override
    protected void init() {
        super.init();
        checkBoxes = new TreeMap<>();
        doneButton = ButtonWidget.builder(Text.of("Done"), button -> {
            browser.setPage(0);
            browser.setInitialized(false);
            client.setScreen(browser);
        }).build();
        doneButton.setPosition(width/2 - 70, height - 25);
        this.addSelectableChild(doneButton);
        for (int i = 0; i < tags.size(); i++) {
            String name = tags.get(i).getAsJsonObject().get("name").getAsString();
            CheckboxWidget checkBox = CheckboxWidget.builder(Text.of(StringUtils.capitalize(name)), textRenderer).checked(browser.getCategories().contains(name)).callback(((box, checked) -> {
                if (checked) {
                   browser.addFilterCategory(name);
                } else {
                    browser.removeFilterCategory(name);
                }
            })).build();
            this.addSelectableChild(checkBox);
            if (!checkBoxes.containsKey(tags.get(i).getAsJsonObject().get("header").getAsString())) {
                checkBoxes.put(tags.get(i).getAsJsonObject().get("header").getAsString(), new ArrayList<>());
            }
            checkBoxes.get(tags.get(i).getAsJsonObject().get("header").getAsString()).add(checkBox);
        }
        clearButton = ButtonWidget.builder(Text.of("Clear All"), button -> {
            browser.clearCategories();
            refreshWidgetPositions();
        }).build();
        clearButton.setDimensions(100, 20);
        this.addSelectableChild(clearButton);
        if (projectType == ProjectType.RESOURCE_PACK) {
            checkBoxes.get("resolutions").sort((o1, o2) -> {
                String str1 = o1.getMessage().getString();
                String str2 = o2.getMessage().getString();
                return Integer.compare(Integer.parseInt(str1.substring(0, str1.indexOf('x'))), Integer.parseInt(str2.substring(0, str2.indexOf('x'))));
            });
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        clearButton.active = false;
        clearButton.setPosition(width - 115, 8);
        if (checkBoxes != null) {
            int i = 0;
            int offset = 10;
            for (String header : checkBoxes.sequencedKeySet()) {
                ArrayList<CheckboxWidget> boxes = checkBoxes.get(header);
                context.getMatrices().scale(1.4f, 1.4f, 1);
                context.drawText(textRenderer, StringUtils.capitalize(header), (int)(20 /1.4f), (int) ((i * 25 + offset + scrollAmount)/1.4f), Colors.WHITE, true);
                context.getMatrices().scale(1/1.4f,1/1.4f, 1f);
                offset += 20;
                for (CheckboxWidget box : boxes) {
                    box.setPosition(20, i * 25 + offset + (int) scrollAmount);
                    box.render(context, mouseX, mouseY, delta);
                    if (box.isChecked()) {
                        clearButton.active = true;
                    }
                    i++;
                }

            }
            maxY = i * 25 + offset;
        }
        doneButton.render(context, mouseX, mouseY, delta);
        clearButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollAmount + verticalAmount * 12 <= 0 && scrollAmount + verticalAmount * 12 >= -20 - maxY + height) {
            scrollAmount += 12 * verticalAmount;
        } else if (scrollAmount + verticalAmount * 12 > 0) {
            scrollAmount = 0;
        } else if (scrollAmount + verticalAmount * 12 < -20 - maxY + height) {
            scrollAmount = - 20 - maxY + height;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

    }

}
