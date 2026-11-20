package be.foscwidgets;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.Bundle;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) { super.onCreate(b); }
    @Override public void onResume() { super.onResume(); show(); }
    private void show() {
        LinearLayout page=Ui.page(this,"foscwidgets");
        Ui.text(this,page,"Jouw snelkoppelingen. Jouw achtergrond.",19);
        Ui.text(this,page,"Maak volledig transparante widgets met apps, websites, bestanden en notities. Alleen de tekst en eventuele pictogrammen zijn zichtbaar op je startscherm.",16);
        Ui.button(this,page,"Widget toevoegen",v -> {
            AppWidgetManager m=AppWidgetManager.getInstance(this);
            if(m.isRequestPinAppWidgetSupported()) {
                Intent configure=new Intent(this,ConfigureActivity.class).setData(android.net.Uri.parse("foscwidgets://pinned/"+System.nanoTime()));
                Bundle options=null;
                if(android.os.Build.VERSION.SDK_INT>=35) {
                    ActivityOptions activityOptions=ActivityOptions.makeBasic();
                    activityOptions.setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                    options=activityOptions.toBundle();
                }
                PendingIntent callback=PendingIntent.getActivity(this,0,configure,PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_MUTABLE,options);
                if(!m.requestPinAppWidget(new ComponentName(this,WidgetProvider.class),null,callback)) Ui.toast(this,"Voeg de widget toe via een lege plek op je startscherm → Widgets → foscwidgets.");
            }
            else Ui.toast(this,"Houd een lege plek op je startscherm ingedrukt, kies Widgets en daarna foscwidgets.");
        });
        int[] ids=AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this,WidgetProvider.class));
        Ui.text(this,page,"Mijn widgets",21);
        if(ids.length==0) Ui.text(this,page,"Nog geen widgets. Voeg er hierboven een toe of houd een lege plek op je startscherm ingedrukt en kies Widgets → foscwidgets.",16);
        for(int id:ids) Ui.button(this,page,Store.get(this,id).optString("title","Mijn widget")+" · bewerken",v -> startActivity(new Intent(this,ConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id)));
        Ui.text(this,page,"Tips",21);
        Ui.text(this,page,"• Vergroot een widget op je startscherm als tekst wordt afgekapt.\n• Tik op een notitie om de tekst te wijzigen.\n• Verberg ‘Bewerken’ voor een rustig startscherm; je kunt de widget altijd via deze app aanpassen.\n• Bestanden blijven op hun oorspronkelijke plek. Na verwijderen of verplaatsen moet je ze mogelijk opnieuw kiezen.\n\nfoscwidgets 0.1.0 · Geen account of abonnement nodig.",15);
    }
}
