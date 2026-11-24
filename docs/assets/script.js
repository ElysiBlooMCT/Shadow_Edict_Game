const ASSETS = (() => {
    const src = document.currentScript && document.currentScript.src;
    return src ? new URL(".", src).pathname : "/assets/";
})();

const ROOT = ASSETS.replace(/[^/]+\/$/, "");

const ASSET_V = (() => {
    const src = document.currentScript && document.currentScript.src;
    return src ? new URL(src).searchParams.get("v") || "" : "";
})();

window.SITE = window.SITE || {};

const SITE_DATA = window.SITE;

let SERVER = {};

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

function siteUrl(path) {
    if (typeof path === "string" && path.startsWith("/") && !path.startsWith("//")) {
        return ROOT + path.slice(1);
    }
    return path;
}

function loadScriptFile(path) {
    return new Promise(resolve => {
        const script = document.createElement("script");
        script.src = `${ASSETS}${path}${ASSET_V ? "?v=" + ASSET_V : ""}`;
        script.onload = script.onerror = () => resolve();
        document.head.append(script);
    });
}

function loadData(name) {
    return loadScriptFile(`data/${name}.js`);
}

function fill(value) {
    return String(value).replace(/\{ip\}/g, SERVER.ip ?? "").replace(/\{port\}/g, SERVER.bedrockPort ?? "").replace(/\{name\}/g, SERVER.name ?? "");
}

function richText(parent, value) {
    fill(value).split("**").forEach((part, index) => {
        if (!part) return;
        parent.append(index % 2 === 1 ? el("strong", "", part) : document.createTextNode(part));
    });
    return parent;
}

function linkButton(button) {
    const style = button.style === "secondary" ? "secondary" : "primary";
    const link = el("a", `btn btn-${style}`, button.label || "Xem thêm");
    link.href = siteUrl(button.url || "#");
    if (/^https?:\/\//i.test(String(button.url || ""))) {
        link.target = "_blank";
        link.rel = "noopener noreferrer";
    }
    return link;
}

function renderNote(parent, note) {
    if (note) parent.append(richText(el("p", "note"), note));
}

function renderSteps(steps, className) {
    const list = el("ol", className);
    steps.forEach(step => list.append(richText(el("li"), step)));
    return list;
}

function renderAbout(data) {
    const fragment = document.createDocumentFragment();
    [].concat(data.text || []).forEach(paragraph => fragment.append(richText(el("p"), paragraph)));
    return fragment;
}

function renderCards(data) {
    const grid = el("div", data.columns === 2 ? "card-grid card-grid-2" : "card-grid");
    (data.items || []).forEach(item => {
        const card = el("article", "card");
        if (item.icon) {
            const icon = el("span", "card-icon", item.icon);
            icon.setAttribute("aria-hidden", "true");
            card.append(icon);
        }
        if (item.image) {
            const shot = el("button", "card-image");
            shot.type = "button";
            const img = el("img");
            img.src = `${ASSETS}${item.image}`;
            img.alt = fill(item.title || "");
            img.loading = "lazy";
            img.width = 640;
            img.height = 360;
            shot.append(img);
            shot.addEventListener("click", () => openLightbox([ { src: item.imageFull || item.image, title: item.title, text: item.caption } ], 0));
            card.classList.add("card-has-image");
            card.prepend(shot);
        }
        card.append(el("h3", "", fill(item.title || "")));
        if (item.text) card.append(richText(el("p"), item.text));
        if (item.buttons && item.buttons.length) {
            const actions = el("div", "card-actions");
            item.buttons.forEach(button => actions.append(linkButton(button)));
            card.append(actions);
        }
        grid.append(card);
    });
    return grid;
}

function renderFeatures(data) {
    const list = el("ul", "feature-list");
    (data.items || []).forEach(item => list.append(richText(el("li"), item)));
    return list;
}

function renderStats(data) {
    const fragment = document.createDocumentFragment();
    const grid = el("div", "card-grid stats-grid");
    (data.items || []).forEach(item => {
        const card = el("article", "card stat");
        card.append(el("p", "stat-label", fill(item.label || "")), el("p", "stat-value", fill(item.value || "")));
        grid.append(card);
    });
    fragment.append(grid);
    renderNote(fragment, data.note);
    return fragment;
}

