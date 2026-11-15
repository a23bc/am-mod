package com.a23bc.ammod;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * v0.1.0 —— 脚手架版本。
 *
 * 目的不是"改好看"，而是把三件事证明掉：
 *   1. 模块被 LSPosed 加载进了 com.apple.android.music 进程；
 *   2. 我们能在界面绘制完成后拿到真实的 View 树；
 *   3. 我们确实改得动界面（给命中的容器叠一层半透明色）。
 *
 * 有了 2 打出来的 id 清单，第二版才能精确地改 bottom nav / mini player。
 */
public class HookMain implements IXposedHookLoadPackage {

    private static final String TAG = "AMMod";
    private static final String TARGET = "com.apple.android.music";

    // 验证阶段两个都开。正式做效果时关掉 DUMP_TREE，避免刷 logcat。
    private static final boolean DUMP_TREE = true;
    private static final boolean HIGHLIGHT = true;

    private static long sLastInspect = 0L;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET.equals(lpparam.packageName)) {
            return;
        }

        log("module loaded in " + lpparam.processName + " (v0.1.0)");

        // onResume 之后 post 到主线程队列，此时 layout 已经完成，View 树是完整的。
        XposedHelpers.findAndHookMethod(
                Activity.class,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        final Activity activity = (Activity) param.thisObject;
                        final View decor = activity.getWindow().getDecorView();
                        decor.post(new Runnable() {
                            @Override
                            public void run() {
                                inspect(decor);
                            }
                        });
                    }
                });
    }

    private static void inspect(View root) {
        long now = System.currentTimeMillis();
        if (now - sLastInspect < 3000L) {
            return; // 节流：切页面时别把 logcat 冲爆
        }
        sLastInspect = now;

        if (DUMP_TREE) {
            log("---- view tree begin ----");
            walk(root, 0);
            log("---- view tree end ----");
        }

        if (HIGHLIGHT) {
            // 这两个 id 是 2026-11-15 在 Apple Music 7.0.0 真机上 dump 出来的真实资源名。
            // 用 ViewOverlay 叠加，不覆盖原 background，圆角/波纹都还在。
            highlightParents(root, "mini_player_title", Color.argb(70, 255, 64, 64));
            highlightParents(root, "navigation_bar_item_icon_container", Color.argb(70, 64, 128, 255));
        }
    }

    private static void walk(View v, int depth) {
        String text = "";
        if (v instanceof TextView) {
            text = ((TextView) v).getText().toString().replace('\n', ' ');
        }
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            pad.append("  ");
        }
        log(pad + v.getClass().getName()
                + " id=" + idName(v)
                + " pos=[" + v.getLeft() + "," + v.getTop() + "]["
                + v.getRight() + "," + v.getBottom() + "]"
                + (text.isEmpty() ? "" : " text=" + text));

        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                walk(g.getChildAt(i), depth + 1);
            }
        }
    }

    private static void highlightParents(View root, String entryName, int color) {
        List<View> found = new ArrayList<>();
        collect(root, entryName, found);

        int hits = 0;
        for (View v : found) {
            ViewParent p = v.getParent();
            if (!(p instanceof View)) {
                continue;
            }
            View parent = (View) p;
            ColorDrawable overlay = new ColorDrawable(color);
            overlay.setBounds(0, 0, parent.getWidth(), parent.getHeight());
            parent.getOverlay().add(overlay);
            hits++;
        }
        log("highlight " + entryName + " -> " + hits + " parent(s) tinted");
    }

    private static void collect(View v, String entryName, List<View> out) {
        if (entryName.equals(idName(v))) {
            out.add(v);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                collect(g.getChildAt(i), entryName, out);
            }
        }
    }

    private static String idName(View v) {
        int id = v.getId();
        if (id == View.NO_ID) {
            return "-";
        }
        try {
            return v.getResources().getResourceEntryName(id);
        } catch (Throwable t) {
            return "0x" + Integer.toHexString(id);
        }
    }

    private static void log(String msg) {
        XposedBridge.log(TAG + ": " + msg); // LSPosed 模块日志
        Log.d(TAG, msg);                    // adb logcat -s AMMod:D
    }
}
