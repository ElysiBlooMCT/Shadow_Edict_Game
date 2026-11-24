package io.qzz.elysibloomct.ctv.se;

import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý đa ngôn ngữ (Localization/I18n) cho game.
 * Hỗ trợ 7 ngôn ngữ quốc tế phổ biến: Tiếng Việt, English, Español, Français, Português, Deutsch, Русский.
 */
public final class LanguageManager {
    public static final String VI = "vi";
    public static final String EN = "en";
    public static final String ES = "es";
    public static final String FR = "fr";
    public static final String PT = "pt";
    public static final String DE = "de";
    public static final String RU = "ru";

    // Chỉ bật các ngôn ngữ mà font bitmap (assets/ui/*.fnt) có đủ ký tự:
    // font hiện chỉ có Latin + tiếng Việt, thiếu ñ ç ä ß và toàn bộ chữ Kirin -> ES/FR/PT/DE/RU hiện thành ô trống.
    // Bản dịch của các ngôn ngữ đó vẫn giữ trong initStrings() để bật lại khi tạo font đủ ký tự.
    public static final String[] SUPPORTED_CODES = { VI, EN };
    public static final String[] DISPLAY_NAMES = {
        "Tiếng Việt (VI)",
        "English (EN)"
    };

    private static String currentLang = VI;
    private static final Map<String, Map<String, String>> STRINGS = new HashMap<>();

    static {
        initStrings();
    }

    private LanguageManager() {}

    public static void init() {
        PlayerProfile profile = PlayerProfile.getInstance();
        if (profile != null && profile.language != null) {
            setLanguage(profile.language);
        }
    }

    public static String getCurrentLanguage() {
        return currentLang;
    }

    public static String getCurrentDisplayName() {
        for (int i = 0; i < SUPPORTED_CODES.length; i++) {
            if (SUPPORTED_CODES[i].equalsIgnoreCase(currentLang)) {
                return DISPLAY_NAMES[i];
            }
        }
        return DISPLAY_NAMES[0];
    }

    public static void setLanguage(String code) {
        if (code == null) return;
        boolean found = false;
        for (String c : SUPPORTED_CODES) {
            if (c.equalsIgnoreCase(code)) {
                currentLang = c;
                found = true;
                break;
            }
        }
        if (!found) currentLang = VI;
        PlayerProfile p = PlayerProfile.getInstance();
        if (p != null) {
            p.language = currentLang;
        }
    }

    public static String nextLanguage() {
        int idx = 0;
        for (int i = 0; i < SUPPORTED_CODES.length; i++) {
            if (SUPPORTED_CODES[i].equalsIgnoreCase(currentLang)) {
                idx = i;
                break;
            }
        }
        idx = (idx + 1) % SUPPORTED_CODES.length;
        setLanguage(SUPPORTED_CODES[idx]);
        return getCurrentDisplayName();
    }

    public static String get(String key) {
        Map<String, String> langMap = STRINGS.get(currentLang);
        if (langMap != null && langMap.containsKey(key)) {
            return langMap.get(key);
        }
        // Fallback sang tiếng Anh hoặc tiếng Việt
        Map<String, String> enMap = STRINGS.get(EN);
        if (enMap != null && enMap.containsKey(key)) {
            return enMap.get(key);
        }
        Map<String, String> viMap = STRINGS.get(VI);
        if (viMap != null && viMap.containsKey(key)) {
            return viMap.get(key);
        }
        return key;
    }

    private static void put(String lang, String key, String value) {
        STRINGS.computeIfAbsent(lang, k -> new HashMap<>()).put(key, value);
    }

