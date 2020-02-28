package vn.fgc.doorremote.ultilities;

import android.app.Activity;

public class ThreadHelper {
    public static void run(boolean runOnUi, Activity activity, Runnable runnable) {
        if (runOnUi) {
            activity.runOnUiThread(runnable);
        } else {
            runnable.run();
        }
    }
}
