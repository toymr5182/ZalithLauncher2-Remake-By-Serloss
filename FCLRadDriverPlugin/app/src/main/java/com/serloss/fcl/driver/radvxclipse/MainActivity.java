package com.serloss.fcl.driver.radvxclipse;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        text.setTextSize(16);
        text.setPadding(48, 48, 48, 48);
        text.setGravity(Gravity.START);
        text.setText(
                "RADV Xclipse 540 — FCL driver plugin\n\n"
                        + "Vulkan driver: RADV Xclipse (Mesa 27.0.0-devel).\n"
                        + "GPU: Samsung Xclipse 540 / Exynos 1580.\n"
                        + "Requires Android 14 or newer (API 34).\n\n"
                        + "How to use in Fold Craft Launcher:\n"
                        + "1. Install this APK.\n"
                        + "2. Version settings -> Driver -> select \"RADV Xclipse 540 (RDNA, Mesa 27)\".\n"
                        + "3. Version settings -> Renderer -> select \"Zink\".\n"
                        + "4. Clear the shader cache after switching drivers.\n\n"
                        + "Warning: this driver is experimental. A GPU hang can freeze or reboot the phone. "
                        + "Switch back to the system Vulkan driver if games crash.\n\n"
                        + "Driver source: JimVulkan/radv-xclipse (MIT). Mesa licenses are in the APK assets."
        );

        ScrollView scroll = new ScrollView(this);
        scroll.addView(text);
        setContentView(scroll);
    }
}
