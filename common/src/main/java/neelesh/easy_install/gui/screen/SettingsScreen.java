package neelesh.easy_install.gui.screen;

import neelesh.easy_install.EasyInstallClient;
import neelesh.easy_install.ProjectType;
import neelesh.easy_install.gui.widget.VersionPickerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;

public class SettingsScreen extends Screen {
    private VersionPickerWidget versionPickerWidget;

    private Button versionSaveButton;
    private Button versionCancelButton;
    private Button doneButton;

    private Screen parent;
    private ProjectType projectType;
    private Button clearButton;


    protected SettingsScreen(Screen parent, ProjectType projectType) {
        super(Component.nullToEmpty("Settings Screen"));
        this.versionPickerWidget = new VersionPickerWidget(this, Minecraft.getInstance(), 180, 60, 70, 15);

        Thread thread = new Thread(() -> {
            ArrayList<String> versions = EasyInstallClient.getReleaseVersionNumbers();
            Minecraft.getInstance().execute(() -> {
                for (String version : versions) {
                    VersionPickerWidget.Entry entry = versionPickerWidget.addEntry(version);
                    if (version.equals(EasyInstallClient.getCurrentTargetUpdateVersion())) {
                        versionPickerWidget.setSelected(entry);
                        versionPickerWidget.setFocused(entry);
                    }
                }
                versionPickerWidget.refreshScrollAmount();
            });
        });
        thread.start();

        this.parent = parent;
        this.projectType = projectType;
    }


    @Override
    protected void init() {
        super.init();
        versionPickerWidget.setX(10);
        //versionPickerWidget.active = false;
        this.versionSaveButton = Button.builder(Component.nullToEmpty("Save"), button -> {
            if (versionPickerWidget.getFocused() != null) {
                String version = (versionPickerWidget.getFocused()).getNarration().getString();
                if (!(versionPickerWidget.getFocused()).getNarration().getString().equals(EasyInstallClient.getGameVersion())) {
                    ConfirmScreen confirmScreen = new ConfirmScreen(bool -> {
                        if (bool) {
                            setTargetUpdateVersion(version, projectType);
                            minecraft.gui.setScreen(this);
                        } else {
                            minecraft.gui.setScreen(this);
                        }
                    }, Component.nullToEmpty("Are you sure you want to change the target update version?"), Component.literal("This could make this mod scan for updates to versions incompatible with this Minecraft instance.").withColor(0xFFA500));
                    minecraft.gui.setScreen(confirmScreen);
                } else {
                    this.setTargetUpdateVersion(version, projectType);
                }
            }
        }).build();

        clearButton = Button.builder(Component.nullToEmpty("Clear All"), button -> {
            this.setTargetUpdateVersion(EasyInstallClient.getGameVersion(), projectType);
            this.repositionElements();
        }).build();
        clearButton.setSize(100, 20);
        this.addWidget(clearButton);

        this.versionCancelButton = Button.builder(Component.nullToEmpty("Cancel"), button -> {
            ArrayList<VersionPickerWidget.Entry> entries = versionPickerWidget.getEntries();
            for (VersionPickerWidget.Entry entry : entries) {
                if (entry.getNarration().getString().equals(EasyInstallClient.getCurrentTargetUpdateVersion())) {
                    versionPickerWidget.setSelected(entry);
                    versionPickerWidget.setFocused(entry);
                }
            }
        }).build();

        this.doneButton = Button.builder(Component.nullToEmpty("Done"), button -> {
            Minecraft.getInstance().gui.setScreen(parent);
        }).build();


        ArrayList<VersionPickerWidget.Entry> entries = versionPickerWidget.getEntries();
        for (VersionPickerWidget.Entry entry : entries) {
            if (entry.getNarration().getString().equals(EasyInstallClient.getCurrentTargetUpdateVersion())) {
                versionPickerWidget.setSelected(entry);
                versionPickerWidget.setFocused(entry);
            }
        }


        versionSaveButton.setSize(40, 20);
        versionSaveButton.setPosition(60, 140);

        versionCancelButton.setSize(40, 20);
        versionCancelButton.setPosition(10, 140);


        this.addWidget(versionPickerWidget);
        this.addWidget(versionSaveButton);
        this.addWidget(versionCancelButton);
        this.addWidget(doneButton);


    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        context.text(font, Component.literal("Settings"), this.width / 2 - font.width("Settings") / 2, 10, CommonColors.WHITE, true);


        context.text(font, Component.nullToEmpty("Scanning for updates to Minecraft version: " + EasyInstallClient.getCurrentTargetUpdateVersion()), 10, 40, CommonColors.WHITE, true);



        context.text(font, Component.nullToEmpty("Configure target update version"), 10, 55, CommonColors.WHITE, false);


        if (versionPickerWidget.getFocused() != null) {
            versionSaveButton.active = !EasyInstallClient.getCurrentTargetUpdateVersion().equals(versionPickerWidget.getFocused().getNarration().getString());
            versionCancelButton.active = !EasyInstallClient.getCurrentTargetUpdateVersion().equals(versionPickerWidget.getFocused().getNarration().getString());
        } else {
            versionSaveButton.active = false;
            versionCancelButton.active = false;
        }

        versionPickerWidget.extractRenderState(context, mouseX, mouseY, deltaTicks);
        versionSaveButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
        versionCancelButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
        doneButton.setPosition(width / 2 - doneButton.getWidth() / 2, height - 25);
        doneButton.extractRenderState(context, mouseX, mouseY, deltaTicks);

        clearButton.active = !EasyInstallClient.getCurrentTargetUpdateVersion().equals(EasyInstallClient.getGameVersion());
        clearButton.setPosition(width - 115, 8);
        clearButton.extractRenderState(context, mouseX, mouseY, deltaTicks);


    }

    public void setTargetUpdateVersion(String version, ProjectType projectType) {
        EasyInstallClient.setCurrentTargetUpdateVersion(version);
        Thread thread = new Thread(() -> {
            EasyInstallClient.checkStatus(projectType);
        });
        thread.start();
    }
}
