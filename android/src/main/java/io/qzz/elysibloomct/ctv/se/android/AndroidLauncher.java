package io.qzz.elysibloomct.ctv.se.android;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import io.qzz.elysibloomct.ctv.se.DeviceIdentity;
import io.qzz.elysibloomct.ctv.se.JavaGameSocket;
import io.qzz.elysibloomct.ctv.se.Main;
import io.qzz.elysibloomct.ctv.se.NetworkManager;

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NetworkManager.socketFactory = JavaGameSocket::create; // kết nối online bằng socket.io bản Java
        DeviceIdentity.androidIdProvider = () ->
            Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true;
        Main game = new Main();
        game.micPermissionChecker = () -> Build.VERSION.SDK_INT < 23
            || checkSelfPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED;
        game.micPermissionRequester = () -> {
            if (Build.VERSION.SDK_INT >= 23) {
                runOnUiThread(() -> requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 1001));
            }
        };
        initialize(game, configuration);
    }
}