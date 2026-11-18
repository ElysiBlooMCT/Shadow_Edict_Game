(function() {
    const header = document.querySelector(".site-header");
    const wrap = header && header.querySelector(".nav-wrap");
    const nav = wrap && wrap.querySelector("nav");
    if (!nav || header.querySelector(".menu-toggle")) return;
    nav.id = nav.id || "site-menu";
    const button = document.createElement("button");
    button.type = "button";
    button.className = "menu-toggle";
    button.setAttribute("aria-label", "Mở menu");
    button.setAttribute("aria-controls", nav.id);
    button.setAttribute("aria-expanded", "false");
    for (let i = 0; i < 3; i += 1) button.append(document.createElement("span"));
    wrap.prepend(button);
    function setOpen(open) {
        header.classList.toggle("menu-open", open);
        button.setAttribute("aria-expanded", String(open));
        button.setAttribute("aria-label", open ? "Đóng menu" : "Mở menu");
    }
    button.addEventListener("click", () => setOpen(!header.classList.contains("menu-open")));
    nav.addEventListener("click", event => {
        if (event.target.closest("a")) setOpen(false);
    });
    document.addEventListener("keydown", event => {
        if (event.key === "Escape") setOpen(false);
    });
    document.addEventListener("click", event => {
        if (!header.contains(event.target)) setOpen(false);
    });
    window.matchMedia("(min-width: 901px)").addEventListener("change", event => {
        if (event.matches) setOpen(false);
    });
})();