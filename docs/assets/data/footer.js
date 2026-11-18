/*
  CHÂN TRANG (hiện ở cuối mọi trang) — giữ giống web ElysiBlooMCT.

  text: dòng chữ chính. Gõ {year} để tự chèn năm hiện tại.
  links: các link nhỏ cạnh chữ. url dạng "/faq/" (trang trong web) hoặc "https://..." (link ngoài).
         Để trống [] nếu không muốn có link.
  logo: true/false = hiện logo ElysiBlooMCT nhỏ ở chân trang (file assets/logo.png).
  backToTop: true = hiện nút "Lên đầu trang", false = ẩn.
  note: dòng chữ nhỏ bên dưới (để trống "" nếu không cần).
*/
SITE.footer = {
  text: "© {year} ElysiBlooMCT. Crafted with pink magic.",
  links: [
    { label: "Server Minecraft", url: "https://mc.elysibloomct.qzz.io/" },
    { label: "GitHub", url: "https://github.com/ElysiBlooMCT/Shadow_Edict_Game" },
  ],
  note: "Website và một phần mã nguồn game được thực hiện với sự hỗ trợ của AI (Claude).",
  backToTop: true,
  logo: true,
};