function renderJoin(data) {
    const fragment = document.createDocumentFragment();
    const grid = el("div", "join-grid");
    (data.platforms || []).forEach(platform => {
        const card = el("article", "join-card");
        card.append(el("h3", "", fill(platform.name || "")), renderSteps(platform.steps || [], "steps"));
        grid.append(card);
    });
    fragment.append(grid);
    renderNote(fragment, data.note);
    return fragment;
}

function renderRules(data) {
    const fragment = document.createDocumentFragment();
    fragment.append(renderSteps(data.items || [], "steps steps-grid"));
    renderNote(fragment, data.note);
    return fragment;
}

function renderTeam(data) {
    const grid = el("div", "card-grid");
    (data.members || []).forEach(member => {
        const card = el("article", "card member");
        const avatar = el("span", "avatar");
        avatar.setAttribute("aria-hidden", "true");
        const hint = member.avatar || "";
        if (/[./]/.test(hint)) {
            const image = el("img");
            image.src = siteUrl(hint.startsWith("http") || hint.startsWith("/") ? hint : `/${hint}`);
            image.alt = "";
            avatar.append(image);
        } else {
            avatar.textContent = hint || (member.name || "?").charAt(0).toUpperCase();
        }
        card.append(avatar, el("h3", "", member.name || ""));
        if (member.role) card.append(el("p", "role", member.role));
        grid.append(card);
    });
    return grid;
}

const CHANGE_TAGS = {
    new: [ "tag-new", "Mới" ],
    fix: [ "tag-fix", "Sửa lỗi" ],
    balance: [ "tag-balance", "Cân bằng" ],
    change: [ "tag-change", "Thay đổi" ],
    remove: [ "tag-remove", "Gỡ bỏ" ]
};

function renderChangelog(data) {
    const list = el("ol", "changelog");
    (data.entries || []).forEach(entry => {
        const item = el("li", "log-item");
        const head = el("div", "log-head");
        head.append(el("span", "log-version", entry.version || ""));
        if (entry.title) head.append(el("span", "log-title", entry.title));
        if (entry.date) head.append(el("span", "log-date", entry.date));
        const changes = el("ul", "log-list");
        (entry.changes || []).forEach(([type, text]) => {
            const [tagClass, tagLabel] = CHANGE_TAGS[type] || [ "tag-change", String(type) ];
            const row = el("li");
            row.append(el("span", `tag ${tagClass}`, tagLabel), richText(el("span", "log-text"), text || ""));
            changes.append(row);
        });
        item.append(head, changes);
        list.append(item);
    });
    return list;
}

function renderFaq(data) {
    const wrap = el("div", "faq-list");
    (data.items || []).forEach(item => {
        const details = el("details");
        details.append(el("summary", "", fill(item.q || "")), richText(el("p"), item.a || ""));
        wrap.append(details);
    });
    return wrap;
}

const RELEASE = {
    promise: null,
    get() {
        const repo = SITE_DATA.release && SITE_DATA.release.repo;
        if (!repo) return Promise.resolve(null);
        if (!this.promise) {
            this.promise = fetch(`https://api.github.com/repos/${repo}/releases/latest`, {
                headers: {
                    Accept: "application/vnd.github+json"
                }
            }).then(response => response.ok ? response.json() : null).catch(() => null);
        }
        return this.promise;
    },
    page() {
        const repo = SITE_DATA.release && SITE_DATA.release.repo;
        return repo ? `https://github.com/${repo}/releases/latest` : "#";
    }
};

function detectOs() {
    const ua = navigator.userAgent || "";
    if (/Android/i.test(ua)) return "android";
    if (/iPhone|iPad|iPod/i.test(ua) || (/Mac/i.test(ua) && navigator.maxTouchPoints > 1)) return "ios";
    if (/Windows/i.test(ua)) return "windows";
    if (/Mac/i.test(ua)) return "mac";
    if (/Linux/i.test(ua)) return "linux";
    return "";
}

