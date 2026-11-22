package be.foscwidgets;

import android.app.*;
import android.content.Intent;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.*;
import android.view.*;
import android.widget.LinearLayout;

/** Keeps widget content behind the operating system's authentication surface. */
public abstract class ProtectedWidgetActivity extends Activity {
    private static final int AUTH_REQUEST=9081;
    private int guardedWidgetId=-1;
    private boolean protectionStarted,unlocked,authRunning,requiresExistingWidget;
    private int grantedMode;
    private Runnable firstDisplay,deferredAction;
    private View hiddenContent;
    private CancellationSignal cancellation;

    protected final void protectWidget(int id,Runnable display) {
        guardedWidgetId=id; protectionStarted=true; firstDisplay=display;
        requiresExistingWidget=WidgetSecurity.isLocked(this,id);
        if(!requiresExistingWidget) { allowDisplay(); return; }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        showLockedPage();
    }
    protected final void whenUnlocked(Runnable action) {
        if(unlocked) action.run(); else deferredAction=action;
    }
    protected void showLockedPage() {
        LinearLayout page=Ui.page(this,"Widget vergrendeld");
        Ui.text(this,page,"Ontgrendel met de beveiliging van je telefoon om verder te gaan.",16);
        Ui.button(this,page,"Sluiten",v -> finish());
    }
    private void allowDisplay() {
        if(isFinishing()||isDestroyed()) return;
        if(requiresExistingWidget && !Store.exists(this,guardedWidgetId)) { finish(); return; }
        authRunning=false; unlocked=true;
        grantedMode=WidgetSecurity.mode(Store.get(this,guardedWidgetId));
        if(firstDisplay!=null) { Runnable display=firstDisplay; firstDisplay=null; display.run(); }
        else if(hiddenContent!=null) { View content=hiddenContent; hiddenContent=null; setContentView(content); }
        if(deferredAction!=null) { Runnable action=deferredAction; deferredAction=null; action.run(); }
    }
    @Override protected void onResume() {
        super.onResume();
        if(protectionStarted && requiresExistingWidget && !Store.exists(this,guardedWidgetId)) { finish(); return; }
        if(protectionStarted && unlocked && grantedMode==0 && WidgetSecurity.isLocked(this,guardedWidgetId)) hideContent();
        if(protectionStarted && !unlocked && !authRunning) authenticate();
    }
    private void authenticate() {
        if(requiresExistingWidget && !Store.exists(this,guardedWidgetId)) { finish(); return; }
        int mode=WidgetSecurity.mode(Store.get(this,guardedWidgetId));
        if(mode==0) { allowDisplay(); return; }
        if(!WidgetSecurity.deviceIsSecure(this)) {
            LinearLayout page=Ui.page(this,"Widget vergrendeld");
            Ui.text(this,page,"Stel eerst een pincode of schermvergrendeling in bij de instellingen van je telefoon. Deze widget blijft vergrendeld.",16);
            Ui.button(this,page,"Sluiten",v -> finish()); return;
        }
        authRunning=true;
        if(mode==2 && Build.VERSION.SDK_INT>=30) {
            cancellation=new CancellationSignal();
            BiometricPrompt prompt=new BiometricPrompt.Builder(this)
                .setTitle("Widget ontgrendelen")
                .setSubtitle("Gebruik je vingerafdruk of de ontgrendeling van je telefoon")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG|BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
            try {
                prompt.authenticate(cancellation,getMainExecutor(),new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) { if(authRunning) allowDisplay(); }
                    @Override public void onAuthenticationError(int code,CharSequence message) { if(!isFinishing()&&!isDestroyed()) { authRunning=false; finish(); } }
                });
            } catch(RuntimeException e) { authRunning=false; Ui.toast(this,"Ontgrendelen is niet beschikbaar. Probeer opnieuw."); finish(); }
        } else {
            KeyguardManager manager=getSystemService(KeyguardManager.class);
            Intent confirmation=manager.createConfirmDeviceCredentialIntent("Widget ontgrendelen","Gebruik de pincode of schermvergrendeling van je telefoon");
            if(confirmation==null) { authRunning=false; finish(); return; }
            try { startActivityForResult(confirmation,AUTH_REQUEST); }
            catch(RuntimeException e) { authRunning=false; finish(); }
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent intent) {
        super.onActivityResult(request,result,intent);
        if(request==AUTH_REQUEST && authRunning) {
            authRunning=false;
            if(result==RESULT_OK) allowDisplay(); else finish();
        }
    }
    @Override protected void onStop() {
        if(protectionStarted && unlocked && !isFinishing() && WidgetSecurity.isLocked(this,guardedWidgetId)) {
            hideContent();
        }
        super.onStop();
    }
    private void hideContent() {
        unlocked=false; requiresExistingWidget=true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        ViewGroup content=findViewById(android.R.id.content);
        if(content!=null && content.getChildCount()>0) { hiddenContent=content.getChildAt(0); content.removeView(hiddenContent); }
        showLockedPage();
    }
    @Override protected void onDestroy() {
        authRunning=false;
        if(cancellation!=null) cancellation.cancel();
        firstDisplay=null; deferredAction=null; hiddenContent=null;
        super.onDestroy();
    }
}
