package org.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSONObject rút gọn cho bản web (TeaVM), chữ ký giống org.json 20090211, không dùng reflection.
 * Chỉ dùng trong module teavm; PC/Android vẫn dùng thư viện org.json thật.
 */
public class JSONObject {

    private static final class Null {
        @Override
        public boolean equals(Object o) {
            return o == null || o == this;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "null";
        }
    }

    public static final Object NULL = new Null();

    private final Map<String, Object> map = new LinkedHashMap<>();

    public JSONObject() {
    }

    public JSONObject(JSONTokener x) throws JSONException {
        if (x.nextClean() != '{') throw new JSONException("A JSONObject text must begin with '{'");
        if (x.peekChar() == '}') {
            x.nextClean();
            return;
        }
        while (true) {
            char c = x.nextClean();
            String key;
            if (c == '"' || c == '\'') {
                key = x.nextString(c);
            } else if (c == 0) {
                throw new JSONException("A JSONObject text must end with '}'");
            } else {
                x.back();
                key = x.nextValue().toString();
            }
            c = x.nextClean();
            if (c != ':') throw new JSONException("Expected a ':' after a key");
            putOnce(key, x.nextValue());
            c = x.nextClean();
            if (c == '}') return;
            if (c != ',' && c != ';') throw new JSONException("Expected a ',' or '}'");
            if (x.peekChar() == '}') {
                x.nextClean();
                return;
            }
        }
    }

    public JSONObject(String source) throws JSONException {
        this(new JSONTokener(source));
    }

    public JSONObject(Map map) {
        if (map != null) {
            for (Object o : map.entrySet()) {
                Map.Entry e = (Map.Entry) o;
                if (e.getValue() != null) this.map.put(String.valueOf(e.getKey()), wrap(e.getValue()));
            }
        }
    }

    static Object wrap(Object value) {
        if (value instanceof Map && !(value instanceof JSONObject)) return new JSONObject((Map) value);
        if (value instanceof Collection) return new JSONArray((Collection) value);
        return value;
    }

    static void testValidity(Object o) throws JSONException {
        if (o instanceof Double) {
            Double d = (Double) o;
            if (d.isInfinite() || d.isNaN()) throw new JSONException("JSON does not allow non-finite numbers.");
        } else if (o instanceof Float) {
            Float f = (Float) o;
            if (f.isInfinite() || f.isNaN()) throw new JSONException("JSON does not allow non-finite numbers.");
        }
    }

    // ----- truy cập -----

    public Object opt(String key) {
        return key == null ? null : map.get(key);
    }

    public Object get(String key) throws JSONException {
        Object o = opt(key);
        if (o == null) throw new JSONException("JSONObject[" + quote(key) + "] not found.");
        return o;
    }

    public boolean has(String key) {
        return map.containsKey(key);
    }

    public boolean isNull(String key) {
        return NULL.equals(opt(key));
    }

    public Iterator keys() {
        return new ArrayList<>(map.keySet()).iterator();
    }

    public Iterator sortedKeys() {
        List<String> k = new ArrayList<>(map.keySet());
        Collections.sort(k);
        return k.iterator();
    }

    public int length() {
        return map.size();
    }

    public JSONArray names() {
        JSONArray ja = new JSONArray();
        for (String k : map.keySet()) ja.put(k);
        return ja.length() == 0 ? null : ja;
    }

    public Object remove(String key) {
        return map.remove(key);
    }

    public boolean getBoolean(String key) throws JSONException {
        Object o = get(key);
        if (Boolean.FALSE.equals(o) || (o instanceof String && ((String) o).equalsIgnoreCase("false"))) return false;
        if (Boolean.TRUE.equals(o) || (o instanceof String && ((String) o).equalsIgnoreCase("true"))) return true;
        throw new JSONException("JSONObject[" + quote(key) + "] is not a Boolean.");
    }

    public double getDouble(String key) throws JSONException {
        Object o = get(key);
        try {
            return o instanceof Number ? ((Number) o).doubleValue() : Double.parseDouble(o.toString());
        } catch (Exception e) {
            throw new JSONException("JSONObject[" + quote(key) + "] is not a number.");
        }
    }

    public int getInt(String key) throws JSONException {
        Object o = get(key);
        return o instanceof Number ? ((Number) o).intValue() : (int) getDouble(key);
    }

    public long getLong(String key) throws JSONException {
        Object o = get(key);
        return o instanceof Number ? ((Number) o).longValue() : (long) getDouble(key);
    }

    public String getString(String key) throws JSONException {
        return get(key).toString();
    }

    public JSONObject getJSONObject(String key) throws JSONException {
        Object o = get(key);
        if (o instanceof JSONObject) return (JSONObject) o;
        throw new JSONException("JSONObject[" + quote(key) + "] is not a JSONObject.");
    }

    public JSONArray getJSONArray(String key) throws JSONException {
        Object o = get(key);
        if (o instanceof JSONArray) return (JSONArray) o;
        throw new JSONException("JSONObject[" + quote(key) + "] is not a JSONArray.");
    }

    public boolean optBoolean(String key) {
        return optBoolean(key, false);
    }

