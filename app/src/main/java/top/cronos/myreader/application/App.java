/*
 * This file is part of QYReader.
 * QYReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QYReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QYReader.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 - 2022 fengyuecanzhu
 */

package top.cronos.myreader.application;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.style.MaterialStyle;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.connection.FileDownloadUrlConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import top.cronos.myreader.R;
import top.cronos.myreader.common.APPCONST;
import top.cronos.myreader.common.URLCONST;
import top.cronos.myreader.entity.Setting;
import top.cronos.myreader.model.user.User;
import top.cronos.myreader.model.user.UserService;
import top.cronos.myreader.ui.dialog.UpdateDialog;
import top.cronos.myreader.util.SharedPreUtils;
import top.cronos.myreader.util.ToastUtils;
import top.cronos.myreader.util.help.SSLSocketClient;
import top.cronos.myreader.util.help.StringHelper;
import top.cronos.myreader.util.utils.NetworkUtils;
import top.cronos.myreader.util.utils.OkHttpUtils;
import top.cronos.myreader.util.utils.PluginUtils;


public class App extends Application {

    public static final String TAG = App.class.getSimpleName();
    private static final Handler handler = new Handler();
    private static App application;
    private ExecutorService mFixedThreadPool;
    private static boolean debug;
    public static boolean isBackground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        debug = isApkInDebug(this);
        CrashHandler.register(this);
        SSLSocketClient.trustAllHosts();//??????????????????
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            webviewSetPath(this);
        }
        FileDownloader.setupOnApplicationOnCreate(this)
                .connectionCreator(new FileDownloadUrlConnection
                        .Creator(new FileDownloadUrlConnection.Configuration()
                        .connectTimeout(15_000) // set connection timeout.
                        .readTimeout(15_000) // set read timeout.
                ))
                .commit();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        mFixedThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());//??????????????????
        initNightTheme();