    private static void initStrings() {
        // === 1. TIẾNG VIỆT (VI) ===
        put(VI, "SETTINGS_TITLE", "CÀI ĐẶT & BÀN PHÍM");
        put(VI, "LANGUAGE", "Ngôn ngữ");
        put(VI, "MUSIC", "Nhạc nền");
        put(VI, "SFX", "Hiệu ứng");
        put(VI, "ON", "BẬT");
        put(VI, "OFF", "TẮT");
        put(VI, "SAVE_CLOSE", "LƯU & ĐÓNG (Esc)");
        put(VI, "SAVE_CLOSE_ENTER", "▸ LƯU & ĐÓNG (Enter/Esc) ‹");
        put(VI, "CONTROLS_GUIDE",
            "DI CHUYỂN & PARKOUR: A/D hoặc ←/→ chạy • W/Space/Lên nhảy & leo lên • S/Xuống ngồi/đỡ đòn & chui thấp • R bám tường\n" +
            "CHIẾN ĐẤU: J hoặc Ctrl đánh • L/Shift lướt • Z/X/C (hoặc I/O/Y) kỹ năng 1/2/3 • U tuyệt kỹ • K ưng kích • F/Q ám sát\n" +
            "VŨ KHÍ & TIỆN ÍCH: Tab hoặc 1–5 đổi vũ khí • E tương tác • B tiệm đồ • H uống thuốc • F1 ẩn/hiện nút ảo\n" +
            "TRỰC TUYẾN: T mở chat • Giữ V bộ đàm / M bật-tắt mic • P/Esc tạm dừng trận đấu");
        put(VI, "PLAY_STORY", "CỐT TRUYỆN");
        put(VI, "CUSTOM_MAP", "CHỌN MÀN CHƠI");
        put(VI, "MULTIPLAYER", "CHƠI MẠNG");
        put(VI, "FRIENDS", "Bạn bè");
        put(VI, "SELECT_HERO", "CHỌN SÁT THỦ");
        put(VI, "START_BATTLE", "VÀO TRẬN");
        put(VI, "CONTINUE", "CHƠI TIẾP");
        put(VI, "NEW_GAME", "CHƠI MỚI");
        put(VI, "BACK", "QUAY LẠI");
        put(VI, "PAUSED", "TẠM DỪNG");
        put(VI, "RESUME", "TIẾP TỤC");
        put(VI, "VICTORY", "CHIẾN THẮNG");
        put(VI, "DEFEAT", "THẤT BẠI");

        // === 2. ENGLISH (EN) ===
        put(EN, "SETTINGS_TITLE", "SETTINGS & CONTROLS");
        put(EN, "LANGUAGE", "Language");
        put(EN, "MUSIC", "Music (BGM)");
        put(EN, "SFX", "Sound (SFX)");
        put(EN, "ON", "ON");
        put(EN, "OFF", "OFF");
        put(EN, "SAVE_CLOSE", "SAVE & CLOSE (Esc)");
        put(EN, "SAVE_CLOSE_ENTER", "▸ SAVE & CLOSE (Enter/Esc) ‹");
        put(EN, "CONTROLS_GUIDE",
            "MOVEMENT & PARKOUR: A/D or ←/→ Run • W/Space/Up Jump & Climb • S/Down Crouch/Block & Crawl • R Wall Cling\n" +
            "COMBAT: J or Ctrl Attack • L/Shift Dash • Z/X/C (or I/O/Y) Skills 1/2/3 • U Ultimate • K Eagle Strike • F/Q Assassinate\n" +
            "WEAPONS & UTILITY: Tab or 1–5 Switch Weapon • E Interact • B Shop • H Potion • F1 Virtual Buttons\n" +
            "MULTIPLAYER: T Open Chat • Hold V Voice / M Toggle Mic • P/Esc Pause Game");
        put(EN, "PLAY_STORY", "STORY CAMPAIGN");
        put(EN, "CUSTOM_MAP", "CUSTOM MAP");
        put(EN, "MULTIPLAYER", "MULTIPLAYER");
        put(EN, "FRIENDS", "Friends");
        put(EN, "SELECT_HERO", "SELECT ASSASSIN");
        put(EN, "START_BATTLE", "ENTER BATTLE");
        put(EN, "CONTINUE", "CONTINUE");
        put(EN, "NEW_GAME", "NEW CAMPAIGN");
        put(EN, "BACK", "BACK");
        put(EN, "PAUSED", "PAUSED");
        put(EN, "RESUME", "RESUME");
        put(EN, "VICTORY", "VICTORY");
        put(EN, "DEFEAT", "DEFEATED");

        // === 3. ESPAÑOL (ES) ===
        put(ES, "SETTINGS_TITLE", "CONFIGURACIÓN Y CONTROLES");
        put(ES, "LANGUAGE", "Idioma");
        put(ES, "MUSIC", "Música (BGM)");
        put(ES, "SFX", "Efectos (SFX)");
        put(ES, "ON", "SÍ");
        put(ES, "OFF", "NO");
        put(ES, "SAVE_CLOSE", "GUARDAR Y SALIR (Esc)");
        put(ES, "SAVE_CLOSE_ENTER", "► GUARDAR Y SALIR (Enter/Esc) ◄");
        put(ES, "CONTROLS_GUIDE",
            "MOVIMIENTO Y PARKOUR: A/D o ←/→ Correr • W/Espacio/↑ Saltar • S/↓ Agacharse/Arrastrarse • R Trepar\n" +
            "COMBATE: J o Ctrl Atacar • L/Shift Deslizar • Z/X/C Habilidades 1/2/3 • U Definitiva • F Asesinato\n" +
            "ARMAS Y OBJETOS: Tab o 1–5 Cambiar Arma • E Interactuar • B Tienda • H Poción de Salud\n" +
            "EN LÍNEA: T Chat • V Voz / M Micrófono • P/Esc Pausa");
        put(ES, "PLAY_STORY", "MODO HISTORIA");
        put(ES, "CUSTOM_MAP", "ELEGIR MAPA");
        put(ES, "MULTIPLAYER", "MULTIJUGADOR");
        put(ES, "FRIENDS", "Amigos");
        put(ES, "SELECT_HERO", "ELEGIR ASESINO");
        put(ES, "START_BATTLE", "A LA BATALLA");
        put(ES, "CONTINUE", "CONTINUAR");
        put(ES, "NEW_GAME", "NUEVA PARTIDA");
        put(ES, "BACK", "VOLVER");
        put(ES, "PAUSED", "PAUSA");
        put(ES, "RESUME", "CONTINUAR");
        put(ES, "VICTORY", "¡VICTORIA!");
        put(ES, "DEFEAT", "DERROTA");

        // === 4. FRANÇAIS (FR) ===
        put(FR, "SETTINGS_TITLE", "PARAMÈTRES & COMMANDES");
        put(FR, "LANGUAGE", "Langue");
        put(FR, "MUSIC", "Musique (BGM)");
        put(FR, "SFX", "Effets sonores");
        put(FR, "ON", "OUI");
        put(FR, "OFF", "NON");
        put(FR, "SAVE_CLOSE", "ENREGISTRER & FERMER (Esc)");
        put(FR, "SAVE_CLOSE_ENTER", "► ENREGISTRER & FERMER ◄");
        put(FR, "CONTROLS_GUIDE",
            "DÉPLACEMENT: A/D ou ←/→ Courir • W/Espace/↑ Sauter • S/↓ S'accroupir/Ramper • R S'accrocher\n" +
            "COMBAT: J ou Ctrl Attaquer • L/Shift Évasion • Z/X/C Compétences 1/2/3 • U Ultime • F Assassinat\n" +
            "ARMES & UTILITAIRES: Tab ou 1–5 Changer d'arme • E Interagir • B Boutique • H Potion\n" +
            "EN LIGNE: T Chat • V Voix / M Micro • P/Esc Pause");
        put(FR, "PLAY_STORY", "CAMPAGNE");
        put(FR, "CUSTOM_MAP", "CHOISIR CARTE");
        put(FR, "MULTIPLAYER", "MULTIJOUEUR");
        put(FR, "FRIENDS", "Amis");
        put(FR, "SELECT_HERO", "CHOISIR ASSASSIN");
        put(FR, "START_BATTLE", "AU COMBAT");
        put(FR, "CONTINUE", "CONTINUER");
        put(FR, "NEW_GAME", "NOUVELLE PARTIE");
        put(FR, "BACK", "RETOUR");
        put(FR, "PAUSED", "PAUSE");
        put(FR, "RESUME", "REPRENDRE");
        put(FR, "VICTORY", "VICTOIRE !");
        put(FR, "DEFEAT", "DÉFAITE");

        // === 5. PORTUGUÊS (PT) ===
        put(PT, "SETTINGS_TITLE", "CONFIGURAÇÕES E CONTROLES");
        put(PT, "LANGUAGE", "Idioma");
        put(PT, "MUSIC", "Música (BGM)");
        put(PT, "SFX", "Efeitos (SFX)");
        put(PT, "ON", "SIM");
        put(PT, "OFF", "NÃO");
        put(PT, "SAVE_CLOSE", "SALVAR & FECHAR (Esc)");
        put(PT, "SAVE_CLOSE_ENTER", "► SALVAR & FECHAR ◄");
        put(PT, "CONTROLS_GUIDE",
            "MOVIMENTO E PARKOUR: A/D ou ←/→ Correr • W/Espaço/↑ Pular • S/↓ Agachar/Rastejar • R Escalar\n" +
            "COMBATE: J ou Ctrl Atacar • L/Shift Esquiva • Z/X/C Habilidades 1/2/3 • U Golpe Final • F Assassinar\n" +
            "ARMAS: Tab ou 1–5 Trocar Arma • E Interagir • B Loja • H Poção de Vida\n" +
            "ONLINE: T Chat • V Voz / M Microfone • P/Esc Pausa");
        put(PT, "PLAY_STORY", "MODO HISTÓRIA");
        put(PT, "CUSTOM_MAP", "ESCOLHER MAPA");
        put(PT, "MULTIPLAYER", "MULTIJOGADOR");
        put(PT, "FRIENDS", "Amigos");
        put(PT, "SELECT_HERO", "ESCOLHER ASSASSINO");
        put(PT, "START_BATTLE", "ENTRAR NA BATALHA");
        put(PT, "CONTINUE", "CONTINUAR");
        put(PT, "NEW_GAME", "NOVO JOGO");
        put(PT, "BACK", "VOLTAR");
        put(PT, "PAUSED", "PAUSA");
        put(PT, "RESUME", "CONTINUAR");
        put(PT, "VICTORY", "VITÓRIA!");
        put(PT, "DEFEAT", "DERROTA");

        // === 6. DEUTSCH (DE) ===
        put(DE, "SETTINGS_TITLE", "EINSTELLUNGEN & STEUERUNG");
        put(DE, "LANGUAGE", "Sprache");
        put(DE, "MUSIC", "Musik (BGM)");
        put(DE, "SFX", "Soundeffekte");
        put(DE, "ON", "EIN");
        put(DE, "OFF", "AUS");
        put(DE, "SAVE_CLOSE", "SPEICHERN & SCHLIEßEN");
        put(DE, "SAVE_CLOSE_ENTER", "► SPEICHERN & SCHLIEßEN ◄");
        put(DE, "CONTROLS_GUIDE",
            "BEWEGUNG & PARKOUR: A/D oder ←/→ Laufen • W/Leertaste/↑ Springen • S/↓ Ducken/Kriechen • R Klettern\n" +
            "KAMPF: J oder Strg Angriff • L/Shift Sprint • Z/X/C Fähigkeiten 1/2/3 • U Ultimativ • F Attentat\n" +
            "WAFFEN & ITEMS: Tab oder 1–5 Waffe wechseln • E Interagieren • B Händler • H Heiltrank\n" +
            "MULTIPLAYER: T Chat • V Sprache / M Mikrofon • P/Esc Pause");
        put(DE, "PLAY_STORY", "KAMPAGNE");
        put(DE, "CUSTOM_MAP", "KARTE WÄHLEN");
        put(DE, "MULTIPLAYER", "MEHRSPIELER");
        put(DE, "FRIENDS", "Freunde");
        put(DE, "SELECT_HERO", "ASSASSINE WÄHLEN");
        put(DE, "START_BATTLE", "KAMPF STARTEN");
        put(DE, "CONTINUE", "WEITER");
        put(DE, "NEW_GAME", "NEUES SPIEL");
        put(DE, "BACK", "ZURÜCK");
        put(DE, "PAUSED", "PAUSE");
        put(DE, "RESUME", "WEITER");
        put(DE, "VICTORY", "SIEG!");
        put(DE, "DEFEAT", "NIEDERLAGE");

        // === 7. РУССКИЙ (RU) ===
        put(RU, "SETTINGS_TITLE", "НАСТРОЙКИ И УПРАВЛЕНИЕ");
        put(RU, "LANGUAGE", "Язык");
        put(RU, "MUSIC", "Музыка (BGM)");
        put(RU, "SFX", "Звуки (SFX)");
        put(RU, "ON", "ВКЛ");
        put(RU, "OFF", "ВЫКЛ");
        put(RU, "SAVE_CLOSE", "СОХРАНИТЬ (Esc)");
        put(RU, "SAVE_CLOSE_ENTER", "► СОХРАНИТЬ (Enter/Esc) ◄");
        put(RU, "CONTROLS_GUIDE",
            "ДВИЖЕНИЕ: A/D или ←/→ Бег • W/Пробел/↑ Прыжок и лазание • S/↓ Присесть/Ползти • R Зацепиться\n" +
            "БОЙ: J или Ctrl Атака • L/Shift Рывок • Z/X/C Навыки 1/2/3 • U Суперудар • F Скрытное убийство\n" +
            "СНАРЯЖЕНИЕ: Tab или 1–5 Оружие • E Действие • B Магазин • H Зелье здоровья\n" +
            "ОНЛАЙН: T Чат • V Голос / M Микрофон • P/Esc Пауза");
        put(RU, "PLAY_STORY", "СЮЖЕТНАЯ КАМПАНИЯ");
        put(RU, "CUSTOM_MAP", "ВЫБОР КАРТЫ");
        put(RU, "MULTIPLAYER", "МУЛЬТИПЛЕЕР");
        put(RU, "FRIENDS", "Друзья");
        put(RU, "SELECT_HERO", "ВЫБОР АССАСИНА");
        put(RU, "START_BATTLE", "В БОЙ");
        put(RU, "CONTINUE", "ПРОДОЛЖИТЬ");
        put(RU, "NEW_GAME", "НОВАЯ ИГРА");
        put(RU, "BACK", "НАЗАД");
        put(RU, "PAUSED", "ПАУЗА");
        put(RU, "RESUME", "ПРОДОЛЖИТЬ");
        put(RU, "VICTORY", "ПОБЕДА!");
        put(RU, "DEFEAT", "ПОРАЖЕНИЕ");
    }
}