    public boolean optBoolean(String key, boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public double optDouble(String key) {
        return optDouble(key, Double.NaN);
    }

    public double optDouble(String key, double defaultValue) {
        try {
            Object o = opt(key);
            return o instanceof Number ? ((Number) o).doubleValue() : Double.parseDouble((String) o);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int optInt(String key) {
        return optInt(key, 0);
    }

    public int optInt(String key, int defaultValue) {
        try {
            return getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long optLong(String key) {
        return optLong(key, 0L);
    }

    public long optLong(String key, long defaultValue) {
        try {
            return getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String optString(String key) {
        return optString(key, "");
    }

    public String optString(String key, String defaultValue) {
        Object o = opt(key);
        return (o == null || NULL.equals(o)) ? defaultValue : o.toString();
    }

    public JSONObject optJSONObject(String key) {
        Object o = opt(key);
        return o instanceof JSONObject ? (JSONObject) o : null;
    }

    public JSONArray optJSONArray(String key) {
        Object o = opt(key);
        return o instanceof JSONArray ? (JSONArray) o : null;
    }

    // ----- ghi -----

    public JSONObject put(String key, boolean value) throws JSONException {
        return put(key, (Object) Boolean.valueOf(value));
    }

    public JSONObject put(String key, double value) throws JSONException {
        return put(key, (Object) Double.valueOf(value));
    }

    public JSONObject put(String key, int value) throws JSONException {
        return put(key, (Object) Integer.valueOf(value));
    }

    public JSONObject put(String key, long value) throws JSONException {
        return put(key, (Object) Long.valueOf(value));
    }

    public JSONObject put(String key, Collection value) throws JSONException {
        return put(key, (Object) new JSONArray(value));
    }

    public JSONObject put(String key, Map value) throws JSONException {
        return put(key, (Object) new JSONObject(value));
    }

    public JSONObject put(String key, Object value) throws JSONException {
        if (key == null) throw new JSONException("Null key.");
        if (value != null) {
            testValidity(value);
            map.put(key, wrap(value));
        } else {
            remove(key);
        }
        return this;
    }

    public JSONObject putOnce(String key, Object value) throws JSONException {
        if (key != null && value != null) {
            if (opt(key) != null) throw new JSONException("Duplicate key \"" + key + "\"");
            put(key, value);
        }
        return this;
    }

    public JSONObject putOpt(String key, Object value) throws JSONException {
        if (key != null && value != null) put(key, value);
        return this;
    }

    public JSONObject accumulate(String key, Object value) throws JSONException {
        testValidity(value);
        Object o = opt(key);
        if (o == null) {
            put(key, value instanceof JSONArray ? new JSONArray().put(value) : value);
        } else if (o instanceof JSONArray) {
            ((JSONArray) o).put(value);
        } else {
            put(key, new JSONArray().put(o).put(value));
        }
        return this;
    }

    public JSONObject append(String key, Object value) throws JSONException {
        testValidity(value);
        Object o = opt(key);
        if (o == null) {
            put(key, new JSONArray().put(value));
        } else if (o instanceof JSONArray) {
            put(key, ((JSONArray) o).put(value));
        } else {
            throw new JSONException("JSONObject[" + key + "] is not a JSONArray.");
        }
        return this;
    }

    // ----- chuyển đổi -----

    public static Object stringToValue(String s) {
        if (s.isEmpty()) return s;
        if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (s.equalsIgnoreCase("false")) return Boolean.FALSE;
        if (s.equalsIgnoreCase("null")) return NULL;
        char b = s.charAt(0);
        if ((b >= '0' && b <= '9') || b == '-' || b == '.' || b == '+') {
            try {
                if (s.indexOf('.') < 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
                    long l = Long.parseLong(s.charAt(0) == '+' ? s.substring(1) : s);
                    if (l == (int) l) return Integer.valueOf((int) l);
                    return Long.valueOf(l);
                }
                return Double.valueOf(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return s;
    }

    public static String numberToString(Number n) throws JSONException {
        if (n == null) throw new JSONException("Null pointer");
        testValidity(n);
        if (n instanceof Double || n instanceof Float) {
            double d = n.doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e15) return Long.toString((long) d);
        }
        return n.toString();
    }

    public static String doubleToString(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) return "null";
        try {
            return numberToString(Double.valueOf(d));
        } catch (JSONException e) {
            return "null";
        }
    }

    public static String quote(String string) {
        if (string == null || string.isEmpty()) return "\"\"";
        StringBuilder sb = new StringBuilder(string.length() + 4);
        sb.append('"');
        char prev = 0;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\').append(c);
                    break;
                case '/':
                    if (prev == '<') sb.append('\\');
                    sb.append(c);
                    break;
                case '\b': sb.append("\\b"); break;
                case '\t': sb.append("\\t"); break;
                case '\n': sb.append("\\n"); break;
                case '\f': sb.append("\\f"); break;
                case '\r': sb.append("\\r"); break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < ' ') || (c >= ' ' && c < '℀')) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int k = hex.length(); k < 4; k++) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
            prev = c;
        }
        sb.append('"');
        return sb.toString();
    }

    static String valueToString(Object value) throws JSONException {
        if (value == null || NULL.equals(value)) return "null";
        if (value instanceof Number) return numberToString((Number) value);
        if (value instanceof Boolean || value instanceof JSONObject || value instanceof JSONArray) return value.toString();
        if (value instanceof Map) return new JSONObject((Map) value).toString();
        if (value instanceof Collection) return new JSONArray((Collection) value).toString();
        return quote(value.toString());
    }

    @Override
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(quote(e.getKey())).append(':').append(valueToString(e.getValue()));
            }
            return sb.append('}').toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String toString(int indentFactor) throws JSONException {
        return toString();
    }
}
