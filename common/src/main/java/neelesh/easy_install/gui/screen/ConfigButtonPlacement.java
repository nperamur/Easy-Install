package neelesh.easy_install.gui.screen;

public enum ConfigButtonPlacement {
    FOOTER("Footer"),
    IN_BUTTON_ROW("Icon");

    String type;

    ConfigButtonPlacement(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ConfigButtonPlacement getDefaultConfigButtonPlacement() {
        return ConfigButtonPlacement.FOOTER;
    }

    public static ConfigButtonPlacement fromType(String type) {
        for (ConfigButtonPlacement placement : values()) {
            if (placement.type.equals(type)) {
                return placement;
            }
        }
        return getDefaultConfigButtonPlacement();
    }
}
