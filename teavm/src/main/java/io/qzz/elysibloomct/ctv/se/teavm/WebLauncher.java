package io.qzz.elysibloomct.ctv.se.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.github.xpenatan.gdx.teavm.backends.web.WebPreloadApplicationListener;
import io.qzz.elysibloomct.ctv.se.Main;
import io.qzz.elysibloomct.ctv.se.NetworkManager;
import io.qzz.elysibloomct.ctv.se.PortraitRotation;
import io.qzz.elysibloomct.ctv.se.TextInputBridge;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/** Chạy game trên trình duyệt (gdx-teavm). */
public class WebLauncher {
    @JSFunctor
    public interface TextCallback extends JSObject {
        void onResult(String value);
    }

    public static void main(String[] args) {
        // Online trên web dùng thư viện socket.io bản JavaScript, cùng server với PC/Android.
        NetworkManager.socketFactory = WebGameSocket::new;
        // Điện thoại để dọc (khóa xoay) vẫn thấy game nằm ngang.
        PortraitRotation.enabled = true;

        // Nút THOÁT trong game: về trang chủ website (trình duyệt không cho trang tự đóng tab).
        Main.exitHandler = WebLauncher::goHome;

        // Điện thoại/máy tính bảng: chạm vào ô nhập chữ sẽ mở hộp nhập HTML bất đồng bộ (không treo Socket.IO).
        if (isTouchDevice()) {
            TextInputBridge.prompt = (title, current, maxLength, onResult) ->
                askTextAsync(title == null ? "" : title, current == null ? "" : current, maxLength, onResult::accept);
        }

        // Biểu tượng và tên trên tab trình duyệt (trang do gdx-teavm tạo không có sẵn).
        setupPageIcon();

        // Trang không cuộn và luôn vừa khung nhìn (tránh lệch khi thanh địa chỉ ẩn/hiện).
        setupPageLayout();

        // Bảng lưu ý hiện mỗi lần mở bản web; game vẫn tải ngầm phía sau.
        showNotice();

        WebApplicationConfiguration config = new WebApplicationConfiguration();
        config.width = 0;   // 0 = lấp đầy cửa sổ trình duyệt
        config.height = 0;
        config.showDownloadLogs = true;
        // Màn hình tải: logo ElysiBlooMCT thay cho logo libGDX mặc định (đường dẫn tính từ thư mục assets).
        WebPreloadApplicationListener preload = new WebPreloadApplicationListener();
        preload.startupLogo = "web_loading_logo.png";
        new WebApplication(new Main(), preload, config);
    }

