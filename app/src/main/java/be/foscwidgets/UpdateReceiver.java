package be.foscwidgets;

import android.content.*;

public class UpdateReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        WidgetLaunchTasks.cleanPreviousTasks(context,-1);
        for(int id:WidgetProvider.allIds(context)) WidgetProvider.update(context,id);
    }
}
