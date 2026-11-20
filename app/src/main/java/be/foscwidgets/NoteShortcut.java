package be.foscwidgets;

import android.content.*;
import android.content.pm.ResolveInfo;

final class NoteShortcut {
    static Intent validate(Context context,Intent original) {
        Intent intent=new Intent(original);
        intent.setSelector(null); intent.setClipData(null);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.samsung.android.app.notes");
        ComponentName component=intent.getComponent();
        if(component!=null && !"com.samsung.android.app.notes".equals(component.getPackageName())) throw new SecurityException("Wrong shortcut package");
        ResolveInfo resolved=context.getPackageManager().resolveActivity(intent,0);
        if(resolved==null || !resolved.activityInfo.exported || !resolved.activityInfo.enabled || !"com.samsung.android.app.notes".equals(resolved.activityInfo.packageName)) throw new SecurityException("Unavailable note shortcut");
        intent.setComponent(new ComponentName(resolved.activityInfo.packageName,resolved.activityInfo.name));
        return intent;
    }
}
