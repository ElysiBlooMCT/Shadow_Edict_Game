package org.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * JSONArray rút gọn cho bản web (TeaVM), chữ ký giống org.json 20090211, không dùng reflection.
 */
public class JSONArray {
    private final ArrayList<Object> list = new ArrayList<>();

    public JSONArray() {
    }

    public JSONArray(JSONTokener x) throws JSONException {
        if (x.nextClean() != '[') throw new JSONException("A JSONArray text must start with '['");
        if (x.peekChar() == ']') {
            x.nextClean();
            return;
        }
        while (true) {
            if (x.peekChar() == ',') {
                x.nextClean();
                list.add(null);
            } else {
                list.add(x.nextValue());
            }
            char c = x.nextClean();
            if (c == ']') return;
            if (c != ',' && c != ';') throw new JSONException("Expected a ',' or ']'");
            if (x.peekChar() == ']') {
                x.nextClean();
                return;
            }
        }
    }

    public JSONArray(String source) throws JSONException {
        this(new JSONTokener(source));
    }

    public JSONArray(Collection collection) {
        if (collection != null) {
            for (Object o : collection) {
                if (o != null) list.add(JSONObject.wrap(o));
            }
        }
    }

    public int length() {
        return list.size();
    }

    public Object opt(int index) {
        return (index < 0 || index >= list.size()) ? null : list.get(index);
    }

    public Object get(int index) throws JSONException {
        Object o = opt(index);
        if (o == null) throw new JSONException("JSONArray[" + index + "] not found.");
        return o;
    }

    public boolean isNull(int index) {
        return JSONObject.NULL.equals(opt(index));
    }

    public boolean getBoolean(int index) throws JSONException {
        Object o = get(index);
        if (Boolean.FALSE.equals(o) || (o instanceof String && ((String) o).equalsIgnoreCase("false"))) return false;
        if (Boolean.TRUE.equals(o) || (o instanceof String && ((String) o).equalsIgnoreCase("true"))) return true;
        throw new JSONException("JSONArray[" + index + "] is not a Boolean.");
    }

    public double getDouble(int index) throws JSONException {
        Object o = get(index);
        try {
            return o instanceof Number ? ((Number) o).doubleValue() : Double.parseDouble(o.toString());
        } catch (Exception e) {
            throw new JSONException("JSONArray[" + index + "] is not a number.");
        }
    }

    public int getInt(int index) throws JSONException {
        Object o = get(index);
        return o instanceof Number ? ((Number) o).intValue() : (int) getDouble(index);
    }

    public long getLong(int index) throws JSONException {
        Object o = get(index);
        return o instanceof Number ? ((Number) o).longValue() : (long) getDouble(index);
    }

    public String getString(int index) throws JSONException {
        return get(index).toString();
    }

    public JSONObject getJSONObject(int index) throws JSONException {
        Object o = get(index);
        if (o instanceof JSONObject) return (JSONObject) o;
        throw new JSONException("JSONArray[" + index + "] is not a JSONObject.");
    }

    public JSONArray getJSONArray(int index) throws JSONException {
        Object o = get(index);
        if (o instanceof JSONArray) return (JSONArray) o;
        throw new JSONException("JSONArray[" + index + "] is not a JSONArray.");
    }

    public boolean optBoolean(int index) {
        return optBoolean(index, false);
    }

    public boolean optBoolean(int index, boolean defaultValue) {
        try {
            return getBoolean(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public double optDouble(int index) {
        return optDouble(index, Double.NaN);
    }

    public double optDouble(int index, double defaultValue) {
        try {
            return getDouble(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int optInt(int index) {
        return optInt(index, 0);
    }

    public int optInt(int index, int defaultValue) {
        try {
            return getInt(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long optLong(int index) {
        return optLong(index, 0L);
    }

    public long optLong(int index, long defaultValue) {
        try {
            return getLong(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String optString(int index) {
        return optString(index, "");
    }

    public String optString(int index, String defaultValue) {
        Object o = opt(index);
        return o != null ? o.toString() : defaultValue;
    }

    public JSONObject optJSONObject(int index) {
        Object o = opt(index);
        return o instanceof JSONObject ? (JSONObject) o : null;
    }

    public JSONArray optJSONArray(int index) {
        Object o = opt(index);
        return o instanceof JSONArray ? (JSONArray) o : null;
    }

    public JSONArray put(boolean value) {
        list.add(Boolean.valueOf(value));
        return this;
    }

    public JSONArray put(Collection value) {
        list.add(new JSONArray(value));
        return this;
    }

    public JSONArray put(double value) throws JSONException {
        Double d = Double.valueOf(value);
        JSONObject.testValidity(d);
        list.add(d);
        return this;
    }

    public JSONArray put(int value) {
        list.add(Integer.valueOf(value));
        return this;
    }

    public JSONArray put(long value) {
        list.add(Long.valueOf(value));
        return this;
    }

    public JSONArray put(Map value) {
        list.add(new JSONObject(value));
        return this;
    }

    public JSONArray put(Object value) {
        list.add(JSONObject.wrap(value));
        return this;
    }

    public JSONArray put(int index, boolean value) throws JSONException {
        return put(index, (Object) Boolean.valueOf(value));
    }

    public JSONArray put(int index, Collection value) throws JSONException {
        return put(index, (Object) new JSONArray(value));
    }

    public JSONArray put(int index, double value) throws JSONException {
        return put(index, (Object) Double.valueOf(value));
    }

    public JSONArray put(int index, int value) throws JSONException {
        return put(index, (Object) Integer.valueOf(value));
    }

    public JSONArray put(int index, long value) throws JSONException {
        return put(index, (Object) Long.valueOf(value));
    }

    public JSONArray put(int index, Map value) throws JSONException {
        return put(index, (Object) new JSONObject(value));
    }

    public JSONArray put(int index, Object value) throws JSONException {
        JSONObject.testValidity(value);
        if (index < 0) throw new JSONException("JSONArray[" + index + "] not found.");
        if (index < list.size()) {
            list.set(index, JSONObject.wrap(value));
        } else {
            while (index != list.size()) list.add(JSONObject.NULL);
            list.add(JSONObject.wrap(value));
        }
        return this;
    }

    public String join(String separator) throws JSONException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(separator);
            sb.append(JSONObject.valueToString(list.get(i)));
        }
        return sb.toString();
    }

    public JSONObject toJSONObject(JSONArray names) throws JSONException {
        if (names == null || names.length() == 0 || list.isEmpty()) return null;
        JSONObject jo = new JSONObject();
        for (int i = 0; i < names.length(); i++) jo.put(names.getString(i), opt(i));
        return jo;
    }

    @Override
    public String toString() {
        try {
            return '[' + join(",") + ']';
        } catch (Exception e) {
            return null;
        }
    }

    public String toString(int indentFactor) throws JSONException {
        return toString();
    }
}