    @JSBody(script = ""
        + "if (document.getElementById('se-notice')) return;\n"
        + "var css = [\n"
        + "  '#se-notice{position:fixed;inset:0;z-index:99999;display:flex;align-items:center;justify-content:center;padding:16px;background:rgba(6,5,4,.82);font-family:system-ui,-apple-system,\"Segoe UI\",Roboto,sans-serif;}',\n"
        + "  '#se-notice .box{width:min(560px,100%);max-height:calc(100vh - 32px);max-height:calc(100dvh - 32px);overflow:auto;background:#16120f;color:#f2ebdf;border:2px solid #e2b65a;border-radius:14px;padding:22px 22px 18px;box-shadow:0 18px 40px rgba(0,0,0,.6);}',\n"
        + "  '#se-notice h2{margin:0 0 4px;font:700 1.45rem/1.2 Georgia,serif;color:#f7e3b3;}',\n"
        + "  '#se-notice .sub{margin:0 0 14px;color:#b6a891;font-size:.92rem;}',\n"
        + "  '#se-notice ul{margin:0 0 18px;padding:0;list-style:none;display:grid;gap:9px;}',\n"
        + "  '#se-notice li{position:relative;padding:9px 12px 9px 34px;background:rgba(237,207,142,.06);border:1px solid rgba(226,182,90,.3);border-radius:8px;font-size:.93rem;line-height:1.45;}',\n"
        + "  '#se-notice li::before{content:\"◆\";position:absolute;left:12px;top:9px;color:#e2b65a;}',\n"
        + "  '#se-notice b{color:#f7e3b3;}',\n"
        + "  '#se-notice .acts{display:flex;flex-wrap:wrap;gap:10px;}',\n"
        + "  '#se-notice a,#se-notice button{flex:1 1 200px;min-height:46px;display:flex;align-items:center;justify-content:center;border-radius:9px;font:800 1rem/1.2 system-ui,sans-serif;cursor:pointer;text-decoration:none;}',\n"
        + "  '#se-notice a{background:linear-gradient(180deg,#f7e3b3,#edcf8e);color:#1a1208;border:2px solid transparent;}',\n"
        + "  '#se-notice button{background:rgba(226,182,90,.1);color:#f2ebdf;border:2px solid #e2b65a;}',\n"
        + "  '@media (max-height:520px),(max-width:420px){#se-notice .box{padding:16px 14px 14px;}#se-notice li{font-size:.86rem;padding:7px 10px 7px 30px;}#se-notice li::before{left:10px;top:7px;}#se-notice h2{font-size:1.2rem;}#se-notice ul{gap:7px;margin-bottom:14px;}}'\n"
        + "].join('');\n"
        + "var st = document.createElement('style');\n"
        + "st.textContent = css;\n"
        + "document.head.appendChild(st);\n"
        + "var d = document.createElement('div');\n"
        + "d.id = 'se-notice';\n"
        + "d.setAttribute('role', 'dialog');\n"
        + "d.setAttribute('aria-modal', 'true');\n"
        + "d.setAttribute('aria-labelledby', 'se-notice-title');\n"
        + "d.innerHTML = '<div class=\"box\">'\n"
        + "  + '<h2 id=\"se-notice-title\">Bạn đang chơi bản trình duyệt</h2>'\n"
        + "  + '<p class=\"sub\">Game đang tải ngầm phía sau. Đọc nhanh vài lưu ý nhé:</p><ul>'\n"
        + "  + '<li>Lần đầu cần tải khoảng <b>100 MB</b>, có thể mất vài phút. Những lần sau nhanh hơn.</li>'\n"
        + "  + '<li>Vẫn <b>chơi online chung</b> với người chơi điện thoại và máy tính, nhưng <b>chưa có đàm thoại micro</b> (vẫn chat chữ).</li>'\n"
        + "  + '<li>Tiến độ và tên nhân vật <b>lưu trong trình duyệt</b>: xoá dữ liệu duyệt web hoặc dùng tab ẩn danh sẽ mất.</li>'\n"
        + "  + '<li>Chạy chậm hơn bản cài đặt; điện thoại yếu có thể giật ở trận 5v5 đông người.</li>'\n"
        + "  + '<li>Điện thoại <b>để dọc vẫn chơi được</b>: game tự nằm ngang, chỉ cần xoay máy để cầm.</li>'\n"
        + "  + '</ul><div class=\"acts\">'\n"
        + "  + '<a href=\"../download/\">Tải ứng dụng (mượt nhất)</a>'\n"
        + "  + '<button type=\"button\" id=\"se-notice-ok\">Chơi trên web</button>'\n"
        + "  + '</div></div>';\n"
        + "document.body.appendChild(d);\n"
        + "document.getElementById('se-notice-ok').addEventListener('click', function () {\n"
        + "  d.parentNode.removeChild(d);\n"
        + "  var c = document.querySelector('canvas');\n"
        + "  if (c && c.focus) c.focus();\n"
        + "});\n"
    )
    private static native void showNotice();

    /**
     * Cố định trang cho vừa màn hình điện thoại: không cuộn, không kéo-để-tải-lại, và báo game
     * đổi kích thước khi thanh địa chỉ của trình duyệt (Samsung Internet, Chrome...) ẩn/hiện.
     */
    @JSBody(script = ""
        + "var st = document.createElement('style');\n"
        + "st.textContent = 'html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:#000;'\n"
        + "  + 'overscroll-behavior:none;-webkit-text-size-adjust:100%;}'\n"
        + "  + 'body{position:fixed;inset:0;touch-action:none;}'\n"
        + "  + 'canvas{display:block;touch-action:none;outline:none;}';\n"
        + "document.head.appendChild(st);\n"
        + "var vp = document.querySelector('meta[name=viewport]');\n"
        + "if (!vp) { vp = document.createElement('meta'); vp.name = 'viewport'; document.head.appendChild(vp); }\n"
        + "vp.content = 'width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover';\n"
        + "var pending = false;\n"
        + "function fit() {\n"
        + "  window.scrollTo(0, 0);\n"
        + "  if (pending) return;\n"
        + "  pending = true;\n"
        + "  setTimeout(function () { pending = false; window.scrollTo(0, 0); window.dispatchEvent(new Event('resize')); }, 120);\n"
        + "}\n"
        + "if (window.visualViewport) {\n"
        + "  window.visualViewport.addEventListener('resize', fit);\n"
        + "  window.visualViewport.addEventListener('scroll', function () { window.scrollTo(0, 0); });\n"
        + "}\n"
        + "window.addEventListener('orientationchange', fit);\n"
        + "window.addEventListener('pageshow', fit);\n"
        + "document.addEventListener('fullscreenchange', fit);\n"
    )
    private static native void setupPageLayout();


    @JSBody(script = "return window.matchMedia ? window.matchMedia('(pointer: coarse)').matches : (('ontouchstart' in window) || (navigator.maxTouchPoints || 0) > 0);")
    private static native boolean isTouchDevice();