function formatSize(bytes) {
    return bytes >= 1048576 ? `${Math.round(bytes / 1048576)} MB` : `${Math.round(bytes / 1024)} KB`;
}

function formatDate(value) {
    const date = new Date(value);
    return isNaN(date) ? "" : date.toLocaleDateString("vi-VN");
}

function renderDownload(data) {
    const fragment = document.createDocumentFragment();
    const os = detectOs();
    const grid = el("div", "card-grid dl-grid");
    const cards = [];
    (data.platforms || []).forEach(platform => {
        const card = el("a", "card dl-card");
        if (platform.url) {
            card.href = siteUrl(platform.url);
        } else {
            card.href = RELEASE.page();
            card.target = "_blank";
            card.rel = "noopener noreferrer";
        }
        const icon = el("span", "card-icon", platform.icon || "✦");
        icon.setAttribute("aria-hidden", "true");
        const body = el("span", "dl-body");
        const meta = el("span", "dl-meta", fill(platform.text || ""));
        body.append(el("span", "dl-name", fill(platform.name || "")), meta);
        card.append(icon, body);
        if (platform.os && platform.os === os) {
            card.classList.add("dl-recommended");
            card.append(el("span", "dl-badge", data.yourDevice || "Máy của bạn"));
        }
        grid.append(card);
        cards.push({ card, meta, platform });
    });
    const status = el("p", "note dl-status", data.loading || "");
    status.setAttribute("aria-live", "polite");
    fragment.append(grid, status);
    if (os === "ios" && data.iosNote) status.textContent = fill(data.iosNote);
    RELEASE.get().then(release => {
        if (!release) {
            if (os === "ios" && data.iosNote) return;
            status.replaceChildren(document.createTextNode(fill(data.failed || "") + " "));
            const link = el("a", "", data.allVersions || "Releases");
            link.href = RELEASE.page();
            link.target = "_blank";
            link.rel = "noopener noreferrer";
            status.append(link);
            return;
        }
        const assets = release.assets || [];
        cards.forEach(({ card, meta, platform }) => {
            const suffix = platform.suffix || "";
            const asset = suffix && assets.find(item => item.name.endsWith(suffix));
            if (!asset) return;
            card.href = asset.browser_download_url;
            card.removeAttribute("target");
            meta.textContent = `${fill(platform.text || "")} · ${formatSize(asset.size)}`;
        });
        if (os !== "ios") {
            const version = String(release.tag_name || "").replace(/^v/, "");
            status.textContent = fill(data.ready || "").replace(/\{version\}/g, version).replace(/\{date\}/g, formatDate(release.published_at));
        }
    });
    if (data.guides && data.guides.length) {
        const guides = renderJoin({ platforms: data.guides });
        const box = el("div", "dl-guides");
        box.append(guides);
        fragment.append(box);
    }
    return fragment;
}

function fillRelease() {
    const versionNode = document.getElementById("release-version");
    const dateNode = document.getElementById("release-date");
    const button = document.getElementById("hero-download");
    if (!versionNode && !button) return;
    const conf = SITE_DATA.release || {};
    const os = detectOs();
    RELEASE.get().then(release => {
        if (!release) {
            if (versionNode) {
                const link = el("a", "", "Xem bản mới nhất");
                link.href = RELEASE.page();
                link.target = "_blank";
                link.rel = "noopener noreferrer";
                versionNode.replaceChildren(link);
            }
            if (dateNode) dateNode.textContent = "—";
            return;
        }
        const version = String(release.tag_name || "").replace(/^v/, "");
        if (versionNode) versionNode.textContent = `v${version}`;
        if (dateNode) dateNode.textContent = formatDate(release.published_at);
        const pick = (conf.heroAssets || {})[os];
        const asset = pick && (release.assets || []).find(item => item.name.endsWith(pick.suffix));
        if (button && asset) {
            button.href = asset.browser_download_url;
            button.textContent = `${pick.label} · v${version}`;
        }
    });
}

let LIGHTBOX = null;

