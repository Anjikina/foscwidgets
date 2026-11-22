package be.foscwidgets;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;

public class WidgetProvider extends AppWidgetProvider {
    public static class Compact extends WidgetProvider { }
    public static class Wide extends WidgetProvider { }
    public static class Square extends WidgetProvider { }
    public static class Large extends WidgetProvider { }
    static final Class<?>[] TYPES={Compact.class,Wide.class,Square.class,WidgetProvider.class,Large.class};
    static int[] allIds(Context c) {
        java.util.ArrayList<Integer> result=new java.util.ArrayList<>();
        for(Class<?> type:TYPES) for(int id:AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,type))) result.add(id);
        int[] ids=new int[result.size()]; for(int n=0;n<ids.length;n++) ids[n]=result.get(n); return ids;
    }
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for(int id:ids) update(c,id); }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, android.os.Bundle b) { update(c,id); }
    @Override public void onDeleted(Context c,int[] ids) { for(int id:ids) Store.delete(c,id); }
    static PendingIntent config(Context c,int id) {
        Intent i = new Intent(c,ConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).setData(Uri.parse("foscwidgets://configure/"+id));
        return PendingIntent.getActivity(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void update(Context c,int id) {
        AppWidgetManager.getInstance(c).updateAppWidget(id,render(c,id));
    }
    static RemoteViews render(Context c,int id) {
        JSONObject o = Store.get(c,id); JSONArray a = Store.items(o);
        boolean locked=WidgetSecurity.mode(o)!=0;
        RemoteViews root = new RemoteViews(c.getPackageName(),R.layout.widget); root.removeAllViews(R.id.items);
        int columns = Math.max(1,Math.min(4,o.optInt("columns",1)));
        android.os.Bundle options=AppWidgetManager.getInstance(c).getAppWidgetOptions(id);
        int width=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,1000);
        int height=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,1000);
        boolean compact=width<100 || height<95;
        boolean showEdit=o.optBoolean("edit",true) && !compact;
        columns=Math.min(columns,Math.max(1,width/65));
        int color; try { color=Color.parseColor(o.optString("color","#FFFFFF")); } catch(Exception e) { color=Color.WHITE; }
        int total = Math.min(Store.LIMIT,a.length());
        int availableHeight=Math.max(1,height-(showEdit?32:0));
        int visibleRows=Math.max(1,availableHeight/52);
        total=Math.min(total,columns*visibleRows);
        columns=Math.min(columns,Math.max(1,total));
        if(total==0) {
            RemoteViews cell=new RemoteViews(c.getPackageName(),R.layout.cell); cell.setTextViewText(R.id.label,"Tik om in te stellen"); cell.setTextColor(R.id.label,color); cell.setViewVisibility(R.id.icon,View.GONE); cell.setOnClickPendingIntent(R.id.cell,config(c,id));
            RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.row); row.addView(R.id.row,cell); root.addView(R.id.items,row);
        }
        for(int start=0;start<total;start+=columns) {
            RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.row);
            for(int n=start;n<start+columns;n++) {
                RemoteViews cell=new RemoteViews(c.getPackageName(),R.layout.cell);
                JSONObject item=a.optJSONObject(n);
                if(n>=total || item==null) { cell.setViewVisibility(R.id.cell,View.INVISIBLE); row.addView(R.id.row,cell); continue; }
                String label=item.optString("label");
                if(!locked && (!compact || label.isEmpty()) && "note".equals(item.optString("type")) && !item.optString("value").isEmpty()) label += (label.isEmpty()?"":"\n")+item.optString("value");
                cell.setTextViewText(R.id.label,label); cell.setTextColor(R.id.label,color); cell.setTextViewTextSize(R.id.label,android.util.TypedValue.COMPLEX_UNIT_SP,Math.max(10,Math.min(28,o.optInt("size",16))));
                cell.setViewVisibility(R.id.label,label.isEmpty()?View.GONE:View.VISIBLE);
                cell.setContentDescription(R.id.cell,item.optString("label").isEmpty()?("note".equals(item.optString("type"))||"shortcut".equals(item.optString("type"))?"Notitie openen":"Snelkoppeling openen"):item.optString("label"));
                cell.setInt(R.id.label,"setMaxLines",compact?2:4);
                boolean icons=o.optBoolean("icons",false) && availableHeight/Math.max(1,(total+columns-1)/columns)>=70; cell.setViewVisibility(R.id.icon,icons?View.VISIBLE:View.GONE);
                if(icons) {
                    Bitmap bitmap=null;
                    if("app".equals(item.optString("type"))) try {
                        ComponentName component=ComponentName.unflattenFromString(item.optString("value"));
                        Drawable icon=c.getPackageManager().getActivityIcon(component); bitmap=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888); Canvas canvas=new Canvas(bitmap); icon.setBounds(0,0,80,80); icon.draw(canvas);
                    } catch(Exception ignored) { }
                    if(bitmap!=null) cell.setImageViewBitmap(R.id.icon,bitmap); else cell.setImageViewResource(R.id.icon,"note".equals(item.optString("type"))?android.R.drawable.ic_menu_edit:"file".equals(item.optString("type"))?android.R.drawable.ic_menu_save:android.R.drawable.ic_menu_view);
                }
                Intent i=new Intent(c,OpenActivity.class).putExtra("widget",id).putExtra("item",item.optString("id")).setData(Uri.parse("foscwidgets://open/"+id+"/"+item.optString("id")));
                cell.setOnClickPendingIntent(R.id.cell,PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
                row.addView(R.id.row,cell);
            }
            root.addView(R.id.items,row);
        }
        root.setViewVisibility(R.id.edit,showEdit?View.VISIBLE:View.GONE); root.setTextColor(R.id.edit,color); root.setOnClickPendingIntent(R.id.edit,config(c,id));
        return root;
    }
}