    /**
     * Hộp nhập chữ HTML bất đồng bộ (không khóa luồng JS như window.prompt, tránh đứt nhịp ping Socket.IO 5s).
     * Gọi cb.onResult(null) khi người chơi bấm Hủy / Esc / chạm ra ngoài.
     */
    @JSBody(params = {"title", "current", "maxLen", "cb"}, script = ""
        + "var old = document.getElementById('se-text-prompt');\n"
        + "if (old && old.parentNode) old.parentNode.removeChild(old);\n"
        + "if (!document.getElementById('se-text-prompt-css')) {\n"
        + "  var st = document.createElement('style'); st.id = 'se-text-prompt-css';\n"
        + "  st.textContent = '#se-text-prompt{position:fixed;inset:0;z-index:100000;display:flex;align-items:flex-start;justify-content:center;padding:18px;background:rgba(6,5,4,.78);font-family:system-ui,-apple-system,\"Segoe UI\",Roboto,sans-serif;}'\n"
        + "    + '#se-text-prompt .box{width:min(440px,100%);margin-top:8vh;background:#16120f;color:#f2ebdf;border:2px solid #e2b65a;border-radius:12px;padding:16px;box-shadow:0 16px 36px rgba(0,0,0,.65);}'\n"
        + "    + '#se-text-prompt label{display:block;margin:0 0 10px;font:700 1rem/1.3 system-ui,sans-serif;color:#f7e3b3;}'\n"
        + "    + '#se-text-prompt input{box-sizing:border-box;width:100%;padding:11px 12px;border-radius:8px;border:2px solid #e2b65a;background:#0d0a08;color:#fff;font:600 16px/1.3 system-ui,sans-serif;outline:none;}'\n"
        + "    + '#se-text-prompt .acts{display:flex;gap:10px;margin-top:12px;}'\n"
        + "    + '#se-text-prompt button{flex:1;min-height:42px;border-radius:8px;font:700 .95rem/1.2 system-ui,sans-serif;cursor:pointer;}'\n"
        + "    + '#se-text-prompt .ok{background:linear-gradient(180deg,#f7e3b3,#edcf8e);color:#1a1208;border:2px solid transparent;}'\n"
        + "    + '#se-text-prompt .cancel{background:rgba(226,182,90,.1);color:#f2ebdf;border:2px solid #e2b65a;}';\n"
        + "  document.head.appendChild(st);\n"
        + "}\n"
        + "var overlay = document.createElement('div'); overlay.id = 'se-text-prompt';\n"
        + "var box = document.createElement('div'); box.className = 'box';\n"
        + "var lbl = document.createElement('label'); lbl.textContent = title || 'Nhập chữ';\n"
        + "var inp = document.createElement('input'); inp.type = 'text'; inp.value = current || '';\n"
        + "if (maxLen > 0) inp.maxLength = maxLen;\n"
        + "var acts = document.createElement('div'); acts.className = 'acts';\n"
        + "var btnCancel = document.createElement('button'); btnCancel.type = 'button'; btnCancel.className = 'cancel'; btnCancel.textContent = 'Hủy';\n"
        + "var btnOk = document.createElement('button'); btnOk.type = 'button'; btnOk.className = 'ok'; btnOk.textContent = 'Xong';\n"
        + "acts.appendChild(btnCancel); acts.appendChild(btnOk);\n"
        + "box.appendChild(lbl); box.appendChild(inp); box.appendChild(acts); overlay.appendChild(box);\n"
        + "document.body.appendChild(overlay);\n"
        + "var done = false;\n"
        + "function finish(val) {\n"
        + "  if (done) return; done = true;\n"
        + "  if (overlay.parentNode) overlay.parentNode.removeChild(overlay);\n"
        + "  var c = document.querySelector('canvas'); if (c && c.focus) c.focus();\n"
        + "  cb(val);\n"
        + "}\n"
        + "btnOk.addEventListener('click', function () { finish(String(inp.value)); });\n"
        + "btnCancel.addEventListener('click', function () { finish(null); });\n"
        + "overlay.addEventListener('click', function (e) { if (e.target === overlay) finish(null); });\n"
        + "inp.addEventListener('keydown', function (e) {\n"
        + "  e.stopPropagation();\n"
        + "  if (e.key === 'Enter') { e.preventDefault(); finish(String(inp.value)); }\n"
        + "  else if (e.key === 'Escape') { e.preventDefault(); finish(null); }\n"
        + "});\n"
        + "setTimeout(function () { inp.focus(); try { inp.setSelectionRange(inp.value.length, inp.value.length); } catch (e) {} }, 20);")
    private static native void askTextAsync(String title, String current, int maxLen, TextCallback cb);

    @JSBody(script = "window.location.href = '../';")
    private static native void goHome();

    @JSBody(script = ""
        + "document.title = 'Shadow Edict: Ám Lệnh – Chơi trên trình duyệt';\n"
        + "var old = document.querySelectorAll('link[rel~=\\\"icon\\\"]');\n"
        + "for (var i = 0; i < old.length; i++) old[i].parentNode.removeChild(old[i]);\n"
        + "var l = document.createElement('link'); l.rel = 'icon'; l.type = 'image/png';\n"
        + "l.href = '../assets/img/favicon.png'; document.head.appendChild(l);\n"
        + "var a = document.createElement('link'); a.rel = 'apple-touch-icon';\n"
        + "a.href = '../assets/img/game-icon.png'; document.head.appendChild(a);\n")
    private static native void setupPageIcon();
}