function openLightbox(items, index) {
    if (typeof HTMLDialogElement === "undefined" || typeof HTMLDialogElement.prototype.showModal !== "function") {
        window.open(`${ASSETS}${items[index].src}`, "_blank", "noopener");
        return;
    }
    if (!LIGHTBOX) {
        const dialog = el("dialog", "lightbox");
        dialog.setAttribute("aria-label", "Xem ảnh");
        const figure = el("figure");
        const img = el("img");
        const caption = el("figcaption");
        figure.append(img, caption);
        const close = el("button", "lightbox-close", "×");
        close.type = "button";
        close.setAttribute("aria-label", "Đóng");
        const prev = el("button", "lightbox-nav lightbox-prev", "‹");
        prev.type = "button";
        prev.setAttribute("aria-label", "Ảnh trước");
        const next = el("button", "lightbox-nav lightbox-next", "›");
        next.type = "button";
        next.setAttribute("aria-label", "Ảnh sau");
        dialog.append(figure, close, prev, next);
        document.body.append(dialog);
        LIGHTBOX = { dialog, img, caption, prev, next, items: [], index: 0 };
        const show = step => {
            const box = LIGHTBOX;
            box.index = (box.index + step + box.items.length) % box.items.length;
            const item = box.items[box.index];
            box.img.src = `${ASSETS}${item.src}`;
            box.img.alt = fill(item.title || "");
            box.caption.replaceChildren();
            if (item.title) box.caption.append(el("strong", "", fill(item.title)));
            if (item.text) box.caption.append(richText(el("span"), item.text));
            const many = box.items.length > 1;
            box.prev.hidden = !many;
            box.next.hidden = !many;
        };
        LIGHTBOX.show = show;
        close.addEventListener("click", () => dialog.close());
        prev.addEventListener("click", () => show(-1));
        next.addEventListener("click", () => show(1));
        dialog.addEventListener("click", event => {
            if (event.target === dialog) dialog.close();
        });
        dialog.addEventListener("keydown", event => {
            if (LIGHTBOX.items.length <= 1) return;
            if (event.key === "ArrowLeft") show(-1);
            if (event.key === "ArrowRight") show(1);
        });
    }
    LIGHTBOX.items = items;
    LIGHTBOX.index = index;
    LIGHTBOX.show(0);
    LIGHTBOX.dialog.showModal();
}

function renderGallery(data, section) {
    const fragment = document.createDocumentFragment();
    const all = (data.items || []).map(item => ({ src: item.full || item.thumb, title: item.title, text: item.text }));
    const limit = parseInt(section && section.dataset.limit, 10) || all.length;
    const grid = el("div", "gallery-grid");
    (data.items || []).slice(0, limit).forEach((item, index) => {
        const tile = el("button", "gallery-item");
        tile.type = "button";
        const img = el("img");
        img.src = `${ASSETS}${item.thumb || item.full}`;
        img.alt = fill(item.title || "");
        img.loading = "lazy";
        img.width = 640;
        img.height = 360;
        const label = el("span", "gallery-label", fill(item.title || ""));
        tile.append(img, label);
        tile.addEventListener("click", () => openLightbox(all, index));
        grid.append(tile);
    });
    fragment.append(grid);
    if (limit < all.length && data.more) {
        const actions = el("div", "card-actions");
        actions.append(linkButton(data.more));
        fragment.append(actions);
    }
    return fragment;
}

const MODULE_RENDERERS = {
    home: renderCards,
    about: renderAbout,
    modes: renderCards,
    features: renderFeatures,
    stats: renderStats,
    join: renderJoin,
    rules: renderRules,
    team: renderTeam,
    changelog: renderChangelog,
    support: renderCards,
    community: renderCards,
    faq: renderFaq,
    download: renderDownload,
    controls: renderJoin,
    story: renderAbout,
    gallery: renderGallery
};

