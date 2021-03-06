package com.tool.common.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.util.Preconditions;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.tool.common.base.App;
import com.tool.common.di.component.AppComponent;
import com.tool.common.utils.base.BaseUtils;

import org.simple.eventbus.EventBus;

import java.io.File;
import java.util.List;

import static com.tool.common.integration.AppManager.APPMANAGER_MESSAGE;
import static com.tool.common.integration.AppManager.EXIT;
import static com.tool.common.integration.AppManager.KILL;
import static com.tool.common.integration.AppManager.START_ACTIVITY;

/**
 * 应用工具类
 */
public final class AppUtils extends BaseUtils {

    public AppUtils() {
        super();
    }

    /**
     * 获取版本号
     *
     * @param context
     * @return
     */
    public static int getVersionCode(Context context) {
        int versionCode = 0;
        try {
            versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            versionCode = 0;
        }
        return versionCode;
    }

    /**
     * 获取指定包名应用的版本号
     *
     * @param context
     * @param packageName
     * @return
     */
    public static int getVersionCode(Context context, String packageName) {
        int versionCode = 0;
        try {
            versionCode = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            versionCode = 0;
        }
        return versionCode;
    }

    /**
     * 获取版本名
     *
     * @param context
     * @return
     */
    public static String getVersionName(Context context) {
        String name = "";
        try {
            name = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ex) {
            name = "";
        }
        return name;
    }

    /**
     * 获取应用渠道信息
     *
     * @param context
     * @param channel 渠道名称
     * @return
     */
    public static String getAppChannel(Context context, String channel) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                for (String key : appInfo.metaData.keySet()) {
                    if (key.equals(channel)) {
                        return appInfo.metaData.get(key).toString();
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return channel;
    }

    /**
     * 获取应用运行的最大内存
     *
     * @return 最大内存
     */
    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory() / 1024;
    }

    /**
     * 检测服务是否运行
     *
     * @param context   上下文
     * @param className 类名
     * @return 是否运行的状态
     */
    public static boolean isServiceRunning(Context context, String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> servicesList = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (RunningServiceInfo si : servicesList) {
            if (className.equals(si.service.getClassName())) {
                isRunning = true;
            }
        }
        return isRunning;
    }

    /**
     * 停止运行服务
     *
     * @param context 上下文
     * @param cls     类名
     * @return 是否执行成功
     */
    public static void startService(Context context, Class<?> cls) {
        Intent intentService = null;
        try {
            intentService = new Intent(context, cls);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (intentService != null) {
            context.startService(intentService);
        }
    }

    /**
     * 停止运行服务
     *
     * @param context 上下文
     * @param cls     类名
     * @return 是否执行成功
     */
    public static boolean stopService(Context context, Class<?> cls) {
        Intent intentService = null;
        boolean ret = false;
        try {
            intentService = new Intent(context, cls);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (intentService != null) {
            ret = context.stopService(intentService);
        }
        return ret;
    }

    /**
     * 清理后台进程与服务
     *
     * @param context 应用上下文对象context
     * @return 被清理的数量
     */
    public static int gc(Context context) {
        long i = getDeviceUsableMemory(context);
        int count = 0; // 清理掉的进程数
        ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        // 获取正在运行的service列表
        List<RunningServiceInfo> serviceList = am.getRunningServices(100);
        if (serviceList != null) {
            for (RunningServiceInfo service : serviceList) {
                if (service.pid == android.os.Process.myPid()) continue;
                try {
                    android.os.Process.killProcess(service.pid);
                    count++;
                } catch (Exception e) {
                    e.getStackTrace();
                }
            }
        }

        // 获取正在运行的进程列表
        List<RunningAppProcessInfo> processList = am.getRunningAppProcesses();
        if (processList != null) {
            for (RunningAppProcessInfo process : processList) {
                // 一般数值大于RunningAppProcessInfo.IMPORTANCE_SERVICE的进程都长时间没用或者空进程了
                // 一般数值大于RunningAppProcessInfo.IMPORTANCE_VISIBLE的进程都是非可见进程，也就是在后台运行着
                if (process.importance >
                        RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    // pkgList 得到该进程下运行的包名
                    String[] pkgList = process.pkgList;
                    for (String pkgName : pkgList) {
                        try {
                            am.killBackgroundProcesses(pkgName);
                            count++;
                        } catch (Exception e) { // 防止意外发生
                            e.getStackTrace();
                        }
                    }
                }
            }
        }

        return count;
    }

    /**
     * 给定Context获取进程名
     *
     * @param context
     * @return
     */
    public static String getProcessName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo proInfo : runningApps) {
            if (proInfo.pid == android.os.Process.myPid()) {
                if (proInfo.processName != null) {
                    return proInfo.processName;
                }
            }
        }
        return null;
    }

