package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class SoundManager {
    private static SoundManager instance;

    private Sound sndSlash;
    private Sound sndHit;
    private Sound sndHeavyHit;
    private Sound sndDash;
    private Sound sndJump;
    private Sound sndCoin;
    private Sound sndHurt;
    private Sound sndBossSmash;
    
    // Skill & Parkour SFX
    private Sound sndUltimateP1;
    private Sound sndTrangUltimate;
    private Sound sndUltimateP2;
    private Sound sndWallJump;
    private Sound sndOrbPickup;

    // Background Musics
    private Music menuBgm;
    private Music battleBgm;
    private Music currentBgm;

    private boolean soundEnabled = true;
    private float soundVolume = 0.85f;
    private boolean musicEnabled = true;
    private float musicVolume = 0.50f;

    private SoundManager() {
        initSounds();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private Sound safeLoadSound(String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                return Gdx.audio.newSound(Gdx.files.internal(path));
            }
        } catch (Exception e) {
            Gdx.app.log("SoundManager", "Error loading sound " + path + ": " + e.getMessage());
        }
        return null;
    }

    private Sound safeLoadFirstSound(String[] candidates) {
        for (String file : candidates) {
            Sound s = safeLoadSound(file);
            if (s != null) return s;
        }
        return null;
    }

    private void initSounds() {
        sndSlash = safeLoadSound("sounds/slash.wav");
        sndHit = safeLoadSound("sounds/hit.wav");
        sndHeavyHit = safeLoadSound("sounds/heavy_hit.wav");
        sndDash = safeLoadSound("sounds/dash.wav");
        sndJump = safeLoadSound("sounds/jump.wav");
        sndCoin = safeLoadSound("sounds/coin.wav");
        sndHurt = safeLoadSound("sounds/hurt.wav");
        sndBossSmash = safeLoadSound("sounds/boss_smash.wav");

        sndUltimateP1 = safeLoadSound("sounds/ultimate_p1.wav");
        sndTrangUltimate = safeLoadFirstSound(new String[] {
            "sounds/trang_ultimate.mp3", "sounds/trang_ultimate.wav", "sounds/trang_ultimate.ogg",
            "trang_ultimate.mp3", "trang_ultimate.wav"
        });
        sndUltimateP2 = safeLoadSound("sounds/ultimate_p2.wav");
        sndWallJump = safeLoadSound("sounds/wall_jump.wav");
        sndOrbPickup = safeLoadSound("sounds/orb_pickup.wav");

        reloadMenuBgm();

        String[] battleBgmCandidates = new String[] {
            "sounds/battle_bgm.mp3", "sounds/battle_bgm.ogg", "sounds/battle_bgm.wav",
            "sounds/bgm.mp3", "sounds/bgm.ogg", "sounds/bgm.wav"
        };
        for (String file : battleBgmCandidates) {
            try {
                if (Gdx.files.internal(file).exists()) {
                    battleBgm = Gdx.audio.newMusic(Gdx.files.internal(file));
                    battleBgm.setLooping(true);
                    battleBgm.setVolume(musicVolume);
                    break;
                }
            } catch (Exception e) {
                Gdx.app.log("SoundManager", "Error loading battle BGM " + file + ": " + e.getMessage());
            }
        }
    }

    public void playSlash() {
        playSound(sndSlash, 0.75f, 1.0f);
    }

    public void playPlayerSlash(boolean isWhiteHero) {
        // P1 Trắng: kiếm vũ thanh thoát bão tố (pitch 1.22f)
        // P2 Đen: tử thần ám sát trầm đục (pitch 0.88f)
        playSound(sndSlash, 0.80f, isWhiteHero ? 1.22f : 0.88f);
    }

    public void playMonsterSlash() {
        playSound(sndSlash, 0.65f, 0.72f);
    }

    public void playHit() {
        playSound(sndHit, 0.85f, 1.0f);
    }

    public void playHeavyHit() {
        playSound(sndHeavyHit, 1.0f, 1.0f);
    }

    public void playDash() {
        playSound(sndDash, 0.70f, 1.0f);
    }

    public void playJump() {
        playSound(sndJump, 0.65f, 1.0f);
    }

    public void playWallJump() {
        if (sndWallJump != null) {
            playSound(sndWallJump, 0.90f, 1.0f);
        } else {
            playSound(sndJump, 0.80f, 1.35f);
        }
    }

    public void playCoin() {
        playSound(sndCoin, 0.80f, 1.0f);
    }

    public void playHurt() {
        playSound(sndHurt, 0.90f, 1.0f);
    }

    public void playMonsterHurt() {
        playSound(sndHurt, 0.75f, 0.78f);
    }

    public void playBossSmash() {
        playSound(sndBossSmash, 1.0f, 1.0f);
    }

    public void playBossAttack() {
        playSound(sndHeavyHit, 0.9f, 0.8f);
        playSound(sndBossSmash, 0.85f, 0.95f);
    }

    public void playUltimateP1() {
        if (sndUltimateP1 != null) {
            playSound(sndUltimateP1, 1.0f, 1.0f);
        } else {
            playSound(sndHeavyHit, 1.0f, 1.2f);
        }
    }

    public void playTrangUltimate() {
        if (sndTrangUltimate != null) {
            playSound(sndTrangUltimate, 1.0f, 1.0f);
        }
    }

    public void playUltimateP2() {
        if (sndUltimateP2 != null) {
            playSound(sndUltimateP2, 1.0f, 1.0f);
        } else {
            playSound(sndSlash, 1.0f, 0.75f);
        }
    }

    public void playOrbPickup() {
        if (sndOrbPickup != null) {
            playSound(sndOrbPickup, 1.0f, 1.0f);
        } else {
            playSound(sndCoin, 1.0f, 1.25f);
        }
    }

    private void playSound(Sound sound, float volumeScale, float pitch) {
        if (!soundEnabled || sound == null) return;
        try {
            sound.play(soundVolume * volumeScale, pitch, 0.0f);
        } catch (Exception e) {
            // Prevent audio device exceptions on mobile
        }
    }

    private void playSound(Sound sound, float volumeScale) {
        playSound(sound, volumeScale, 1.0f);
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundVolume(float volume) {
        this.soundVolume = Math.max(0f, Math.min(1f, volume));
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void reloadMenuBgm() {
        if (menuBgm != null) {
            if (currentBgm == menuBgm) currentBgm = null;
            try {
                menuBgm.stop();
                menuBgm.dispose();
            } catch (Exception ignored) {}
            menuBgm = null;
        }
        String[] menuBgmCandidates = new String[] {
            "sounds/menu_bgm.mp3", "sounds/menu_bgm.ogg", "sounds/menu_bgm.wav",
            "sounds/lobby_bgm.mp3", "sounds/lobby_bgm.ogg", "sounds/lobby_bgm.wav",
            "sounds/lobby.mp3", "sounds/lobby.ogg", "sounds/lobby.wav",
            "sounds/menu.mp3", "sounds/menu.ogg", "sounds/menu.wav",
            "menu_bgm.mp3", "menu_bgm.ogg", "menu_bgm.wav",
            "lobby_bgm.mp3", "lobby_bgm.ogg", "lobby_bgm.wav",
            "lobby.mp3", "lobby.ogg", "lobby.wav",
            "menu.mp3", "menu.ogg", "menu.wav"
        };
        for (String file : menuBgmCandidates) {
            try {
                if (Gdx.files.internal(file).exists()) {
                    menuBgm = Gdx.audio.newMusic(Gdx.files.internal(file));
                    menuBgm.setLooping(true);
                    menuBgm.setVolume(musicVolume);
                    break;
                }
            } catch (Exception ignored) {}
        }
    }

    public void playMenuBgm() {
        if (menuBgm == null) {
            reloadMenuBgm();
        }
        switchBgm(menuBgm);
    }

    public void playBattleBgm() {
        switchBgm(battleBgm != null ? battleBgm : menuBgm);
    }

    public void playBgm() {
        playBattleBgm();
    }

    private void switchBgm(Music targetBgm) {
        if (targetBgm == null) return;
        if (!musicEnabled) {
            // Ghi nhớ bản nhạc cần phát để bật lại nhạc thì phát đúng bài của màn hình hiện tại
            if (currentBgm != null && currentBgm != targetBgm) {
                try { currentBgm.stop(); } catch (Exception ignored) {}
            }
            currentBgm = targetBgm;
            return;
        }
        try {
            if (currentBgm == targetBgm && currentBgm.isPlaying()) {
                return;
            }
            if (currentBgm != null && currentBgm != targetBgm) {
                try { currentBgm.stop(); } catch (Exception ignored) {}
            }
            currentBgm = targetBgm;
            currentBgm.setLooping(true);
            currentBgm.setVolume(musicVolume);
            currentBgm.play();
        } catch (Exception e) {}
    }

    public void stopBgm() {
        if (currentBgm != null) {
            try { currentBgm.stop(); } catch (Exception e) {}
        }
        if (menuBgm != null) {
            try { menuBgm.stop(); } catch (Exception e) {}
        }
        if (battleBgm != null) {
            try { battleBgm.stop(); } catch (Exception e) {}
        }
    }

    public void pauseBgm() {
        if (currentBgm != null) {
            try { currentBgm.pause(); } catch (Exception e) {}
        }
    }

    public void resumeBgm() {
        if (musicEnabled && currentBgm != null) {
            try {
                currentBgm.setLooping(true);
                currentBgm.setVolume(musicVolume);
                currentBgm.play();
            } catch (Exception e) {}
        }
    }

    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0f, Math.min(1f, volume));
        if (currentBgm != null) {
            try { currentBgm.setVolume(this.musicVolume); } catch (Exception e) {}
        }
    }

    public void setMusicEnabled(boolean enabled) {
        if (this.musicEnabled == enabled) return;
        this.musicEnabled = enabled;
        if (!enabled) stopBgm();
        else resumeBgm();
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void dispose() {
        if (sndSlash != null) sndSlash.dispose();
        if (sndHit != null) sndHit.dispose();
        if (sndHeavyHit != null) sndHeavyHit.dispose();
        if (sndDash != null) sndDash.dispose();
        if (sndJump != null) sndJump.dispose();
        if (sndCoin != null) sndCoin.dispose();
        if (sndHurt != null) sndHurt.dispose();
        if (sndBossSmash != null) sndBossSmash.dispose();
        if (sndUltimateP1 != null) sndUltimateP1.dispose();
        if (sndTrangUltimate != null) sndTrangUltimate.dispose();
        if (sndUltimateP2 != null) sndUltimateP2.dispose();
        if (sndWallJump != null) sndWallJump.dispose();
        if (sndOrbPickup != null) sndOrbPickup.dispose();

        if (menuBgm != null) {
            try {
                menuBgm.stop();
                menuBgm.dispose();
            } catch (Exception e) {}
        }
        if (battleBgm != null) {
            try {
                battleBgm.stop();
                battleBgm.dispose();
            } catch (Exception e) {}
        }
        currentBgm = null;
        instance = null;
    }
}