function currentSlug() {
    const path = location.pathname;
    const relative = path.startsWith(ROOT) ? path.slice(ROOT.length) : path.replace(/^\//, "");
    return relative.replace(/index\.html$/, "").replace(/\/+$/, "");
}

const slugOf = url => String(url || "").replace(/^\/+|\/+$/g, "");

function redirectLegacyHash() {
    const legacy = location.hash.slice(1);
    if (!legacy || currentSlug() !== "") return false;
    const alias = {
        features: "about",
        stats: "about"
    };
    const target = alias[legacy] || legacy;
    const hit = (SITE_DATA.nav || []).find(item => slugOf(item.url) === target);
    if (!hit) return false;
    location.replace(siteUrl(hit.url));
    return true;
}

function renderShell() {
    const name = SERVER.name || "ElysiBlooMCT";
    const header = document.querySelector(".site-header");
    if (header) {
        const wrap = el("div", "container nav-wrap");
        const brand = el("a", "brand");
        brand.href = ROOT;
        brand.setAttribute("aria-label", `${name} trang chủ`);
        const brandConf = SITE_DATA.brand || {};
        if (brandConf.logo) {
            const glogo = el("img", "brand-logo brand-logo-game");
            glogo.src = `${ASSETS}${brandConf.logo}`;
            glogo.width = brandConf.width || 720;
            glogo.height = brandConf.height || 209;
            glogo.alt = name;
            brand.append(glogo);
        } else if (brandConf.icon) {
            const icon = el("img", "brand-icon");
            icon.src = `${ASSETS}${brandConf.icon}`;
            icon.width = 128;
            icon.height = 128;
            icon.alt = "";
            brand.classList.add("brand-game");
            brand.append(icon, el("span", "brand-text", brandConf.text || name));
        } else {
            const logo = el("img", "brand-logo");
            logo.src = `${ASSETS}logo.png`;
            logo.width = 960;
            logo.height = 181;
            logo.alt = name;
            brand.append(logo);
        }
        const nav = el("nav");
        nav.setAttribute("aria-label", "Điều hướng chính");
        const list = el("ul", "nav-links");
        const here = currentSlug();
        (SITE_DATA.nav || []).forEach(item => {
            const li = el("li");
            const link = el("a", "", item.label || "");
            link.href = siteUrl(item.url || "/");
            if (slugOf(item.url) && slugOf(item.url) === here) link.setAttribute("aria-current", "page");
            li.append(link);
            list.append(li);
        });
        nav.append(list);
        wrap.append(brand, nav);
        header.append(wrap);
    }
    const footer = document.querySelector(".site-footer");
    if (footer) {
        const conf = SITE_DATA.footer || {};
        const wrap = el("div", "container footer-wrap");
        const brandBox = el("div", "footer-brand");
        if (conf.logo !== false) {
            const flogo = el("img", "footer-logo");
            flogo.src = `${ASSETS}logo.png`;
            flogo.width = 960;
            flogo.height = 181;
            flogo.alt = name;
            brandBox.append(flogo);
        }
        const text = conf.text || "© {year} {name}.";
        brandBox.append(el("p", "", fill(text).replace(/\{year\}/g, String((new Date).getFullYear()))));
        if (conf.note) brandBox.append(richText(el("p", "footer-note"), conf.note));
        wrap.append(brandBox);
        const links = el("nav", "footer-links");
        links.setAttribute("aria-label", "Liên kết chân trang");
        (Array.isArray(conf.links) ? conf.links : []).forEach(item => {
            if (!item || !item.label) return;
            const a = el("a", "", item.label);
            a.href = siteUrl(item.url || "#");
            if (/^https?:\/\//.test(a.getAttribute("href"))) {
                a.target = "_blank";
                a.rel = "noopener noreferrer";
            }
            links.append(a);
        });
        if (conf.backToTop !== false) {
            const top = el("a", "", "Lên đầu trang");
            top.href = "#top";
            links.append(top);
        }
        wrap.append(links);
        footer.append(wrap);
    }
}

function buildModules() {
    document.querySelectorAll("[data-module]").forEach(section => {
        const name = section.dataset.module;
        const data = SITE_DATA[name];
        const render = MODULE_RENDERERS[name];
        if (!data || !render) {
            section.hidden = true;
            console.warn(`Module "${name}" bị ẩn: thiếu data/${name}.js hoặc file đang bị lỗi cú pháp.`);
            return;
        }
        try {
            const container = el("div", "container");
            if (data.title) {
                const heading = el(section.dataset.heading === "h1" ? "h1" : "h2", "section-title", fill(data.title));
                heading.id = `${name}-title`;
                container.append(heading);
                section.setAttribute("aria-labelledby", heading.id);
            }
            container.append(render(data, section));
            section.id = name;
            section.append(container);
        } catch (error) {
            section.hidden = true;
            section.replaceChildren();
            console.error(`Module "${name}" bị ẩn vì lỗi nội dung:`, error);
        }
    });
}

function fillHero() {
    const ip = document.getElementById("server-ip");
    if (ip && SERVER.ip) ip.textContent = SERVER.ip;
    const port = document.getElementById("bedrock-port");
    if (port && SERVER.bedrockPort) port.textContent = SERVER.bedrockPort;
    const chips = document.querySelector(".panel-meta");
    if (chips && Array.isArray(SERVER.chips)) {
        chips.replaceChildren(...SERVER.chips.map(label => el("li", "", label)));
    }
}

function initCopyButton() {
    const copyButton = document.getElementById("copy-ip-btn");
    const copyStatus = document.getElementById("copy-status");
    const serverIpNode = document.getElementById("server-ip");
    if (!copyButton || !copyStatus || !serverIpNode) return;
    copyButton.addEventListener("click", async () => {
        const ip = serverIpNode.textContent?.trim() || "";
        if (!ip) {
            copyStatus.textContent = "Không tìm thấy IP.";
            return;
        }
        try {
            await navigator.clipboard.writeText(ip);
            copyStatus.textContent = "Đã copy IP!";
        } catch {
            const tempInput = document.createElement("input");
            tempInput.value = ip;
            document.body.appendChild(tempInput);
            tempInput.select();
            document.execCommand("copy");
            tempInput.remove();
            copyStatus.textContent = "Đã copy IP!";
        }
        window.setTimeout(() => {
            copyStatus.textContent = "";
        }, 2200);
    });
}

function initRevealAnimation() {
    const revealNodes = document.querySelectorAll(".section, .site-footer");
    const reducedMotionQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
    let revealObserver = null;
    function disableRevealAnimation() {
        if (revealObserver) {
            revealObserver.disconnect();
            revealObserver = null;
        }
        revealNodes.forEach(node => {
            node.classList.remove("scroll-reveal");
            node.classList.add("is-visible");
        });
    }
    function enableRevealAnimation() {
        if (!("IntersectionObserver" in window)) {
            disableRevealAnimation();
            return;
        }
        if (revealObserver) {
            revealObserver.disconnect();
        }
        revealObserver = new IntersectionObserver((entries, observer) => {
            entries.forEach(entry => {
                if (!entry.isIntersecting) return;
                entry.target.classList.add("is-visible");
                observer.unobserve(entry.target);
            });
        }, {
            threshold: .12,
            rootMargin: "0px 0px -30px"
        });
        revealNodes.forEach(node => {
            node.classList.add("scroll-reveal");
            node.classList.remove("is-visible");
            revealObserver.observe(node);
        });
    }
    function syncRevealMotionPreference() {
        if (reducedMotionQuery.matches) {
            disableRevealAnimation();
            return;
        }
        enableRevealAnimation();
    }
    syncRevealMotionPreference();
    if (typeof reducedMotionQuery.addEventListener === "function") {
        reducedMotionQuery.addEventListener("change", syncRevealMotionPreference);
    } else if (typeof reducedMotionQuery.addListener === "function") {
        reducedMotionQuery.addListener(syncRevealMotionPreference);
    }
}

async function main() {
    const modules = [ ...document.querySelectorAll("[data-module]") ].map(node => node.dataset.module);
    await Promise.all([ ...new Set([ "site", "footer", ...modules ]) ].map(loadData));
    SERVER = {
        ip: document.getElementById("server-ip")?.textContent.trim(),
        bedrockPort: document.getElementById("bedrock-port")?.textContent.trim(),
        ...SITE_DATA.server
    };
    if (redirectLegacyHash()) return;
    renderShell();
    loadScriptFile("mobile.js");
    fillHero();
    fillRelease();
    buildModules();
    initCopyButton();
    initRevealAnimation();
}

main();