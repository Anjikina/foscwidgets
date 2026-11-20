package be.foscwidgets;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import org.json.*;

public class OpenActivity extends Activity {
    private int widgetId;
    private String itemId;
    private EditText note;
    private String savedText;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b); widgetId=getIntent().getIntExtra("widget",-1); itemId=getIntent().getStringExtra("item");
        if(!Store.exists(this,widgetId)) { finish(); return; }
        JSONObject item=find(Store.get(this,widgetId)); if(item==null) { finish(); return; }
        String type=item.optString("type"),value=item.optString("value");
        if("note".equals(type)) {
            LinearLayout p=Ui.page(this,item.optString("label"));
            Ui.text(this,p,"Je wijzigingen worden automatisch opgeslagen.",14);
            savedText=value; note=Ui.input(this,p,"Notitie",value); note.setSingleLine(false); note.setMinLines(8); note.setGravity(android.view.Gravity.TOP); note.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); note.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(10000)});
            if(b!=null) note.setText(b.getString("draft",value));
            Ui.button(this,p,"Opslaan en sluiten",v -> { if(saveNote()) finish(); });
            return;
        }
        try {
            Intent launch;
            if("app".equals(type)) launch=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ComponentName.unflattenFromString(value)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            else if("shortcut".equals(type)) launch=NoteShortcut.validate(this,Intent.parseUri(value,0));
            else if("file".equals(type)) {
                Uri uri=Uri.parse(value); String mime=getContentResolver().getType(uri);
                launch=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime==null?"*/*":mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                launch.setClipData(ClipData.newRawUri("Bestand",uri));
            } else launch=new Intent(Intent.ACTION_VIEW,Uri.parse(value));
            startActivity(launch); finish();
        } catch(Exception e) {
            new AlertDialog.Builder(this).setTitle("Kan niet openen").setMessage("De app of het bestand is niet meer beschikbaar, of er is geen geschikte app geïnstalleerd. Pas de snelkoppeling aan via foscwidgets.").setPositiveButton("Sluiten",(d,w) -> finish()).setOnCancelListener(d -> finish()).show();
        }
    }
    private JSONObject find(JSONObject data) {
        JSONArray a=Store.items(data); for(int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if(o!=null && o.optString("id").equals(itemId)) return o; } return null;
    }
    private boolean saveNote() {
        if(note==null || note.getText().toString().equals(savedText)) return true;
        try { JSONObject data=Store.get(this,widgetId); JSONObject item=find(data); if(item==null) return true; String value=note.getText().toString(); item.put("value",value); Store.save(this,widgetId,data); savedText=value; WidgetProvider.update(this,widgetId); return true; }
        catch(Exception e) { Ui.toast(this,"De notitie kon niet worden opgeslagen."); return false; }
    }
    @Override protected void onPause() { saveNote(); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle b) { if(note!=null) b.putString("draft",note.getText().toString()); super.onSaveInstanceState(b); }
}
