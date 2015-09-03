package com.pyler.xposedmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

public class XposedManagerActivity extends Activity {
    TextView tvDownloadXposedInstaller, tvDownloadXposedFramework;
    Button btnDownloadXposedInstaller, btnDownloadXposedFramework;
    public static final String XPOSED_INSTALLER_PKG = "de.robv.android.xposed.installer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xposed_manager);
        tvDownloadXposedInstaller = (TextView) findViewById(R.id.tv_download_xposed_installer);
        tvDownloadXposedFramework = (TextView) findViewById(R.id.tv_download_xposed_framework);
        btnDownloadXposedInstaller = (Button) findViewById(R.id.btn_download_xposed_installer);
        btnDownloadXposedFramework = (Button) findViewById(R.id.btn_download_xposed_framework);
        if (!isNetworkAvailable()) {
            showDialog("No internet connection", "This app can't work without internet connection since it needs to download some files. Start internet connection and launch app again.");
        }
        if (isSamsungDevice() && isLollipopAndNewer() && isTouchWizROM() && isOdexedROM()) {
            showDialog("Odexed ROM", "Xposed can't work on Samsung's odexed ROMs. Please deodex your ROM and try again.");
        }
        if (isXposedInstallerInstalled()) {
            hideXposedInstallerSection();
        }
    }

    public void hideXposedInstallerSection() {
        tvDownloadXposedInstaller.setVisibility(View.GONE);
        btnDownloadXposedInstaller.setVisibility(View.GONE);
    }

    public void hideXposedFrameworkSection() {
        tvDownloadXposedFramework.setVisibility(View.GONE);
        btnDownloadXposedFramework.setVisibility(View.GONE);
    }

    public boolean isXposedInstallerInstalled() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(XPOSED_INSTALLER_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            // App is not installed
        }
        return false;
    }

    public boolean isLollipopAndNewer() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public boolean isOdexedROM() {
        return new File("/system/app/SystemUI.odex").exists();
    }

    public boolean isTouchWizROM() {
        return new File("/system/framework/twframework.jar").exists();
    }

    public boolean isSamsungDevice() {
        return Build.MANUFACTURER.equalsIgnoreCase("samsung");
    }

    public String getCPUArchitecture() {
        String arch = System.getProperty("os.arch");
        if ( arch.contains("x86") || arch.contains("i686") ||  arch.contains("i386")) {
            return "intel_32b";
        } else if (arch.contains("armeabi") || arch.contains("armv7")) {
            return "arm_32b";
        } else if (arch.contains("aarch64") || arch.contains("armv8")) {
            return "arm_64b";
        } else {
            return "unknown";
        }
    }

    public boolean hasTWRPRecovery() {
        return new File(Environment.getExternalStorageDirectory() + File.separator + "TWRP").exists();
    }

    public boolean hasCWMPRecovery() {
        return new File(Environment.getExternalStorageDirectory() + File.separator + "clockworkmod").exists();
    }

    public boolean hasCustomRecovery() {
        if (hasTWRPRecovery() || hasCWMPRecovery()) {
            return true;
        }
        return false;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }).show();
    }

}
