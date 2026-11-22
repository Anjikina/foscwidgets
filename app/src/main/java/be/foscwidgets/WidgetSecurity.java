package be.foscwidgets;

import android.app.KeyguardManager;
import android.content.Context;
import org.json.JSONObject;

final class WidgetSecurity {
    static int mode(JSONObject data) { return Math.max(0,Math.min(2,data.optInt("lock",0))); }
    static boolean isLocked(Context context,int id) { return mode(Store.get(context,id))!=0; }
    static boolean deviceIsSecure(Context context) {
        KeyguardManager manager=context.getSystemService(KeyguardManager.class);
        return manager!=null && manager.isDeviceSecure();
    }
}
