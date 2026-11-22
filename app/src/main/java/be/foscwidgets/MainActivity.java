package be.foscwidgets;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.app.AlertDialog;
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
        Ui.button(this,page,"Widget toevoegen",v -> new AlertDialog.Builder(this).setTitle("Kies het widgetformaat").setItems(new String[]{"1×1 · compact","2×1 · breed","2×2 · vierkant","3×2 · standaard","4×2 · groot"},(dialog,index) -> addWidget(WidgetProvider.TYPES[index])).show());
        int[] ids=WidgetProvider.allIds(this);
        Ui.text(this,page,"Mijn widgets",21);
        if(ids.length==0) Ui.text(this,page,"Nog geen widgets. Kies hierboven een formaat, of houd een lege plek op je startscherm ingedrukt en kies Widgets → foscwidgets.",16);
        for(int n=0;n<ids.length;n++) {
            int id=ids[n]; String title=Store.get(this,id).optString("title");
            Ui.button(this,page,(title.isEmpty()?"Widget "+(n+1):title)+" · bewerken",v -> startActivity(new Intent(this,ConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id)));
        }
        Ui.text(this,page,"Tips",21);
        Ui.text(this,page,"• Kies 1×1 voor één snelkoppeling.\n• Houd een widget ingedrukt en sleep de randen om het formaat te wijzigen.\n• Op een kleine widget wordt ‘Bewerken’ automatisch verborgen; aanpassen kan via deze app.\n• Kies Samsung Notes om een bestaande Samsung-notitie te koppelen.\n• Bestanden blijven op hun oorspronkelijke plek. Na verwijderen of verplaatsen moet je ze mogelijk opnieuw kiezen.\n\nfoscwidgets 0.3.1 · Geen account of abonnement nodig.",15);
    }
    private void addWidget(Class<?> type) {
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
                if(!m.requestPinAppWidget(new ComponentName(this,type),null,callback)) Ui.toast(this,"Voeg de widget toe via een lege plek op je startscherm → Widgets → foscwidgets.");
            }
            else Ui.toast(this,"Houd een lege plek op je startscherm ingedrukt, kies Widgets en daarna foscwidgets.");
    }
}