//        LLog.init(APPCONST.LOG_DIR);
        initDialogX();
        //AdUtils.initAd();
        PluginUtils.INSTANCE.init();
    }


    private void initDialogX() {
        DialogX.init(this);
        DialogX.DEBUGMODE = debug;
        DialogX.globalStyle = MaterialStyle.style();
    }

    public void initNightTheme() {
        if (isNightFS()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            DialogX.globalTheme = DialogX.THEME.AUTO;
        } else {
            if (isNightTheme()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                DialogX.globalTheme = DialogX.THEME.DARK;
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                DialogX.globalTheme = DialogX.THEME.LIGHT;
            }
        }
    }

    public boolean isNightTheme() {
        return !SysManager.getSetting().isDayStyle();
    }

    public boolean isNightFS() {
        return SharedPreUtils.getInstance().getBoolean(getString(R.string.isNightFS), false);
    }

    /**
     * ??????????????????
     *
     * @param isNightMode
     */
    public void setNightTheme(boolean isNightMode) {
        SharedPreUtils.getInstance().putBoolean(getmContext().getString(R.string.isNightFS), false);
        Setting setting = SysManager.getSetting();
        setting.setDayStyle(!isNightMode);
        SysManager.saveSetting(setting);
        App.getApplication().initNightTheme();
    }


    public static Context getmContext() {
        return application;
    }


    public void newThread(Runnable runnable) {
        try {
            mFixedThreadPool.execute(runnable);
        } catch (Exception e) {
            //e.printStackTrace();
            mFixedThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());//??????????????????
            mFixedThreadPool.execute(runnable);
        }
    }

    public void shutdownThreadPool() {
        mFixedThreadPool.shutdownNow();
    }


    @TargetApi(26)
    private void createNotificationChannel() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel downloadChannel = new NotificationChannel(APPCONST.channelIdDownload, "????????????", NotificationManager.IMPORTANCE_LOW);
        downloadChannel.enableLights(true);//???????????????icon????????????????????????
        downloadChannel.setLightColor(Color.RED);//???????????????
        downloadChannel.setShowBadge(false); //??????????????????????????????????????????????????????
        notificationManager.createNotificationChannel(downloadChannel);

        NotificationChannel readChannel = new NotificationChannel(APPCONST.channelIdRead, "????????????", NotificationManager.IMPORTANCE_LOW);
        readChannel.enableLights(true);//???????????????icon????????????????????????
        readChannel.setLightColor(Color.RED);//???????????????
        readChannel.setShowBadge(false); //??????????????????????????????????????????????????????
        notificationManager.createNotificationChannel(readChannel);
    }


    /**
     * ???????????????
     *
     * @param runnable
     */
    public static void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public static Handler getHandler() {
        return handler;
    }

    public static App getApplication() {
        return application;
    }


    private boolean isFolderExist(String dir) {
        File folder = Environment.getExternalStoragePublicDirectory(dir);
        return (folder.exists() && folder.isDirectory()) || folder.mkdirs();
    }

    /**
     * ??????app?????????
     *
     * @return
     */
    public static int getVersionCode() {
        try {
            PackageManager manager = application.getPackageManager();
            PackageInfo info = manager.getPackageInfo(application.getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * ??????app?????????(String)
     *
     * @return
     */
    public static String getStrVersionName() {
        try {
            PackageManager manager = application.getPackageManager();
            PackageInfo info = manager.getPackageInfo(application.getPackageName(), 0);

            return info.versionName;
        } catch (Exception e) {
            e.printStackTrace();
            return "1.0.0";
        }
    }

    /**
     * ??????apk?????????????????????????????????????????????
     *
     * @param absPath apk??????????????????
     */
    public static int apkInfo(String absPath) {
        PackageManager pm = application.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            int versionCode = pkgInfo.versionCode;
            Log.i(TAG, String.format("PkgInfo: %s", versionCode));
            return versionCode;
        }
        return 0;
    }


    /**
     * ????????????
     */
    public static void checkVersionByServer(final AppCompatActivity activity, final boolean isManualCheck) {
        App.getApplication().newThread(() -> {
            try {
                String url = "https://shimo.im/docs/cqkgjPRRydYYhQKt/read";
                if (debug) {
                    url = "https://shimo.im/docs/zfzpda7MUGskOC9v/read";
                }
                String html = OkHttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                String content = doc.getElementsByClass("ql-editor").text();
                if (StringHelper.isEmpty(content)) {
                    content = OkHttpUtils.getUpdateInfo();
                    if (StringHelper.isEmpty(content)) {
                        if (isManualCheck || NetworkUtils.isNetWorkAvailable()) {
                            ToastUtils.showError("?????????????????????");
                        }
                        return;
                    }
                }
                String[] contents = content.split(";");
                int newestVersion = 0;
                String updateContent = "";
                String downloadLink = null;
                boolean isForceUpdate = false;
                int forceUpdateVersion;
                StringBuilder s = new StringBuilder();
                newestVersion = Integer.parseInt(contents[0].substring(contents[0].indexOf(":") + 1));
                isForceUpdate = Boolean.parseBoolean(contents[1].substring(contents[1].indexOf(":") + 1));
                downloadLink = contents[2].substring(contents[2].indexOf(":") + 1).trim();
                updateContent = contents[3].substring(contents[3].indexOf(":") + 1);
                SharedPreUtils.getInstance().putString(getmContext().getString(R.string.lanzousKeyStart), contents[4].substring(contents[4].indexOf(":") + 1));

                String newSplashTime = contents[5].substring(contents[5].indexOf(":") + 1);
                String oldSplashTime = SharedPreUtils.getInstance().getString("splashTime");
                SharedPreUtils.getInstance().putBoolean("needUdSI", !oldSplashTime.equals(newSplashTime));
                SharedPreUtils.getInstance().putString("splashTime", contents[5].substring(contents[5].indexOf(":") + 1));
                SharedPreUtils.getInstance().putString("splashImageUrl", contents[6].substring(contents[6].indexOf(":") + 1));
                SharedPreUtils.getInstance().putString("splashImageMD5", contents[7].substring(contents[7].indexOf(":") + 1));

                forceUpdateVersion = Integer.parseInt(contents[8].substring(contents[8].indexOf(":") + 1));
                SharedPreUtils.getInstance().putInt("forceUpdateVersion", forceUpdateVersion);

                String domain = contents[9].substring(contents[9].indexOf(":") + 1);
                SharedPreUtils.getInstance().putString("domain", domain);
                String pluginConfigUrl = contents[10].substring(contents[10].indexOf(":") + 1);
                SharedPreUtils.getInstance().putString("pluginConfigUrl", pluginConfigUrl);
                int versionCode = getVersionCode();

                isForceUpdate = isForceUpdate && forceUpdateVersion > versionCode;
                if (!StringHelper.isEmpty(downloadLink)) {
                    SharedPreUtils.getInstance().putString(getmContext().getString(R.string.downloadLink), downloadLink);
                } else {
                    SharedPreUtils.getInstance().putString(getmContext().getString(R.string.downloadLink), URLCONST.APP_DIR_URL);
                }
                String[] updateContents = updateContent.split("/");
                for (String string : updateContents) {
                    s.append(string);
                    s.append("<br>");
                }
                Log.i("???????????????????????????", newestVersion + "");
                if (newestVersion > versionCode) {
                    Setting setting = SysManager.getSetting();
                    if (isManualCheck || setting.getNewestVersionCode() < newestVersion || isForceUpdate) {
                        setting.setNewestVersionCode(newestVersion);
                        SysManager.saveSetting(setting);
                        getApplication().updateApp2(activity, downloadLink, newestVersion, s.toString(), isForceUpdate
                        );
                    }
                } else if (isManualCheck) {
                    ToastUtils.showSuccess("????????????????????????");
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("?????????????????????", "" + e.getLocalizedMessage());
                if (isManualCheck || NetworkUtils.isNetWorkAvailable()) {
                    ToastUtils.showError("?????????????????????");
                }
            }
        });
    }


    public void updateApp2(final AppCompatActivity activity, final String url, final int versionCode, String message,
                           final boolean isForceUpdate) {
        //String version = (versionCode / 100 % 10) + "." + (versionCode / 10 % 10) + "." + (versionCode % 10);
        int hun = versionCode / 100;
        int ten = versionCode / 10 % 10;
        int one = versionCode % 10;
        String versionName = "v" + hun + "." + ten + "." + one;
        UpdateDialog updateDialog = new UpdateDialog.Builder()
                .setVersion(versionName)
                .setContent(message)
                .setCancelable(!isForceUpdate)
                .setDownloadUrl(url)
                .setContentHtml(true)
                .setDebug(App.isDebug())
                .build();

        updateDialog.showUpdateDialog(activity);
    }

    private void goDownload(Activity activity, String url) {
        String downloadLink = url;
        if (url == null || "".equals(url)) {
            downloadLink = URLCONST.APP_DIR_URL;
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(downloadLink));
        activity.startActivity(intent);
    }

    /**
     * ???????????????????????????debug??????
     */
    public static boolean isApkInDebug(Context context) {
        User user = UserService.INSTANCE.readConfig();
        if (user != null && "fengyue".equals(user.getUserName()))
            return true;
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * ??????Activity??????Destroy
     *
     * @param mActivity
     * @return
     */
    public static boolean isDestroy(Activity mActivity) {
        return mActivity == null || mActivity.isFinishing() || mActivity.isDestroyed();
    }


    /****************
     *
     * ??????????????????????????????????????????????????????(1085028304) ??? key ?????? 8PIOnHFuH6A38hgxvD_Rp2Bu-Ke1ToBn
     * ?????? joinQQGroup(8PIOnHFuH6A38hgxvD_Rp2Bu-Ke1ToBn) ???????????????Q????????????????????? ?????????????????????(1085028304)
     *
     * @param key ??????????????????key
     * @return ??????true???????????????Q???????????????false??????????????????
     ******************/
    public static boolean joinQQGroup(Context context, String key) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));
        // ???Flag??????????????????????????????????????????????????????????????????????????????????????????Q???????????????????????????????????????????????????????????????    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            // ????????????Q???????????????????????????
            return false;
        }
    }

    @RequiresApi(api = 28)
    public void webviewSetPath(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = getProcessName(context);

            if (!getApplicationContext().getPackageName().equals(processName)) {//?????????????????????????????????
                WebView.setDataDirectorySuffix(processName);
            }
        }
    }

    public String getProcessName(Context context) {
        if (context == null) return null;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == android.os.Process.myPid()) {
                return processInfo.processName;
            }
        }
        return null;
    }

    public static boolean isDebug() {
        return debug;
    }

    public ExecutorService getmFixedThreadPool() {
        return mFixedThreadPool;
    }

}
