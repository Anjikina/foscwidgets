package be.foscwidgets;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.*;

final class Ui {
    static int dp(Activity a, int n) { return (int)(n * a.getResources().getDisplayMetrics().density + .5f); }
    static LinearLayout page(Activity a, String heading) {
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(a); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(a,22), dp(a,22), dp(a,22), dp(a,28));
        body.setBackgroundColor(Color.rgb(245,248,247));
        scroll.addView(body); a.setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets) -> { v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets; });
        text(a,body,"FOSC / PERSOONLIJK",12); text(a,body,heading,28);
        return body;
    }
    static TextView text(Activity a, LinearLayout p, String s, int size) {
        TextView t = new TextView(a); t.setText(s); t.setTextSize(size); t.setTextColor(Color.rgb(24,54,51)); t.setPadding(0,dp(a,8),0,dp(a,12)); p.addView(t); return t;
    }
    static Button button(Activity a, LinearLayout p, String s, View.OnClickListener click) {
        Button b = new Button(a); b.setText(s); b.setAllCaps(false); b.setOnClickListener(click); p.addView(b,new LinearLayout.LayoutParams(-1,-2)); return b;
    }
    static EditText input(Activity a, LinearLayout p, String label, String value) {
        text(a,p,label,14); EditText e = new EditText(a); e.setText(value); e.setSingleLine(true); p.addView(e,new LinearLayout.LayoutParams(-1,-2)); return e;
    }
    static void toast(Activity a,String s) { Toast.makeText(a,s,Toast.LENGTH_LONG).show(); }
}