    /**
     * 获取设备的可用内存大小
     *
     * @param context 应用上下文对象context
     * @return 当前内存大小
     */
    public static int getDeviceUsableMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();
        am.getMemoryInfo(mi);
        // 返回当前系统的可用内存
        return (int) (mi.availMem / (1024 * 1024));
    }

    /**
     * 获取activity尺寸
     *
     * @param activity
     * @return
     */
    public static int[] getRealScreenSize(Activity activity) {
        int[] size = new int[2];
        int screenWidth = 0, screenHeight = 0;
        WindowManager w = activity.getWindowManager();
        Display d = w.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);
        // since SDK_INT = 1;
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
            try {
                screenWidth = (Integer) Display.class.getMethod("getRawWidth")
                        .invoke(d);
                screenHeight = (Integer) Display.class
                        .getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
        }
        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                Point realSize = new Point();
                Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
                screenWidth = realSize.x;
                screenHeight = realSize.y;
            } catch (Exception ignored) {
            }
        }
        size[0] = screenWidth;
        size[1] = screenHeight;
        return size;
    }

    /**
     * 拍照
     *
     * @param activity Activity
     * @param path     保存路径
     */
    public static void openCamera(Activity activity, String path, int requestCode) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(path)));
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 本地相册
     *
     * @param activity Activity
     */
    public static void openLocal(Activity activity, int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 拨打电话
     *
     * @param activity Activity
     * @param phone    电话号码
     */
    public static void call(Activity activity, String phone) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
        activity.startActivity(intent);
    }

    /**
     * 打开网页
     *
     * @param activity Activity
     * @param url      URL
     */
    public static void openHtml(Activity activity, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        activity.startActivity(intent);
    }

    /**
     * 发送短信
     *
     * @param activity Activity
     * @param number   电话号码
     * @param message  短信内容
     */
    public static void sendMessage(Activity activity, String number, String message) {
        Uri uri = Uri.parse("smsto:" + number);
        Intent sendIntent = new Intent(Intent.ACTION_VIEW, uri);
        sendIntent.putExtra("sms_body", message);
        activity.startActivity(sendIntent);
    }

    /**
     * 通过EventBus远程遥控跳转页面
     *
     * @param cls
     */
    public static void startActivity(Class cls) {
        Message message = new Message();
        message.what = START_ACTIVITY;
        message.obj = cls;
        EventBus.getDefault().post(message, APPMANAGER_MESSAGE);
    }

    /**
     * 通过EventBus远程遥控跳转页面
     *
     * @param content
     */
    public static void startActivity(Intent content) {
        Message message = new Message();
        message.what = START_ACTIVITY;
        message.obj = content;
        EventBus.getDefault().post(message, APPMANAGER_MESSAGE);
    }

    public static void kill() {
        Message message = new Message();
        message.what = KILL;
        EventBus.getDefault().post(message, APPMANAGER_MESSAGE);
    }

    public static void exit() {
        Message message = new Message();
        message.what = EXIT;
        EventBus.getDefault().post(message, APPMANAGER_MESSAGE);
    }

    public static AppComponent obtainAppComponentFromContext(Context context) {
        Preconditions.checkState(context.getApplicationContext() instanceof App, "Application does not implements App");
        return ((App) context.getApplicationContext()).getAppComponent();
    }
}