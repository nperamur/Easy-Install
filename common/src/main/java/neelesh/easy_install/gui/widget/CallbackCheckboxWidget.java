package neelesh.easy_install.gui.widget;

import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

public class CallbackCheckboxWidget extends CheckboxWidget {
    private CheckboxCallbackInterface callback;

    public CallbackCheckboxWidget(int x, int y, Text message, boolean checked, CheckboxCallbackInterface callback) {
        super(x, y, 20, 20, message, checked);
        this.callback = callback;
    }

    @Override
    public void onPress() {
        super.onPress();
        callback.onPress(this, isChecked());
    }

    public void setCallback(CheckboxCallbackInterface callback) {
        this.callback = callback;
    }
}
