package be.foscwidgets;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

final class WidgetLaunchTasks {
    static void cleanPreviousTasks(Context context,int currentTask) {
        ActivityManager manager=context.getSystemService(ActivityManager.class);
        if(manager==null) return;
        for(ActivityManager.AppTask task:manager.getAppTasks()) {
            try {
                ActivityManager.RecentTaskInfo info=task.getTaskInfo();
                ComponentName base=info.baseIntent.getComponent();
                if(info.id==currentTask || base==null || !OpenActivity.class.getName().equals(base.getClassName()) || !context.getPackageName().equals(base.getPackageName())) continue;
                if(info.topActivity==null || context.getPackageName().equals(info.topActivity.getPackageName())) task.finishAndRemoveTask();
                else task.setExcludeFromRecents(true);
            } catch(IllegalArgumentException|SecurityException ignored) { }
        }
    }
}
