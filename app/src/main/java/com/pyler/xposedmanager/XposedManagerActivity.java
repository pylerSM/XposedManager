package com.pyler.xposedmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

public class XposedManagerActivity extends Activity {
    TextView tvDownloadXposedInstaller, tvDownloadXposedFramework;
    Button btnDownloadXposedInstaller, btnDownloadXposedFramework;
    public static final String XPOSED_INSTALLER_PKG = "de.robv.android.xposed.installer";
    public static final String XPOSED_BUILDS_JSON = "https://raw.githubusercontent.com/pylerSM/XposedManager/master/xposed_builds.json";
    public static final String XPOSED_INSTALLER_OLD = "xposed_installer_old";
    public static final String XPOSED_INSTALLER_NEW = "xposed_installer_new";
    public static final String XPOSED_FRAMEWORK = "xposed_framework";
    public static final String XPOSED_FRAMEWORK_LP_ARM = "xposed_framework_lp_arm";
    public static final String XPOSED_FRAMEWORK_LP_ARM64 = "xposed_framework_lp_arm64";
    public static final String XPOSED_FRAMEWORK_LP_X86 = "xposed_framework_lp_x86";
    public static final String XPOSED_FRAMEWORK_LP_MR1_ARM = "xposed_framework_lp_mr1_arm";
    public static final String XPOSED_FRAMEWORK_LP_MR1_ARM64 = "xposed_framework_lp_mr1_arm64";
    public static final String XPOSED_FRAMEWORK_LP_MR1_X86 = "xposed_framework_lp_mr1_x86";

    public String mXposedInstallerUrl;
    public String mXposedFrameworkUrl;
    public boolean mLoaded = false;

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
        if (isAndroidMAndNewer()) {
            showDialog("Not supported", "Xposed can't work on Android M yet.");
        }
        if (isSamsungDevice() && isLollipopAndNewer() && isTouchWizROM() && isOdexedROM()) {
            showDialog("Odexed ROM", "Xposed can't work on Samsung's odexed ROMs. Please deodex your ROM and try again.");
        }
        if (isXposedInstallerInstalled()) {
            hideXposedInstallerSection();
        }
        findXposed();
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

    public boolean isAndroidMAndNewer() {
        return Build.VERSION.SDK_INT >= 23;
    }


    public boolean isLollipopMR1() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1;
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
        Toast.makeText(this, "CPU Arch: " + arch, Toast.LENGTH_LONG).show();
        if (arch.contains("x86") || arch.contains("i686") || arch.contains("i386")) {
            return "intel_32b";
        }
        if (arch.contains("armeabi") || arch.contains("armv7")) {
            return "arm_32b";
        }
        if (arch.contains("aarch64") || arch.contains("armv8")) {
            return "arm_64b";
        }
        return null;
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


    public void findXposed() {

        new JSONHelper().execute();
        Log.d("test", mLoaded + "   " +  mXposedFrameworkUrl + "    " + mXposedInstallerUrl);

    }

    public class JSONHelper  extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {


            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            try {
                request.setURI(new URI(XPOSED_BUILDS_JSON));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            HttpResponse response = null;
            BufferedReader in = null;
            String json = "";
            try {
                response = client.execute(request);
                in = new BufferedReader(new InputStreamReader(response
                        .getEntity().getContent()));
                String line = "";
                StringBuilder sb = new StringBuilder();

                while ((line = in.readLine()) != null) {

                    sb.append(line + "\n");
               }
                in.close();
                json = sb.toString();
                JSONObject jObject = null;
                try {
                    jObject = new JSONObject(json);
                } catch (Exception e) {
                }

                if (isLollipopAndNewer()) {
                    if (jObject.has(XPOSED_INSTALLER_NEW)){
                        mXposedInstallerUrl = jObject.getString(XPOSED_INSTALLER_NEW);
                    }

                    if (isLollipopMR1()) {
                        String arch = getCPUArchitecture();
                        if (arch.equals("arm_32b")) {
                           if (jObject.has(XPOSED_FRAMEWORK_LP_MR1_ARM)){
                               mXposedFrameworkUrl = jObject.getString(XPOSED_FRAMEWORK_LP_MR1_ARM);
                           }
                       }
                        if (arch.equals("arm_64b")) {
                            if (jObject.has(XPOSED_FRAMEWORK_LP_MR1_ARM64)){
                                mXposedFrameworkUrl = jObject.getString(XPOSED_FRAMEWORK_LP_MR1_ARM64);
                            }
                        }
                        if (arch.equals("intel_32b")) {
                            if (jObject.has(XPOSED_FRAMEWORK_LP_MR1_X86)){
                                mXposedFrameworkUrl = jObject.getString(XPOSED_FRAMEWORK_LP_MR1_X86);
                            }
                        }
                    } else {
                        String arch = getCPUArchitecture();
                        if (arch.equals("arm_32b")) {
                            if (jObject.has(XPOSED_FRAMEWORK_LP_ARM)){
                                mXposedFrameworkUrl = jObject.getString(XPOSED_FRAMEWORK_LP_ARM);
                            }
                        }
                        if (arch.equals("arm_64b")) {
                            if (jObject.has(XPOSED_FRAMEWORK_LP_ARM64)){
                                mXposedFrameworkUrl = jObject.getString(XPOSED_FRAMEWORK_LP_ARM64);
                            }
                        }
                        if (arch.equals("intel_32b")) {
                            if (jObject.has(XPOSED_FRAMEWORK_LP_X86)){
                                mXposedFrameworkUrl = jObject.getString(XPOSED_FRAMEWORK_LP_X86);
                            }
                        }
                    }


                } else {
                    if (jObject.has(XPOSED_INSTALLER_OLD)){
                        mXposedInstallerUrl = jObject.getString(XPOSED_INSTALLER_OLD);
                    }
                    if (jObject.has(XPOSED_FRAMEWORK)){
                        mXposedFrameworkUrl = jObject.getString(XPOSED_FRAMEWORK);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            mLoaded = true;
            return null;
        }
    }

}
