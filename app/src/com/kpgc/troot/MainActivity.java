
package com.kpgc.troot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class MainActivity extends Activity {

    private static final int REQ_CONF = 1001;
    private static final int REQ_BIN  = 1002;

    private TextView statusView;
    private TextView pathConf;
    private TextView pathBin;
    private TextView logView;
    private ScrollView sv;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private Uri confUri = null;
    private Uri binUri = null;
    private java.io.File autoTimer = null;

    private static final String TIMER_URL =
            "https://github.com/MVN123123123123123/TimerRoot/releases/download/bingchiling/timer";

    private Object shizukuSvc = null;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0E1A24"));
        root.setPadding(dp(20), dp(36), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("临时 Root");
        title.setTextColor(Color.parseColor("#F5FBFF"));
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("通过 Shizuku 执行你选择的漏洞程序\n仅限自己的设备，风险自负");
        sub.setTextColor(Color.parseColor("#8FA6B8"));
        sub.setTextSize(12);
        sub.setPadding(0, dp(6), 0, dp(14));
        root.addView(sub);

        statusView = new TextView(this);
        statusView.setText("Shizuku：未连接");
        statusView.setTextColor(Color.parseColor("#FFC24B"));
        statusView.setTextSize(14);
        statusView.setPadding(0, 0, 0, dp(12));
        root.addView(statusView);

        root.addView(btn("连接 Shizuku", "#1D3A4E", "#CFE7F5", new View.OnClickListener() {
            public void onClick(View v) { doConnect(); }
        }));

        pathConf = small("未选择 conf 文件");
        root.addView(pathConf);
        root.addView(btn("选择 conf 文件", "#1D3A4E", "#CFE7F5", new View.OnClickListener() {
            public void onClick(View v) { pick(REQ_CONF, "*/*"); }
        }));

        pathBin = small("漏洞程序：未准备");
        root.addView(pathBin);
        root.addView(btn("下载漏洞程序（约 2.1 MB）", "#2A4A63", "#CFE7F5", new View.OnClickListener() {
            public void onClick(View v) { downloadTimer(); }
        }));
        root.addView(btn("或：手动选择漏洞程序文件", "#1D3A4E", "#8FA6B8", new View.OnClickListener() {
            public void onClick(View v) { pick(REQ_BIN, "*/*"); }
        }));

        root.addView(btn("获取临时 Root", "#3DE1A4", "#062018", new View.OnClickListener() {
            public void onClick(View v) { confirmAndRun(); }
        }));

        // ---- 运行日志 面板 ----
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable pbg = new GradientDrawable();
        pbg.setColor(Color.parseColor("#132430"));
        pbg.setCornerRadius(dp(12));
        pbg.setStroke(dp(1), Color.parseColor("#1E3A4E"));
        panel.setBackground(pbg);
        panel.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView lt = new TextView(this);
        lt.setText("运行日志");
        lt.setTextColor(Color.parseColor("#CFE7F5"));
        lt.setTextSize(13);
        lt.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        head.addView(lt, hlp);
        TextView clear = new TextView(this);
        clear.setText("清空");
        clear.setTextColor(Color.parseColor("#3DE1A4"));
        clear.setTextSize(12);
        clear.setPadding(dp(10), dp(2), dp(2), dp(2));
        clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { logView.setText(""); }
        });
        head.addView(clear);
        panel.addView(head);

        logView = new TextView(this);
        logView.setTextColor(Color.parseColor("#9FB3C4"));
        logView.setTextSize(11);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(0, dp(8), 0, 0);
        logView.setText("就绪。");

        sv = new ScrollView(this);
        sv.addView(logView);
        panel.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(panel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        refreshStatus();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView small(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(Color.parseColor("#6F8798"));
        tv.setTextSize(11);
        tv.setPadding(0, dp(8), 0, dp(4));
        return tv;
    }

    private Button btn(String text, String bg, String fg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setTextColor(Color.parseColor(fg));
        b.setAllCaps(false);
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.parseColor(bg));
        d.setCornerRadius(dp(12));
        b.setBackground(d);
        b.setPadding(0, dp(10), 0, dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        return b;
    }

    private void log(final String s) {
        ui.post(new Runnable() { public void run() {
            logView.append("\n" + s);
            if (sv != null) sv.post(new Runnable() { public void run() { sv.fullScroll(View.FOCUS_DOWN); } });
        }});
    }

    private void toast(final String s) {
        ui.post(new Runnable() { public void run() {
            Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
        }});
    }

    // ---------- Shizuku ----------

    private Class<?> sz() throws ClassNotFoundException {
        return Class.forName("rikka.shizuku.Shizuku");
    }

    private boolean ping() {
        try {
            Object r = sz().getMethod("pingBinder").invoke(null);
            return r instanceof Boolean && (Boolean) r;
        } catch (Throwable t) { return false; }
    }

    private int selfPerm() {
        try {
            Object r = sz().getMethod("checkSelfPermission").invoke(null);
            return r instanceof Integer ? (Integer) r : -1;
        } catch (Throwable t) { return -1; }
    }

    private void refreshStatus() {
        boolean on = ping();
        int p = on ? selfPerm() : -1;
        if (!on) {
            statusView.setText("Shizuku：未连接");
            statusView.setTextColor(Color.parseColor("#FFC24B"));
        } else if (p != 0) {
            statusView.setText("Shizuku：已连接，未授权");
            statusView.setTextColor(Color.parseColor("#FFC24B"));
        } else {
            statusView.setText("Shizuku：已连接并授权");
            statusView.setTextColor(Color.parseColor("#3DE1A4"));
        }
    }

    private void doConnect() {
        if (!ping()) {
            toast("正在打开 Shizuku…");
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                if (i == null) { toast("未安装 Shizuku"); return; }
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Throwable t) { toast("打开 Shizuku 失败"); }
            return;
        }
        if (selfPerm() == 0) { toast("已连接并授权"); refreshStatus(); return; }
        toast("正在请求授权…");
        try {
            sz().getMethod("requestPermission", int.class).invoke(null, 1);
        } catch (Throwable t) { toast("请求授权失败：" + t.getClass().getSimpleName()); }
        new Thread(new Runnable() { public void run() {
            for (int i = 0; i < 60; i++) {
                if (selfPerm() == 0) { toast("授权成功"); break; }
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
            }
            ui.post(new Runnable() { public void run() { refreshStatus(); } });
        }}).start();
    }

    private Object service() {
        if (shizukuSvc != null) return shizukuSvc;
        try {
            Object binder = sz().getMethod("getBinder").invoke(null);
            if (binder == null) return null;
            Class<?> w = Class.forName("rikka.shizuku.ShizukuBinderWrapper");
            Object wrap = w.getConstructor(android.os.IBinder.class).newInstance(binder);
            Class<?> stub = Class.forName("moe.shizuku.server.IShizukuService$Stub");
            Object svc = stub.getMethod("asInterface", android.os.IBinder.class).invoke(null, wrap);
            shizukuSvc = svc;
            return svc;
        } catch (Throwable t) { return null; }
    }

    private Object newProc(String cmd) {
        try {
            Object svc = service();
            if (svc == null) return null;
            for (Method m : svc.getClass().getMethods()) {
                if (m.getName().equals("newProcess")) {
                    return m.invoke(svc, new String[]{"sh", "-c", cmd}, null, null);
                }
            }
        } catch (Throwable t) { }
        return null;
    }

    // ---------- 文件选择 ----------

    private void pick(int code, String type) {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType(type);
            startActivityForResult(i, code);
        } catch (Throwable t) { toast("没有可用的文件选择器"); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri u = data.getData();
        String name = displayName(u);
        if (req == REQ_CONF) { confUri = u; pathConf.setText("conf：" + name); }
        if (req == REQ_BIN)  { binUri = u;  pathBin.setText("漏洞程序：" + name); }
    }

    private String displayName(Uri u) {
        try {
            Cursor c = getContentResolver().query(u, null, null, null, null);
            if (c != null) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (c.moveToFirst() && idx >= 0) {
                    String n = c.getString(idx);
                    c.close();
                    return n;
                }
                c.close();
            }
        } catch (Throwable t) { }
        return u.getLastPathSegment();
    }

    private byte[] readAll(Uri u) {
        try {
            InputStream in = getContentResolver().openInputStream(u);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            return bos.toByteArray();
        } catch (Throwable t) { return null; }
    }

    // ---------- 下载漏洞程序 ----------

    private void downloadTimer() {
        toast("开始下载…");
        log("下载漏洞程序：" + TIMER_URL);
        new Thread(new Runnable() { public void run() {
            java.io.InputStream in = null;
            java.io.FileOutputStream out = null;
            try {
                java.net.URL u = new java.net.URL(TIMER_URL);
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(60000);
                c.setInstanceFollowRedirects(true);
                c.setRequestProperty("User-Agent", "Mozilla/5.0");
                int code = c.getResponseCode();
                log("HTTP " + code);
                if (code != 200) { toast("下载失败：HTTP " + code); return; }
                int total = c.getContentLength();
                in = c.getInputStream();
                java.io.File f = new java.io.File(getFilesDir(), "timer");
                out = new java.io.FileOutputStream(f);
                byte[] buf = new byte[16384];
                int n; long done = 0; int last = -1;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    done += n;
                    if (total > 0) {
                        int pct = (int) (done * 100 / total);
                        if (pct / 10 != last / 10) { last = pct; log("已下载 " + pct + "%"); }
                    }
                }
                out.flush();
                out.close(); out = null;
                in.close(); in = null;
                if (done < 1000000) { toast("下载内容异常（" + done + " 字节）"); return; }
                autoTimer = f;
                binUri = null;
                ui.post(new Runnable() { public void run() {
                    pathBin.setText("漏洞程序：已下载（" + (autoTimer.length() / 1024) + " KB）");
                }});
                log("下载完成，共 " + done + " 字节");
                toast("漏洞程序下载完成");
            } catch (Throwable t) {
                log("下载失败：" + t);
                toast("下载失败：" + t.getClass().getSimpleName());
            } finally {
                try { if (out != null) out.close(); } catch (Throwable ignored) { }
                try { if (in != null) in.close(); } catch (Throwable ignored) { }
            }
        }}).start();
    }

    private byte[] readFile(java.io.File f) {
        try {
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            return bos.toByteArray();
        } catch (Throwable t) { return null; }
    }

    // ---------- 执行 ----------

    private void confirmAndRun() {
        final android.app.Dialog d = new android.app.Dialog(this);
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#162836"));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.parseColor("#24455C"));
        box.setBackground(bg);
        box.setPadding(dp(22), dp(20), dp(22), dp(16));

        TextView t = new TextView(this);
        t.setText("确认执行？");
        t.setTextColor(Color.parseColor("#FFC24B"));
        t.setTextSize(19);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(t);

        TextView sub = new TextView(this);
        sub.setText("本工具仅在 小米 18 Pro（hongkong / 内核 6.18.21）上验证成功。");
        sub.setTextColor(Color.parseColor("#E6F3FA"));
        sub.setTextSize(13);
        sub.setLineSpacing(dp(2), 1f);
        sub.setPadding(0, dp(10), 0, dp(8));
        box.addView(sub);

        String[] warns = {
                "其他机型、其他内核版本不保证可用",
                "conf 与内核不匹配可能导致内核崩溃或重启",
                "错误的内核偏移可能造成数据丢失",
                "每次开机只能执行一次，失败请重启"
        };
        for (String w : warns) {
            TextView x = new TextView(this);
            x.setText("·  " + w);
            x.setTextColor(Color.parseColor("#8FA6B8"));
            x.setTextSize(12);
            x.setPadding(dp(2), dp(4), 0, dp(4));
            box.addView(x);
        }

        TextView ask = new TextView(this);
        ask.setText("请确认设备是你自己的，且已选择与当前内核匹配的 conf。");
        ask.setTextColor(Color.parseColor("#6F8798"));
        ask.setTextSize(11);
        ask.setPadding(0, dp(12), 0, dp(14));
        box.addView(ask);

        Button cancel = btn("取消", "#1D3A4E", "#CFE7F5", new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        Button ok = btn("我已了解，继续", "#3DE1A4", "#062018", new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); run(); }
        });
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp1.rightMargin = dp(5);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.leftMargin = dp(5);
        cancel.setLayoutParams(lp1);
        ok.setLayoutParams(lp2);
        row.addView(cancel);
        row.addView(ok);
        box.addView(row);

        d.setContentView(box);
        android.view.Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            android.view.WindowManager.LayoutParams wl = w.getAttributes();
            wl.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            w.setAttributes(wl);
        }
        d.show();
    }

    private void run() {
        if (!ping() || selfPerm() != 0) { toast("请先连接并授权 Shizuku"); return; }
        if (confUri == null) { toast("请先选择 conf 文件"); return; }
        if (binUri == null && autoTimer == null) { toast("请先下载或选择漏洞程序"); return; }

        logView.setText("开始…");
        new Thread(new Runnable() { public void run() {
            byte[] bin = (binUri != null) ? readAll(binUri) : readFile(autoTimer);
            byte[] conf = readAll(confUri);
            if (bin == null || conf == null) { log("读取文件失败"); return; }
            log("漏洞程序大小 " + bin.length + " 字节");
            log("conf  大小 " + conf.length + " 字节");

            if (!push("/data/local/tmp/timer", bin, "755")) { log("推送漏洞程序失败"); return; }
            if (!push("/data/local/tmp/timer.conf", conf, "644")) { log("推送 conf 失败"); return; }
            log("已推送，开始执行");

            Object p = newProc("/data/local/tmp/timer --conf /data/local/tmp/timer.conf fake");
            if (p == null) { log("启动失败：Shizuku 未授权"); return; }
            stream(p);
            log("—— 进程结束 ——");
        }}).start();
    }

    private boolean push(String dst, byte[] data, String mode) {
        try {
            Object p = newProc("cat > " + dst + " && chmod " + mode + " " + dst + " && echo PUSH_OK");
            if (p == null) return false;
            OutputStream os = (OutputStream) p.getClass().getMethod("getOutputStream").invoke(p);
            os.write(data);
            os.flush();
            os.close();
            InputStream is = (InputStream) p.getClass().getMethod("getInputStream").invoke(p);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            p.getClass().getMethod("waitFor").invoke(p);
            return new String(bos.toByteArray(), "UTF-8").contains("PUSH_OK");
        } catch (Throwable t) { return false; }
    }

    private void stream(Object p) {
        try {
            InputStream is = (InputStream) p.getClass().getMethod("getInputStream").invoke(p);
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) {
                final String s = new String(buf, 0, n, "UTF-8");
                ui.post(new Runnable() { public void run() { logView.append(s); } });
            }
        } catch (Throwable t) { log("读取输出失败"); }
    }
}
