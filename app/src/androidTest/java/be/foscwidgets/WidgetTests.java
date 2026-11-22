package be.foscwidgets;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WidgetTests {
    private final Instrumentation ins=InstrumentationRegistry.getInstrumentation();
    private final Context ctx=ins.getTargetContext();
    private AppWidgetHost host;
    private int widgetId;
    @Before public void allocate() {
        ins.getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
        host=new AppWidgetHost(ctx,6812); widgetId=host.allocateAppWidgetId();
        assertTrue("Widget binding",AppWidgetManager.getInstance(ctx).bindAppWidgetIdIfAllowed(widgetId,new ComponentName(ctx,WidgetProvider.class)));
        ins.getUiAutomation().dropShellPermissionIdentity();
    }
    @After public void clean() { Store.delete(ctx,widgetId); host.deleteHost(); }
    private JSONObject seed() throws Exception {
        JSONObject d=Store.fresh(); d.put("title","Testwidget"); d.put("columns",2);
        Store.items(d).put(Store.item("note","Boodschappen","Melk"));
        Store.items(d).put(Store.item("web","Website","https://example.com"));
        Store.save(ctx,widgetId,d); return d;
    }
    private <T extends View> List<T> find(View v,Class<T> cls) {
        List<T> list=new ArrayList<>(); if(cls.isInstance(v)) list.add(cls.cast(v));
        if(v instanceof ViewGroup) { ViewGroup p=(ViewGroup)v; for(int i=0;i<p.getChildCount();i++) list.addAll(find(p.getChildAt(i),cls)); } return list;
    }
    private void click(Activity a,String title) {
        for(Button b:find(a.getWindow().getDecorView(),Button.class)) if(title.contentEquals(b.getText())) { b.performClick(); return; }
        fail("Button missing: "+title);
    }
    @Test public void independentWidgetsAndPersistence() throws Exception {
        JSONObject d=seed(); int second=host.allocateAppWidgetId();
        try { Store.save(ctx,second,Store.fresh()); assertEquals("Testwidget",Store.get(ctx,widgetId).optString("title")); assertEquals(0,Store.items(Store.get(ctx,second)).length());
            Store.delete(ctx,second); assertEquals(2,Store.items(Store.get(ctx,widgetId)).length());
            assertEquals(Store.items(d).getJSONObject(0).getString("id"),Store.items(Store.get(ctx,widgetId)).getJSONObject(0).getString("id"));
        } finally { Store.delete(ctx,second); host.deleteAppWidgetId(second); }
    }
    @Test public void allGridSizesInflateWithTransparentBackground() throws Exception {
        JSONObject d=seed();
        for(int i=2;i<12;i++) Store.items(d).put(Store.item("note","Notitie "+i,"Regel 1\nRegel 2"));
        for(int columns=1;columns<=4;columns++) {
            d.put("columns",columns); d.put("color","#81D4FA"); d.put("icons",columns%2==0); Store.save(ctx,widgetId,d);
            ins.runOnMainSync(() -> {
                View root=WidgetProvider.render(ctx,widgetId).apply(ctx,new FrameLayout(ctx));
                assertTrue(root.getBackground() instanceof ColorDrawable);
                assertEquals(Color.TRANSPARENT,((ColorDrawable)root.getBackground()).getColor());
                LinearLayout items=root.findViewById(R.id.items); int count=Store.get(ctx,widgetId).optInt("columns");
                assertEquals((12+count-1)/count,items.getChildCount());
                TextView label=root.findViewById(R.id.label); assertEquals(Color.parseColor("#81D4FA"),label.getCurrentTextColor());
                assertTrue(label.getText().toString().contains("Melk"));
            });
            WidgetProvider.update(ctx,widgetId);
        }
    }
    @Test public void configureSavesSettingsAndCancelKeepsOriginal() throws Exception {
        seed();
        Activity a=ins.startActivitySync(new Intent(ctx,ConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.runOnMainSync(() -> { List<EditText> fields=find(a.getWindow().getDecorView(),EditText.class); fields.get(0).setText("Thuis"); fields.get(1).setText("20"); fields.get(2).setText("#111111"); click(a,"Opslaan"); });
        ins.waitForIdleSync(); assertEquals("Thuis",Store.get(ctx,widgetId).optString("title")); assertEquals(20,Store.get(ctx,widgetId).optInt("size"));
        Activity b=ins.startActivitySync(new Intent(ctx,ConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.runOnMainSync(() -> { find(b.getWindow().getDecorView(),EditText.class).get(0).setText("Verwerpen"); click(b,"Annuleren"); }); ins.waitForIdleSync();
        assertEquals("Thuis",Store.get(ctx,widgetId).optString("title"));
    }
    @Test public void noteEditorUpdatesOnlyChosenNote() throws Exception {
        JSONObject data=seed(); String id=Store.items(data).getJSONObject(0).getString("id");
        Activity a=ins.startActivitySync(new Intent(ctx,OpenActivity.class).putExtra("widget",widgetId).putExtra("item",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.runOnMainSync(() -> { find(a.getWindow().getDecorView(),EditText.class).get(0).setText("Melk\nBrood"); click(a,"Opslaan en sluiten"); }); ins.waitForIdleSync();
        JSONArray items=Store.items(Store.get(ctx,widgetId)); assertEquals("Melk\nBrood",items.getJSONObject(0).getString("value")); assertEquals("https://example.com",items.getJSONObject(1).getString("value"));
    }
    @Test public void invalidColorDoesNotOverwriteWidget() throws Exception {
        seed(); Activity a=ins.startActivitySync(new Intent(ctx,ConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.runOnMainSync(() -> { List<EditText> fields=find(a.getWindow().getDecorView(),EditText.class); fields.get(2).setText("geen kleur"); click(a,"Opslaan"); assertNotNull(fields.get(2).getError()); assertFalse(a.isFinishing()); a.finish(); });
        assertEquals("#FFFFFF",Store.get(ctx,widgetId).optString("color"));
    }
    @Test public void removedWidgetCannotBeEdited() {
        Activity a=ins.startActivitySync(new Intent(ctx,ConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-123).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.waitForIdleSync(); assertTrue(a.isFinishing()||a.isDestroyed());
    }
    @Test public void compactProviderIsOneCellAndCanBeConfigured() throws Exception {
        int small=host.allocateAppWidgetId();
        try {
            ins.getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
            assertTrue(AppWidgetManager.getInstance(ctx).bindAppWidgetIdIfAllowed(small,new ComponentName(ctx,WidgetProvider.Compact.class)));
            ins.getUiAutomation().dropShellPermissionIdentity();
            AppWidgetProviderInfo info=AppWidgetManager.getInstance(ctx).getAppWidgetInfo(small);
            if(android.os.Build.VERSION.SDK_INT>=31) { assertEquals(1,info.targetCellWidth); assertEquals(1,info.targetCellHeight); }
            assertTrue(info.minResizeWidth<40*ctx.getResources().getDisplayMetrics().density);
            assertTrue(java.util.Arrays.stream(WidgetProvider.allIds(ctx)).anyMatch(id -> id==small));
            JSONObject d=Store.fresh(); Store.items(d).put(Store.item("note","Boodschappen","Melk")); Store.save(ctx,small,d);
            Activity a=ins.startActivitySync(new Intent(ctx,ConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,small).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            ins.runOnMainSync(() -> { assertFalse(a.isFinishing()); a.finish(); });
        } finally { Store.delete(ctx,small); host.deleteAppWidgetId(small); }
    }
    @Test public void oneCellLayoutStaysReadableAndExpandsAgain() throws Exception {
        seed(); android.os.Bundle options=new android.os.Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,60); options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,60);
        AppWidgetManager.getInstance(ctx).updateAppWidgetOptions(widgetId,options);
        ins.runOnMainSync(() -> {
            View root=WidgetProvider.render(ctx,widgetId).apply(ctx,new FrameLayout(ctx));
            int pixels=(int)(60*ctx.getResources().getDisplayMetrics().density);
            root.measure(View.MeasureSpec.makeMeasureSpec(pixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(pixels,View.MeasureSpec.EXACTLY)); root.layout(0,0,pixels,pixels);
            assertEquals(View.GONE,root.findViewById(R.id.edit).getVisibility());
            LinearLayout rows=root.findViewById(R.id.items); assertEquals(1,rows.getChildCount()); assertEquals(1,((LinearLayout)rows.getChildAt(0)).getChildCount());
            TextView label=root.findViewById(R.id.label); assertEquals("Boodschappen",label.getText().toString()); assertTrue(label.getHeight()>0); assertTrue(label.getWidth()>pixels/2);
        });
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,250); options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,200);
        AppWidgetManager.getInstance(ctx).updateAppWidgetOptions(widgetId,options);
        ins.runOnMainSync(() -> { View root=WidgetProvider.render(ctx,widgetId).apply(ctx,new FrameLayout(ctx)); assertEquals(View.VISIBLE,root.findViewById(R.id.edit).getVisibility()); assertEquals(2,((LinearLayout)((LinearLayout)root.findViewById(R.id.items)).getChildAt(0)).getChildCount()); assertTrue(((TextView)root.findViewById(R.id.label)).getText().toString().contains("Melk")); });
    }
    @Test public void samsungShortcutCannotTargetAnotherApp() {
        try { NoteShortcut.validate(ctx,new Intent(ctx,MainActivity.class)); fail("An unrelated app must not be accepted as a Samsung note"); } catch(SecurityException expected) { }
    }
    private Intent widgetOpenIntent(JSONObject data) throws Exception {
        return new Intent(ctx,OpenActivity.class).putExtra("widget",widgetId).putExtra("item",Store.items(data).getJSONObject(0).getString("id"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    }
    private boolean hasWidgetOpeningTask() {
        for(ActivityManager.AppTask task:ctx.getSystemService(ActivityManager.class).getAppTasks()) {
            ComponentName component=task.getTaskInfo().baseIntent.getComponent();
            if(component!=null && OpenActivity.class.getName().equals(component.getClassName())) return true;
        }
        return false;
    }
    private void assertOpeningTaskRemoved() {
        for(int n=0;n<50 && hasWidgetOpeningTask();n++) android.os.SystemClock.sleep(100);
        assertFalse("No temporary widget window remains in recent apps",hasWidgetOpeningTask());
    }
    @Test public void unprotectedShortcutOpensTargetAndRemovesTemporaryWindow() throws Exception {
        JSONObject data=Store.fresh(); Store.items(data).put(Store.item("app","App",new ComponentName(ctx,MainActivity.class).flattenToString())); Store.save(ctx,widgetId,data);
        Instrumentation.ActivityMonitor monitor=ins.addMonitor(MainActivity.class.getName(),null,false);
        Activity target=null;
        try {
            ins.startActivitySync(widgetOpenIntent(data));
            target=monitor.waitForActivityWithTimeout(6000); assertNotNull("The unprotected shortcut opens directly",target);
            assertOpeningTaskRemoved(); assertFalse(target.isFinishing());
        } finally { ins.removeMonitor(monitor); if(target!=null) { Activity main=target; ins.runOnMainSync(main::finish); } }
    }
    @Test public void pinAndBiometricShortcutsRemoveTemporaryWindowAfterUnlock() throws Exception {
        org.junit.Assume.assumeTrue(android.os.Build.MODEL.startsWith("sdk_gphone"));
        org.junit.Assume.assumeFalse(WidgetSecurity.deviceIsSecure(ctx));
        emulatorShell("locksettings set-pin 2580");
        try {
            for(int mode=1;mode<=2;mode++) {
                JSONObject data=Store.fresh(); data.put("lock",mode); Store.items(data).put(Store.item("app","App",new ComponentName(ctx,MainActivity.class).flattenToString())); Store.save(ctx,widgetId,data);
                Instrumentation.ActivityMonitor monitor=ins.addMonitor(MainActivity.class.getName(),null,false);
                Activity target=null,opening=null;
                try {
                    opening=ins.startActivitySync(widgetOpenIntent(data));
                    assertEquals("The target stays closed until authentication",0,monitor.getHits());
                    enterTestPin(); target=monitor.waitForActivityWithTimeout(6000); assertNotNull(target);
                    assertOpeningTaskRemoved(); assertFalse(target.isFinishing());
                } finally { ins.removeMonitor(monitor); if(opening!=null && !opening.isFinishing()) { Activity helper=opening; ins.runOnMainSync(helper::finish); } if(target!=null) { Activity main=target; ins.runOnMainSync(main::finish); } }
            }
        } finally { emulatorShell("locksettings clear --old 2580"); }
    }
    @Test public void protectedWidgetsNeverExposeNoteBodyOnHomeScreen() throws Exception {
        JSONObject data=seed();
        for(int mode=1;mode<=2;mode++) {
            data.put("lock",mode); Store.save(ctx,widgetId,data);
            ins.runOnMainSync(() -> {
                View root=WidgetProvider.render(ctx,widgetId).apply(ctx,new FrameLayout(ctx));
                for(TextView label:find(root,TextView.class)) assertFalse(label.getText().toString().contains("Melk"));
            });
        }
    }
    @Test public void protectedEntriesRejectForgedUnlockExtras() throws Exception {
        org.junit.Assume.assumeFalse(WidgetSecurity.deviceIsSecure(ctx));
        JSONObject data=seed(); data.put("lock",1); Store.save(ctx,widgetId,data);
        for(Class<?> screen:new Class<?>[]{OpenActivity.class,ConfigureActivity.class}) {
            Intent intent=new Intent(ctx,screen).putExtra("widget",widgetId).putExtra("item",Store.items(data).getJSONObject(0).getString("id"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId).putExtra("authenticated",true).putExtra("unlocked",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity a=ins.startActivitySync(intent); ins.waitForIdleSync();
            ins.runOnMainSync(() -> {
                assertEquals(0,find(a.getWindow().getDecorView(),EditText.class).size());
                assertTrue((a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);
                a.finish();
            });
        }
    }
    @Test public void enablingLockProtectsAnAlreadyOpenNoteWhenItReturns() throws Exception {
        org.junit.Assume.assumeFalse(WidgetSecurity.deviceIsSecure(ctx));
        JSONObject data=seed();
        Activity a=ins.startActivitySync(new Intent(ctx,OpenActivity.class).putExtra("widget",widgetId).putExtra("item",Store.items(data).getJSONObject(0).getString("id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.runOnMainSync(() -> { assertEquals(1,find(a.getWindow().getDecorView(),EditText.class).size()); ins.callActivityOnStop(a); });
        data.put("lock",1); Store.save(ctx,widgetId,data);
        ins.runOnMainSync(() -> {
            ins.callActivityOnResume(a);
            assertEquals(0,find(a.getWindow().getDecorView(),EditText.class).size());
            assertTrue((a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);
            a.finish();
        });
    }
    private void emulatorShell(String command) throws Exception {
        try(java.io.InputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.getUiAutomation().executeShellCommand(command))) { byte[] buffer=new byte[1024]; while(input.read(buffer)!=-1) { } }
    }
    private void enterTestPin() throws Exception {
        android.view.accessibility.AccessibilityNodeInfo field=credentialField();
        assertNotNull("Android PIN field is available",field);
        field.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS);
        field.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
        android.os.Bundle text=new android.os.Bundle(); text.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"2580");
        if(!field.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,text)) emulatorShell("input text 2580");
        emulatorShell("input keyevent 66");
    }
    private android.view.accessibility.AccessibilityNodeInfo credentialField() throws Exception {
        for(int attempt=0;attempt<60;attempt++) {
            android.view.accessibility.AccessibilityNodeInfo root=ins.getUiAutomation().getRootInActiveWindow();
            android.view.accessibility.AccessibilityNodeInfo field=findCredential(root);
            if(field!=null) return field;
            usePinAlternative(root);
            android.os.SystemClock.sleep(100);
        }
        return null;
    }
    private android.view.accessibility.AccessibilityNodeInfo findCredential(android.view.accessibility.AccessibilityNodeInfo node) {
        if(node==null) return null;
        if(node.isEditable() && !"be.foscwidgets".contentEquals(node.getPackageName())) return node;
        for(int n=0;n<node.getChildCount();n++) { android.view.accessibility.AccessibilityNodeInfo found=findCredential(node.getChild(n)); if(found!=null) return found; }
        return null;
    }
    private void usePinAlternative(android.view.accessibility.AccessibilityNodeInfo node) {
        if(node==null) return;
        CharSequence text=node.getText();
        if(text!=null && text.toString().matches("(?i)use pin|use screen lock|use password")) { node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK); return; }
        for(int n=0;n<node.getChildCount();n++) usePinAlternative(node.getChild(n));
    }
    @Test public void biometricModeCanUseDevicePinFallback() throws Exception {
        org.junit.Assume.assumeTrue(android.os.Build.MODEL.startsWith("sdk_gphone") && android.os.Build.VERSION.SDK_INT>=30);
        org.junit.Assume.assumeFalse(WidgetSecurity.deviceIsSecure(ctx));
        emulatorShell("locksettings set-pin 2580"); Activity a=null;
        try {
            JSONObject data=seed(); data.put("lock",2); Store.save(ctx,widgetId,data);
            a=ins.startActivitySync(new Intent(ctx,OpenActivity.class).putExtra("widget",widgetId).putExtra("item",Store.items(data).getJSONObject(0).getString("id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Activity opened=a;
            assertNotNull("Biometric authentication offers the device PIN",credentialField());
            enterTestPin();
            for(int n=0;n<60;n++) { final boolean[] ready={false}; ins.runOnMainSync(() -> ready[0]=!find(opened.getWindow().getDecorView(),EditText.class).isEmpty()); if(ready[0]) break; android.os.SystemClock.sleep(100); }
            ins.runOnMainSync(() -> { assertEquals("Melk",find(opened.getWindow().getDecorView(),EditText.class).get(0).getText().toString()); opened.finish(); });
        } finally { if(a!=null && !a.isFinishing()) { Activity target=a; ins.runOnMainSync(target::finish); } emulatorShell("locksettings clear --old 2580"); }
    }
    @Test public void devicePinUnlocksNoteAndBackgroundHidesItAgain() throws Exception {
        org.junit.Assume.assumeTrue(android.os.Build.MODEL.startsWith("sdk_gphone"));
        org.junit.Assume.assumeFalse(WidgetSecurity.deviceIsSecure(ctx));
        emulatorShell("locksettings set-pin 2580");
        Activity a=null;
        try {
            assertTrue(WidgetSecurity.deviceIsSecure(ctx));
            JSONObject data=seed(); data.put("lock",1); Store.save(ctx,widgetId,data);
            a=ins.startActivitySync(new Intent(ctx,OpenActivity.class).putExtra("widget",widgetId).putExtra("item",Store.items(data).getJSONObject(0).getString("id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Activity opened=a;
            ins.runOnMainSync(() -> assertEquals(0,find(opened.getWindow().getDecorView(),EditText.class).size()));
            assertNotNull("Android credential screen appears",credentialField());
            enterTestPin();
            for(int n=0;n<60;n++) { final boolean[] ready={false}; ins.runOnMainSync(() -> ready[0]=!find(opened.getWindow().getDecorView(),EditText.class).isEmpty()); if(ready[0]) break; android.os.SystemClock.sleep(100); }
            ins.runOnMainSync(() -> {
                assertEquals("Melk",find(opened.getWindow().getDecorView(),EditText.class).get(0).getText().toString());
                ins.callActivityOnStop(opened);
                assertEquals(0,find(opened.getWindow().getDecorView(),EditText.class).size());
                Store.delete(ctx,widgetId); ins.callActivityOnResume(opened);
                assertTrue("A removed protected widget cannot reopen cached content",opened.isFinishing());
                opened.finish();
            });
        } finally { if(a!=null && !a.isFinishing()) { Activity target=a; ins.runOnMainSync(target::finish); } emulatorShell("locksettings clear --old 2580"); }
    }
    @Test public void cancellingDevicePinDoesNotOpenNote() throws Exception {
        org.junit.Assume.assumeTrue(android.os.Build.MODEL.startsWith("sdk_gphone"));
        org.junit.Assume.assumeFalse(WidgetSecurity.deviceIsSecure(ctx));
        emulatorShell("locksettings set-pin 2580"); Activity a=null;
        try {
            JSONObject data=seed(); data.put("lock",1); Store.save(ctx,widgetId,data);
            a=ins.startActivitySync(new Intent(ctx,OpenActivity.class).putExtra("widget",widgetId).putExtra("item",Store.items(data).getJSONObject(0).getString("id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Activity opened=a;
            assertNotNull(credentialField()); emulatorShell("input keyevent 4");
            for(int n=0;n<10 && !opened.isFinishing() && !opened.isDestroyed();n++) android.os.SystemClock.sleep(100);
            if(!opened.isFinishing() && !opened.isDestroyed()) emulatorShell("input keyevent 4");
            for(int n=0;n<50 && !opened.isFinishing() && !opened.isDestroyed();n++) android.os.SystemClock.sleep(100);
            assertTrue("Cancel closes the protected widget",opened.isFinishing()||opened.isDestroyed());
        } finally { if(a!=null && !a.isFinishing()) { Activity target=a; ins.runOnMainSync(target::finish); } emulatorShell("locksettings clear --old 2580"); }
    }
    @Test public void samsungNoteIdentifierSurvivesSavingWithoutNoteCopy() throws Exception {
        String uuid="test-existing-note-123";
        Intent target=NoteShortcut.forUuid(uuid);
        JSONObject item=Store.item("shortcut","Werknotitie",target.toUri(0)); JSONObject data=Store.fresh(); Store.items(data).put(item); Store.save(ctx,widgetId,data);
        Intent restored=Intent.parseUri(Store.items(Store.get(ctx,widgetId)).getJSONObject(0).getString("value"),0);
        assertEquals(uuid,restored.getStringExtra("sdoc_uuid")); assertEquals("com.samsung.android.app.notes.ACTION_OPEN_MEMO",restored.getAction());
        assertEquals("com.samsung.android.app.notes",restored.getComponent().getPackageName());
        assertFalse(restored.hasExtra(Intent.EXTRA_TEXT));
        try { NoteShortcut.forUuid(""); fail("An empty identifier must be rejected"); } catch(IllegalArgumentException expected) { }
    }
    @Test public void createVisualWidgetPreview() throws Exception {
        JSONObject d=Store.fresh(); d.put("columns",2); d.put("size",18); d.put("edit",true);
        Store.items(d).put(Store.item("app","Mijn agenda","be.foscwidgets/.MainActivity"));
        Store.items(d).put(Store.item("web","Mijn website","https://example.com"));
        Store.items(d).put(Store.item("file","Documenten","content://example/document"));
        Store.items(d).put(Store.item("note","Niet vergeten","Melk en brood"));
        Store.save(ctx,widgetId,d);
        ins.runOnMainSync(() -> {
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(1000,700,android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);
            android.graphics.Paint paint=new android.graphics.Paint();
            paint.setShader(new android.graphics.LinearGradient(0,0,1000,700,new int[]{0xff173e4d,0xff236861,0xff456762},null,android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,1000,700,paint);
            View root=WidgetProvider.render(ctx,widgetId).apply(ctx,new FrameLayout(ctx));
            root.measure(View.MeasureSpec.makeMeasureSpec(860,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY)); root.layout(0,0,860,540);
            canvas.save(); canvas.translate(70,80); root.draw(canvas); canvas.restore();
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(ctx.getExternalFilesDir(null),"widget-preview.png"))) { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out)); }
            catch(java.io.IOException e) { throw new RuntimeException(e); }
        });
    }
}
