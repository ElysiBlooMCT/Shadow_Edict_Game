package io.qzz.elysibloomct.ctv.se.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.github.xpenatan.gdx.teavm.backends.web.WebPreloadApplicationListener;
import io.qzz.elysibloomct.ctv.se.Main;
import io.qzz.elysibloomct.ctv.se.NetworkManager;
import io.qzz.elysibloomct.ctv.se.PortraitRotation;
import org.teavm.jso.JSBody;

/** Chạy game trên trình duyệt (gdx-teavm). */
public class WebLauncher {
    public static void main(String[] args) {
        // Online trên web dùng thư viện socket.io bản JavaScript, cùng server với PC/Android.
        NetworkManager.socketFactory = WebGameSocket::new;
        // Điện thoại để dọc (khóa xoay) vẫn thấy game nằm ngang.
        PortraitRotation.enabled = true;

        // Nút THOÁT trong game: về trang chủ website (trình duyệt không cho trang tự đóng tab).
        Main.exitHandler = WebLauncher::goHome;

        // Biểu tượng và tên trên tab trình duyệt (trang do gdx-teavm tạo không có sẵn).
        setupPageIcon();

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
