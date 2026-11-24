package io.qzz.elysibloomct.ctv.se.teavm;

import io.qzz.elysibloomct.ctv.se.GameSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kết nối Socket.IO trên trình duyệt: gọi thư viện socket.io bản JavaScript (tải từ CDN khi cần).
 * Cùng server, cùng sự kiện với bản PC/Android nên người chơi web và điện thoại vào chung phòng được.
 *
 * Dữ liệu đi qua lại dưới dạng chuỗi JSON rồi chuyển thành JSONObject / JSONArray / String / số,
 * giống hệt kiểu mà thư viện Java trả về, nên code game trong core không phải sửa gì.
 */
public final class WebGameSocket extends GameSocket {

    /** Thư viện client phải cùng đời với server (Socket.IO 4). */
    private static final String SOCKET_IO_JS = "https://cdn.socket.io/4.7.5/socket.io.min.js";

    @JSFunctor
    public interface EventCallback extends JSObject {
        void call(String argsJson);
    }

    private final JSObject handle;
    private final Map<String, List<Listener>> listeners = new HashMap<>();
    private final Map<String, List<Listener>> onceListeners = new HashMap<>();
    private final Set<String> attached = new HashSet<>();

    public WebGameSocket(String url) {
        handle = create(url, SOCKET_IO_JS);
    }

