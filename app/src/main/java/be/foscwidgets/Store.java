package be.foscwidgets;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

final class Store {
    static final int LIMIT = 12;
    static JSONObject fresh() {
        JSONObject o = new JSONObject();
        try { o.put("title", "Mijn widget"); o.put("columns", 1); o.put("size", 16); o.put("color", "#FFFFFF"); o.put("icons", false); o.put("edit", true); o.put("items", new JSONArray()); }
        catch (Exception e) { throw new IllegalStateException(e); }
        return o;
    }
    static JSONObject get(Context c, int id) {
        try { String s = c.getSharedPreferences("widgets", 0).getString("w" + id, null); return s == null ? fresh() : new JSONObject(s); }
        catch (Exception e) { return fresh(); }
    }
    static boolean exists(Context c, int id) { return c.getSharedPreferences("widgets", 0).contains("w" + id); }
    static void save(Context c, int id, JSONObject o) {
        if (!c.getSharedPreferences("widgets", 0).edit().putString("w" + id, o.toString()).commit()) throw new IllegalStateException("Opslaan is mislukt");
    }
    static void delete(Context c, int id) { c.getSharedPreferences("widgets", 0).edit().remove("w" + id).apply(); }
    static JSONArray items(JSONObject o) { JSONArray a = o.optJSONArray("items"); return a == null ? new JSONArray() : a; }
    static JSONObject item(String type, String label, String value) {
        JSONObject o = new JSONObject();
        try { o.put("id", java.util.UUID.randomUUID().toString()); o.put("type", type); o.put("label", label); o.put("value", value); }
        catch (Exception e) { throw new IllegalStateException(e); }
        return o;
    }
}
