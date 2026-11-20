package be.foscwidgets;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.widget.*;
import org.json.*;
import java.util.*;

public class ConfigureActivity extends Activity {
    private int widgetId;
    private JSONObject data;
    private EditText title,color,size;
    private Spinner columns;
    private CheckBox icons,edit;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setResult(RESULT_CANCELED);
        widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        int[] ids=AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this,WidgetProvider.class));
        boolean valid=false; for(int id:ids) if(id==widgetId) valid=true;
        if(!valid) { finish(); return; }
        try { data=b!=null?new JSONObject(b.getString("draft")):Store.get(this,widgetId); } catch(Exception e) { data=Store.get(this,widgetId); }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle b) { capture(); b.putString("draft",data.toString()); super.onSaveInstanceState(b); }
    private void capture() {
        if(title==null) return;
        try {
            data.put("title",title.getText().toString().trim()); data.put("color",color.getText().toString().trim());
            int n; try { n=Integer.parseInt(size.getText().toString()); } catch(Exception e) { n=16; }
            data.put("size",Math.max(10,Math.min(28,n))); data.put("columns",columns.getSelectedItemPosition()+1); data.put("icons",icons.isChecked()); data.put("edit",edit.isChecked());
        } catch(JSONException e) { throw new IllegalStateException(e); }
    }
    private void render() {
        LinearLayout p=Ui.page(this,"Widget instellen");
        title=Ui.input(this,p,"Naam in de app",data.optString("title","Mijn widget"));
        Ui.text(this,p,"Snelkoppelingen · "+Store.items(data).length()+" / "+Store.LIMIT,20);
        JSONArray a=Store.items(data);
        for(int i=0;i<a.length();i++) {
            JSONObject o=a.optJSONObject(i); if(o==null) continue;
            final int index=i;
            String type=o.optString("type"); String kind="app".equals(type)?"App":"web".equals(type)?"Website":"file".equals(type)?"Bestand":"Notitie";
            Ui.button(this,p,(i+1)+". "+o.optString("label")+" · "+kind,v -> { capture(); itemMenu(index); });
        }
        Ui.button(this,p,"+ Snelkoppeling toevoegen",v -> {
            capture(); if(Store.items(data).length()>=Store.LIMIT) { Ui.toast(this,"Maximaal 12 snelkoppelingen per widget."); return; }
            new AlertDialog.Builder(this).setTitle("Wat wil je toevoegen?").setItems(new String[]{"App","Website","Bestand","Notitie"},(d,n) -> {
                if(n==0) chooseApp(); else if(n==1) editItem(-1,Store.item("web","","https://"));
                else if(n==2) chooseFile(); else editItem(-1,Store.item("note","",""));
            }).show();
        });
        Ui.text(this,p,"Uiterlijk",20);
        Ui.text(this,p,"De achtergrond is altijd volledig transparant.",15);
        Ui.text(this,p,"Aantal kolommen",14);
        columns=new Spinner(this); columns.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"1 kolom","2 kolommen","3 kolommen","4 kolommen"})); columns.setSelection(Math.max(0,Math.min(3,data.optInt("columns",2)-1))); p.addView(columns);
        size=Ui.input(this,p,"Tekstgrootte (10–28)",""+data.optInt("size",16)); size.setInputType(InputType.TYPE_CLASS_NUMBER);
        color=Ui.input(this,p,"Tekstkleur (#RRGGBB)",data.optString("color","#FFFFFF"));
        Ui.button(this,p,"Kleur kiezen",v -> new AlertDialog.Builder(this).setTitle("Tekstkleur").setItems(new String[]{"Wit","Zwart","Donkergroen","Geel","Lichtblauw"},(d,n) -> color.setText(new String[]{"#FFFFFF","#111111","#0D6B65","#FFE082","#81D4FA"}[n])).show());
        icons=new CheckBox(this); icons.setText("Pictogrammen tonen"); icons.setChecked(data.optBoolean("icons",false)); p.addView(icons);
        edit=new CheckBox(this); edit.setText("Link ‘Bewerken’ op de widget tonen"); edit.setChecked(data.optBoolean("edit",true)); p.addView(edit);
        Ui.text(this,p,"Tip: met 1–4 items begin je overzichtelijk. Vergroot de widget op je startscherm voor meer items of langere notities.",14);
        Ui.button(this,p,"Opslaan",v -> save());
        Ui.button(this,p,"Annuleren",v -> finish());
    }
    private void save() {
        capture();
        String c=data.optString("color");
        if(!c.matches("#[0-9a-fA-F]{6}")) { color.setError("Gebruik bijvoorbeeld #FFFFFF"); return; }
        if(Store.items(data).length()==0) { Ui.toast(this,"Voeg eerst minstens één snelkoppeling toe."); return; }
        try {
            if(data.optString("title").isEmpty()) data.put("title","Mijn widget");
            Store.save(this,widgetId,data); WidgetProvider.update(this,widgetId);
            setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId)); finish();
        } catch(Exception e) { Ui.toast(this,"Opslaan is mislukt. Probeer opnieuw."); }
    }
    private void itemMenu(int index) {
        new AlertDialog.Builder(this).setTitle(Store.items(data).optJSONObject(index).optString("label"))
            .setItems(new String[]{"Bewerken","Omhoog","Omlaag","Verwijderen"},(d,n) -> {
                if(n==0) editItem(index,Store.items(data).optJSONObject(index));
                else if(n==3) new AlertDialog.Builder(this).setMessage("Deze snelkoppeling uit de widget verwijderen?").setPositiveButton("Verwijderen",(x,y) -> { Store.items(data).remove(index); render(); }).setNegativeButton("Annuleren",null).show();
                else { int target=n==1?index-1:index+1; JSONArray a=Store.items(data); if(target>=0 && target<a.length()) try { Object temp=a.get(index); a.put(index,a.get(target)); a.put(target,temp); } catch(JSONException ignored) { } render(); }
            }).show();
    }
    private void editItem(int index,JSONObject original) {
        LinearLayout p=new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setPadding(Ui.dp(this,20),0,Ui.dp(this,20),0);
        EditText label=Ui.input(this,p,"Naam",original.optString("label"));
        String type=original.optString("type");
        EditText value=null;
        if("web".equals(type)||"note".equals(type)) {
            value=Ui.input(this,p,"web".equals(type)?"Websiteadres":"Tekst",original.optString("value"));
            if("note".equals(type)) { value.setSingleLine(false); value.setMinLines(4); value.setMaxLines(8); value.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); }
            else value.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        }
        final EditText val=value;
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Snelkoppeling").setView(p).setPositiveButton("Bewaren",null).setNegativeButton("Annuleren",null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name=label.getText().toString().trim(); if(name.isEmpty()) { label.setError("Vul een naam in"); return; }
            String target=val==null?original.optString("value"):val.getText().toString();
            if("web".equals(type)) {
                target=target.trim(); if(!target.contains(":")) target="https://"+target;
                Uri u=Uri.parse(target); if(!("https".equals(u.getScheme())||"http".equals(u.getScheme())) || u.getHost()==null || u.getHost().isEmpty()) { val.setError("Vul een geldig http- of https-adres in"); return; }
            }
            if("note".equals(type) && target.length()>10000) { val.setError("Maximaal 10.000 tekens"); return; }
            try {
                JSONObject item=new JSONObject(original.toString()); item.put("label",name); item.put("value",target);
                JSONArray a=Store.items(data); if(index<0) a.put(item); else a.put(index,item); data.put("items",a);
            } catch(JSONException e) { throw new IllegalStateException(e); }
            dialog.dismiss(); render();
        })); dialog.show();
    }
    private void chooseApp() {
        Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps=getPackageManager().queryIntentActivities(intent,0);
        apps.sort((a,b) -> a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
        LinearLayout p=new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setPadding(Ui.dp(this,20),0,Ui.dp(this,20),0);
        EditText search=Ui.input(this,p,"Zoeken",""); ListView list=new ListView(this); p.addView(list,new LinearLayout.LayoutParams(-1,Ui.dp(this,350)));
        ArrayList<ResolveInfo> filtered=new ArrayList<>(apps);
        Runnable refresh=() -> { filtered.clear(); ArrayList<String> names=new ArrayList<>(); String q=search.getText().toString().toLowerCase(Locale.ROOT); for(ResolveInfo r:apps) { String name=r.loadLabel(getPackageManager()).toString(); if(name.toLowerCase(Locale.ROOT).contains(q)) { filtered.add(r); names.add(name); } } list.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,names)); };
        search.addTextChangedListener(new android.text.TextWatcher() { public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){refresh.run();} public void afterTextChanged(android.text.Editable e){} }); refresh.run();
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("App kiezen").setView(p).setNegativeButton("Annuleren",null).create();
        list.setOnItemClickListener((parent,v,pos,id) -> { ResolveInfo r=filtered.get(pos); dialog.dismiss(); editItem(-1,Store.item("app",r.loadLabel(getPackageManager()).toString(),new ComponentName(r.activityInfo.packageName,r.activityInfo.name).flattenToString())); }); dialog.show();
    }
    private void chooseFile() {
        try { startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),40); }
        catch(ActivityNotFoundException e) { Ui.toast(this,"Er is geen bestandskiezer beschikbaar."); }
    }
    @Override protected void onActivityResult(int request,int result,Intent intent) {
        super.onActivityResult(request,result,intent);
        if(request!=40 || result!=RESULT_OK || intent==null || intent.getData()==null) return;
        Uri uri=intent.getData();
        try { getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch(SecurityException e) { Ui.toast(this,"Dit bestand kan niet blijvend worden gekoppeld. Kies een andere locatie."); return; }
        String name="Bestand";
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)) { if(c!=null && c.moveToFirst()) name=c.getString(0); } catch(Exception ignored) { }
        editItem(-1,Store.item("file",name,uri.toString()));
    }
}
