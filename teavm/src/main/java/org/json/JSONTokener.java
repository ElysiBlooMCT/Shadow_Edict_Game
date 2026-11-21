package org.json;

/** Bộ đọc JSON đơn giản (dùng nội bộ cho JSONObject / JSONArray bản web). */
public class JSONTokener {
    private final String s;
    private int pos;

    public JSONTokener(String s) {
        this.s = s == null ? "" : s;
    }

    private JSONException error(String msg) {
        return new JSONException(msg + " at character " + pos);
    }

    private char peek() {
        return pos < s.length() ? s.charAt(pos) : 0;
    }

    private void skipWhitespace() {
        while (pos < s.length() && s.charAt(pos) <= ' ') pos++;
    }

    public char nextClean() {
        skipWhitespace();
        return pos < s.length() ? s.charAt(pos++) : 0;
    }

    public void back() {
        if (pos > 0) pos--;
    }

    public boolean end() {
        skipWhitespace();
        return pos >= s.length();
    }

    public Object nextValue() throws JSONException {
        char c = nextClean();
        switch (c) {
            case '"':
            case '\'':
                return nextString(c);
            case '{':
                back();
                return new JSONObject(this);
            case '[':
                back();
                return new JSONArray(this);
            default:
                break;
        }
        back();
        int start = pos;
        while (pos < s.length()) {
            char ch = s.charAt(pos);
            if (ch == ',' || ch == ':' || ch == ']' || ch == '}' || ch == '/' || ch <= ' ') break;
            pos++;
        }
        String word = s.substring(start, pos).trim();
        if (word.isEmpty()) throw error("Missing value");
        return JSONObject.stringToValue(word);
    }

    public String nextString(char quote) throws JSONException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= s.length()) throw error("Unterminated string");
            char c = s.charAt(pos++);
            if (c == quote) return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= s.length()) throw error("Unterminated string");
            c = s.charAt(pos++);
            switch (c) {
                case 'b': sb.append('\b'); break;
                case 't': sb.append('\t'); break;
                case 'n': sb.append('\n'); break;
                case 'f': sb.append('\f'); break;
                case 'r': sb.append('\r'); break;
                case 'u':
                    if (pos + 4 > s.length()) throw error("Illegal escape");
                    try {
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException e) {
                        throw error("Illegal escape");
                    }
                    pos += 4;
                    break;
                default:
                    sb.append(c);
            }
        }
    }

    char peekChar() {
        skipWhitespace();
        return peek();
    }
}
