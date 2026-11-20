package be.foscwidgets;

import android.content.*;
import android.content.pm.ResolveInfo;

final class NoteShortcut {
    static Intent forUuid(String uuid) {
        if(uuid==null || uuid.trim().isEmpty() || uuid.length()>512) throw new IllegalArgumentException("Missing note identifier");
        return new Intent("com.samsung.android.app.notes.ACTION_OPEN_MEMO")
            .setComponent(new ComponentName("com.samsung.android.app.notes","com.samsung.android.support.senl.nt.app.trigger.DocOpenTriggerActivity"))
            .putExtra("sdoc_uuid",uuid);
    }
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