    @Override
    public GameSocket on(String event, Listener listener) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
        attach(event);
        return this;
    }

    @Override
    public GameSocket once(String event, Listener listener) {
        onceListeners.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
        attach(event);
        return this;
    }

    @Override
    public GameSocket off(String event) {
        listeners.remove(event);
        onceListeners.remove(event);
        if (attached.remove(event)) jsOff(handle, event);
        return this;
    }

    @Override
    public GameSocket emit(String event, Object... args) {
        StringBuilder sb = new StringBuilder("[");
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(toJson(args[i]));
            }
        }
        sb.append(']');
        jsEmit(handle, event, sb.toString());
        return this;
    }

    @Override
    public boolean connected() {
        return jsConnected(handle);
    }

    @Override
    public String id() {
        return jsId(handle);
    }

    @Override
    public GameSocket connect() {
        jsConnect(handle);
        return this;
    }

    @Override
    public GameSocket disconnect() {
        jsDisconnect(handle);
        return this;
    }

    // ---------------------------------------------------------------- chuyển dữ liệu

    private void attach(String event) {
        if (attached.add(event)) {
            jsOn(handle, event, json -> dispatch(event, json));
        }
    }

    private void dispatch(String event, String json) {
        Object[] args = parseArgs(json);
        List<Listener> once = onceListeners.remove(event);
        List<Listener> many = listeners.get(event);
        if (many != null) {
            for (Listener l : new ArrayList<>(many)) safeCall(l, args);
        }
        if (once != null) {
            for (Listener l : once) safeCall(l, args);
        }
    }

    private static void safeCall(Listener l, Object[] args) {
        try {
            l.call(args);
        } catch (Throwable t) {
            System.out.println("WebGameSocket: loi xu ly su kien -> " + t);
        }
    }

    private static Object[] parseArgs(String json) {
        if (json == null || json.isEmpty()) return new Object[0];
        try {
            JSONArray arr = new JSONArray(json);
            Object[] out = new Object[arr.length()];
            for (int i = 0; i < out.length; i++) {
                Object v = arr.opt(i);
                out[i] = JSONObject.NULL.equals(v) ? null : v;
            }
            return out;
        } catch (Exception e) {
            System.out.println("WebGameSocket: du lieu khong hop le -> " + e.getMessage());
            return new Object[0];
        }
    }

    private static String toJson(Object v) {
        if (v == null) return "null";
        if (v instanceof JSONObject || v instanceof JSONArray) return v.toString();
        if (v instanceof Boolean) return v.toString();
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return "null";
            return v.toString();
        }
        return JSONObject.quote(v.toString());
    }

    // ---------------------------------------------------------------- phần JavaScript

    /**
     * Tạo đối tượng điều khiển. Thư viện socket.io được nạp bất đồng bộ; trong lúc chờ,
     * các lệnh on/connect được ghi nhớ và áp dụng ngay khi thư viện nạp xong.
     */
    @JSBody(params = { "url", "lib" }, script = ""
        + "var w = { sock: null, handlers: {}, wantConnect: false, lib: lib };"
        + "function boot() {"
        + "  if (w.sock || typeof io === 'undefined') return;"
        + "  w.sock = io(url, { transports: ['websocket'], autoConnect: false, reconnection: true,"
        + "    reconnectionAttempts: Infinity, reconnectionDelay: 1000, reconnectionDelayMax: 8000, timeout: 5000 });"
        + "  for (var ev in w.handlers) { if (w.handlers.hasOwnProperty(ev)) w.sock.on(ev, w.handlers[ev]); }"
        + "  if (w.wantConnect) w.sock.connect();"
        + "}"
        + "function loadLib() {"
        + "  if (typeof io !== 'undefined') { boot(); return; }"
        + "  var tag = document.getElementById('se-socketio');"
        + "  if (tag) return;"
        + "  tag = document.createElement('script'); tag.id = 'se-socketio'; tag.src = w.lib;"
        + "  tag.addEventListener('load', boot);"
        + "  tag.addEventListener('error', function () {"
        + "    console.error('Khong tai duoc thu vien socket.io: ' + w.lib);"
        + "    if (tag.parentNode) tag.parentNode.removeChild(tag);"
        + "    var errH = w.handlers['connect_error'];"
        + "    if (errH) errH('Khong tai duoc thu vien socket.io');"
        + "  });"
        + "  document.head.appendChild(tag);"
        + "}"
        + "w.boot = boot;"
        + "w.loadLib = loadLib;"
        + "loadLib();"
        + "return w;")
    private static native JSObject create(String url, String lib);

    @JSBody(params = { "w", "ev", "cb" }, script = ""
        + "var h = function () {"
        + "  var a = Array.prototype.slice.call(arguments).map(function (x) {"
        + "    return (x instanceof Error) ? (x.message || String(x)) : x;"
        + "  });"
        + "  var j; try { j = JSON.stringify(a); } catch (e) { j = '[]'; }"
        + "  cb(j);"
        + "};"
        + "w.handlers[ev] = h;"
        + "if (w.sock) w.sock.on(ev, h);")
    private static native void jsOn(JSObject w, String ev, EventCallback cb);

    @JSBody(params = { "w", "ev" }, script = ""
        + "var h = w.handlers[ev]; delete w.handlers[ev];"
        + "if (w.sock && h) w.sock.off(ev, h);")
    private static native void jsOff(JSObject w, String ev);

    @JSBody(params = { "w", "ev", "json" }, script = ""
        + "if (!w.sock) return;"
        + "var a; try { a = JSON.parse(json); } catch (e) { a = []; }"
        + "w.sock.emit.apply(w.sock, [ev].concat(a));")
    private static native void jsEmit(JSObject w, String ev, String json);

    @JSBody(params = { "w" }, script = "return !!(w.sock && w.sock.connected);")
    private static native boolean jsConnected(JSObject w);

    @JSBody(params = { "w" }, script = "return (w.sock && w.sock.id) ? w.sock.id : null;")
    private static native String jsId(JSObject w);

    @JSBody(params = { "w" }, script = "w.wantConnect = true; if (w.sock) w.sock.connect(); else if (typeof io !== 'undefined') w.boot(); else w.loadLib();")
    private static native void jsConnect(JSObject w);

    @JSBody(params = { "w" }, script = "w.wantConnect = false; if (w.sock) w.sock.disconnect();")
    private static native void jsDisconnect(JSObject w);
}
