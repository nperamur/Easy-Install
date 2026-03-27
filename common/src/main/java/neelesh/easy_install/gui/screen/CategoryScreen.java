package neelesh.easy_install.gui.screen;

import com.google.gson.JsonArray;
import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.Environment;
import neelesh.easy_install.ProjectType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.TreeMap;

public class CategoryScreen extends Screen {

    private TreeMap<String, ArrayList<Checkbox>> checkBoxes;
    private ProjectType projectType;
    private double scrollAmount;
    private ProjectBrowser browser;
    private Button doneButton;
    private int maxY;
    private Button clearButton;
    private JsonArray tags;
    private Checkbox disableGameVersionFilter;

    private Checkbox requiresClientSide;
    private Checkbox requiresServerSide;

    public CategoryScreen(ProjectBrowser browser, ProjectType projectType) {
        super(Component.nullToEmpty("Filter Categories"));
        this.browser = browser;
        this.projectType = projectType;
        tags = EasyInstallClient.getCategoryTags(projectType);
    }


    @Override
    protected void init() {
        super.init();
        disableGameVersionFilter = Checkbox.builder(Component.nullToEmpty("Disable Game Version Filter"), font)
                        .tooltip(Tooltip.create(Component.literal("- Warning: Disabling the game version filter may allow you to install incompatible content.\n\n").withColor(0xFFA500)
                        .append(Component.literal("- By default, the search results are automatically filtered for this version of Minecraft. If these filters are disabled, it will display all results regardless of the version of Minecraft you are playing on.").withColor(CommonColors.WHITE))))
                        .selected(!browser.isFilteredByGameVersion()).pos(width / 2 - 70, 25).onValueChange(((box, checked) -> {


            if (checked) {
                ConfirmScreen confirmScreen = new ConfirmScreen(bool -> {
                    browser.setFilteredByGameVersion(!bool);
                    minecraft.setScreen(this);
                }, Component.nullToEmpty("Are you sure you want to disable the game version filter?"), Component.literal("Disabling the game version filter may allow you to install incompatible content.").withColor(0xFFA500));
                minecraft.setScreen(confirmScreen);
            } else {
                browser.setFilteredByGameVersion(true);
            }
        })).build();

        this.requiresClientSide = Checkbox.builder(Component.literal("Client Side"), font)
                .selected(browser.getEnvironment() == Environment.CLIENT_SIDE || browser.getEnvironment() == Environment.CLIENT_AND_SERVER)
                .onValueChange(((box, checked) -> {
            if (checked) {
                if (this.requiresServerSide.selected()) {
                    browser.setEnvironment(Environment.CLIENT_AND_SERVER);
                } else {
                    browser.setEnvironment(Environment.CLIENT_SIDE);
                }
            } else {
                if (this.requiresServerSide.selected()) {
                    browser.setEnvironment(Environment.SERVER_SIDE);
                } else {
                    browser.setEnvironment(null);
                }
            }
        })).build();
        requiresClientSide.visible = browser.getProjectType() == ProjectType.MOD;

        this.requiresServerSide = Checkbox.builder(Component.literal("Server Side"), font)
                .selected(browser.getEnvironment() == Environment.SERVER_SIDE || browser.getEnvironment() == Environment.CLIENT_AND_SERVER)
                .onValueChange(((box, checked) -> {
            if (checked) {
                if (this.requiresClientSide.selected()) {
                    browser.setEnvironment(Environment.CLIENT_AND_SERVER);
                } else {
                    browser.setEnvironment(Environment.SERVER_SIDE);
                }
            } else {
                if (this.requiresClientSide.selected()) {
                    browser.setEnvironment(Environment.CLIENT_SIDE);
                } else {
                    browser.setEnvironment(null);
                }
            }
        })).build();
        this.addWidget(requiresClientSide);
        this.addWidget(requiresServerSide);
        requiresServerSide.visible = browser.getProjectType() == ProjectType.MOD;
        disableGameVersionFilter.setTooltipDelay(Duration.ofMillis(500));
        this.addWidget(disableGameVersionFilter);
        checkBoxes = new TreeMap<>();
        doneButton = Button.builder(Component.nullToEmpty("Done"), button -> {
            browser.setPage(0);
            browser.setInitialized(false);
            minecraft.setScreen(browser);
        }).build();
        doneButton.setPosition(width/2 - 70, height - 25);
        this.addWidget(doneButton);
        for (int i = 0; i < tags.size(); i++) {
            String name = tags.get(i).getAsJsonObject().get("name").getAsString();
            Checkbox checkBox = Checkbox.builder(Component.nullToEmpty(StringUtils.capitalize(name)), font).selected(browser.getCategories().contains(name)).onValueChange(((box, checked) -> {
                if (checked) {
                    browser.addFilterCategory(name);
                } else {
                    browser.removeFilterCategory(name);
                }
            })).build();
            this.addWidget(checkBox);
            if (!checkBoxes.containsKey(tags.get(i).getAsJsonObject().get("header").getAsString())) {
                checkBoxes.put(tags.get(i).getAsJsonObject().get("header").getAsString(), new ArrayList<>());
            }
            checkBoxes.get(tags.get(i).getAsJsonObject().get("header").getAsString()).add(checkBox);
        }
        clearButton = Button.builder(Component.nullToEmpty("Clear All"), button -> {
            browser.clearCategories();
            browser.setFilteredByGameVersion(true);
            browser.setEnvironment(null);
            this.repositionElements();
        }).build();
        
        clearButton.setSize(100, 20);
        this.addWidget(clearButton);
        if (projectType == ProjectType.RESOURCE_PACK) {
            checkBoxes.get("resolutions").sort((o1, o2) -> {
                String str1 = o1.getMessage().getString();
                String str2 = o2.getMessage().getString();
                return Integer.compare(Integer.parseInt(str1.substring(0, str1.indexOf('x'))), Integer.parseInt(str2.substring(0, str2.indexOf('x'))));
            });
        }
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        this.extractMenuBackground(context);
        clearButton.active = false;
        clearButton.setPosition(width - 115, 8);
        if (checkBoxes != null) {
            int i = 0;
            int offset = 10;
            for (String header : checkBoxes.sequencedKeySet()) {
                ArrayList<Checkbox> boxes = checkBoxes.get(header);
                context.pose().scale(1.4f, 1.4f);
                context.text(font, StringUtils.capitalize(header), (int)(20 /1.4f), (int) ((i * 25 + offset + scrollAmount)/1.4f), CommonColors.WHITE, true);
                context.pose().scale(1/1.4f,1/1.4f);
                offset += 20;
                for (Checkbox box : boxes) {
                    box.setPosition(20, i * 25 + offset + (int) scrollAmount);
                    box.extractRenderState(context, mouseX, mouseY, delta);
                    if (box.selected()) {
                        clearButton.active = true;
                    }
                    i++;
                }

            }
            maxY = i * 25 + offset;
        }

        if (browser.getProjectType() == ProjectType.MOD) {
            context.pose().scale(1.4f, 1.4f);
            context.text(font, "Environments", (int)(20/1.4f), (int) ((maxY + scrollAmount)/1.4f), CommonColors.WHITE, true);
            context.pose().scale(1/1.4f,1/1.4f);
            maxY += 20;
            requiresClientSide.setPosition(20, (int) (maxY + scrollAmount));
            requiresClientSide.extractRenderState(context, mouseX, mouseY, delta);
            maxY += 20;
            requiresServerSide.setPosition(20, (int) (maxY + scrollAmount));
            requiresServerSide.extractRenderState(context, mouseX, mouseY, delta);
            maxY += 25;
        }
        context.pose().scale(1.4f, 1.4f);
        context.text(font, "Game Version", (int)(20/1.4f), (int) ((maxY + scrollAmount)/1.4f), CommonColors.WHITE, true);
        context.pose().scale(1/1.4f,1/1.4f);
        disableGameVersionFilter.setPosition(20, (int) (maxY + scrollAmount + 20));
        maxY += 45;

        if (disableGameVersionFilter.selected() || requiresClientSide.selected() || requiresServerSide.selected()) {
            clearButton.active = true;
        }
        disableGameVersionFilter.extractRenderState(context, mouseX, mouseY, delta);
        doneButton.extractRenderState(context, mouseX, mouseY, delta);
        clearButton.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollAmount + verticalAmount * 13 <= 0 && scrollAmount + verticalAmount * 13 >= -20 - maxY + height) {
            scrollAmount += 13 * verticalAmount;
        } else if (scrollAmount + verticalAmount * 13 > 0) {
            scrollAmount = 0;
        } else if (scrollAmount + verticalAmount * 13 < -20 - maxY + height) {
            scrollAmount = - 20 - maxY + height;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

    }

}
