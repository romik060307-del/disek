package com.rk.wacloning;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A small Android wrapper for the official WhatsApp Web website. It does not implement WhatsApp APIs. */
public class MainActivity extends Activity {
    private static final String WHATSAPP_WEB = "https://web.whatsapp.com/";
    private static final String PREFS = "wa_cloning_settings";
    private static final String PREF_DESKTOP = "desktop_mode";
    private static final int REQUEST_FILE_PICKER = 301;
    private static final int REQUEST_WEB_PERMISSIONS = 302;
    private static final int REQUEST_DOWNLOAD_PERMISSION = 303;

    private WebView webView;
    private ProgressBar progressBar;
    private TextView modeButton;
    private boolean desktopMode = true;
    private ValueCallback<Uri[]> fileChooserCallback;
    private PermissionRequest pendingWebPermission;
    private String[] pendingWebResources;
    private PendingDownload pendingDownload;

    private static final class PendingDownload {
        final String url;
        final String userAgent;
        final String contentDisposition;
        final String mimeType;

        PendingDownload(String url, String userAgent, String contentDisposition, String mimeType) {
            this.url = url;
            this.userAgent = userAgent;
            this.contentDisposition = contentDisposition;
            this.mimeType = mimeType;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7, 94, 84));
        getWindow().setNavigationBarColor(Color.rgb(16, 23, 20));
        desktopMode = getPreferences(MODE_PRIVATE).getBoolean(PREF_DESKTOP, true);
        buildUi();
        configureWebView();

        if (savedInstanceState != null) {
            try {
                if (webView.restoreState(savedInstanceState) == null) {
                    webView.loadUrl(WHATSAPP_WEB);
                }
            } catch (Exception ignored) {
                webView.loadUrl(WHATSAPP_WEB);
            }
        } else {
            webView.loadUrl(WHATSAPP_WEB);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), 0, dp(8), 0);
        toolbar.setBackgroundColor(Color.rgb(7, 94, 84));
        toolbar.setElevation(dp(2));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        iconParams.rightMargin = dp(10);
        toolbar.addView(icon, iconParams);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("WA Cloning");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitle = new TextView(this);
        subtitle.setText("Pembungkus WhatsApp Web pihak ketiga");
        subtitle.setTextColor(Color.rgb(217, 240, 233));
        subtitle.setTextSize(10);
        titles.addView(title);
        titles.addView(subtitle);
        toolbar.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView menu = new TextView(this);
        menu.setText("⋮");
        menu.setTextSize(28);
        menu.setTextColor(Color.WHITE);
        menu.setGravity(Gravity.CENTER);
        menu.setContentDescription("Menu aplikasi");
        toolbar.addView(menu, new LinearLayout.LayoutParams(dp(40), dp(48)));
        menu.setOnClickListener(v -> showMenu(menu));
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(6), dp(5), dp(6), dp(5));
        bottom.setBackgroundColor(Color.rgb(248, 250, 249));
        bottom.setElevation(dp(3));

        Button reload = makeBottomButton("↻  Muat ulang");
        modeButton = makeBottomButton("");
        Button browser = makeBottomButton("Buka browser");
        reload.setOnClickListener(v -> reloadPage());
        modeButton.setOnClickListener(v -> toggleDesktopMode());
        browser.setOnClickListener(v -> openOfficialSiteInBrowser());
        bottom.addView(reload, new LinearLayout.LayoutParams(0, dp(44), 1));
        bottom.addView(modeButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        bottom.addView(browser, new LinearLayout.LayoutParams(0, dp(44), 1));
        root.addView(bottom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        setContentView(root);
        updateModeButton();
    }

    private Button makeBottomButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setTextColor(Color.rgb(7, 94, 84));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(2), 0, dp(2), 0);
        return button;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        applyUserAgent();

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !request.isForMainFrame()) {
                    return false;
                }
                return routeTopLevelUrl(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeTopLevelUrl(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(8);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this, "Halaman gagal dimuat. Periksa internet lalu tekan Muat ulang.", Toast.LENGTH_LONG).show();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }
                fileChooserCallback = callback;
                try {
                    Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    pick.setType("*/*");
                    String[] accepted = params.getAcceptTypes();
                    ArrayList<String> mimeTypes = new ArrayList<>();
                    if (accepted != null) {
                        for (String item : accepted) {
                            if (item != null && !item.trim().isEmpty() && !item.equals("*/*")) {
                                for (String type : item.split(",")) {
                                    String trimmed = type.trim();
                                    if (!trimmed.isEmpty() && trimmed.contains("/")) mimeTypes.add(trimmed);
                                }
                            }
                        }
                    }
                    if (!mimeTypes.isEmpty()) pick.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toArray(new String[0]));
                    startActivityForResult(Intent.createChooser(pick, "Pilih foto, video, atau file"), REQUEST_FILE_PICKER);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileChooserCallback = null;
                    Toast.makeText(MainActivity.this, "Tidak ada aplikasi pemilih file di perangkat ini.", Toast.LENGTH_LONG).show();
                    return false;
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermissionRequest(request));
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (pendingWebPermission == request) {
                    pendingWebPermission = null;
                    pendingWebResources = null;
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
                Toast.makeText(MainActivity.this,
                        "Unduhan langsung tidak tersedia untuk tautan media ini. Gunakan tombol unduh bawaan WhatsApp jika tersedia.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            startDownload(new PendingDownload(url, userAgent, contentDisposition, mimeType));
        });
    }

    private boolean routeTopLevelUrl(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!(scheme.equals("http") || scheme.equals("https"))) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception ignored) {
                Toast.makeText(this, "Tautan ini tidak dapat dibuka di perangkat.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("whatsapp.com") || host.endsWith(".whatsapp.com") || host.equals("whatsapp.net") || host.endsWith(".whatsapp.net")) {
            return false;
        }
        // Links clicked from conversations open outside the wrapper, rather than replacing the chat.
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
            Toast.makeText(this, "Tidak ada browser untuk membuka tautan tersebut.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void handleWebPermissionRequest(PermissionRequest request) {
        if (request == null) return;
        String host = request.getOrigin() == null ? "" : request.getOrigin().getHost();
        if (host == null || !(host.equals("web.whatsapp.com") || host.endsWith(".whatsapp.com"))) {
            request.deny();
            return;
        }
        ArrayList<String> needed = new ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource) &&
                    checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.CAMERA);
            }
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource) &&
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.RECORD_AUDIO);
            }
        }
        if (needed.isEmpty()) {
            request.grant(request.getResources());
        } else {
            pendingWebPermission = request;
            pendingWebResources = request.getResources();
            requestPermissions(needed.toArray(new String[0]), REQUEST_WEB_PERMISSIONS);
        }
    }

    private void startDownload(PendingDownload download) {
        if (download == null) return;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingDownload = download;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_DOWNLOAD_PERMISSION);
            return;
        }
        enqueueDownload(download);
    }

    private void enqueueDownload(PendingDownload download) {
        try {
            Uri uri = Uri.parse(download.url);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            String filename = URLUtil.guessFileName(download.url, download.contentDisposition, download.mimeType);
            filename = new File(filename).getName().replaceAll("[\\\\/:*?\"<>|]", "_");
            if (filename.trim().isEmpty()) filename = "wa-media-download";
            request.setTitle(filename);
            request.setDescription("Mengunduh melalui WA Cloning");
            if (download.mimeType != null && !download.mimeType.trim().isEmpty()) request.setMimeType(download.mimeType);
            if (download.userAgent != null && !download.userAgent.isEmpty()) request.addRequestHeader("User-Agent", download.userAgent);
            String cookie = CookieManager.getInstance().getCookie(download.url);
            if (cookie != null && !cookie.isEmpty()) request.addRequestHeader("Cookie", cookie);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (manager == null) throw new IllegalStateException("Download manager tidak tersedia");
            manager.enqueue(request);
            Toast.makeText(this, "Unduhan dimulai. Periksa folder Download.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unduhan gagal dimulai. Coba unduh melalui menu bawaan WhatsApp.", Toast.LENGTH_LONG).show();
        }
    }

    private void applyUserAgent() {
        if (webView == null) return;
        WebSettings settings = webView.getSettings();
        if (!desktopMode) {
            settings.setUserAgentString(WebSettings.getDefaultUserAgent(this));
            settings.setUseWideViewPort(false);
            settings.setLoadWithOverviewMode(false);
        } else {
            String current = WebSettings.getDefaultUserAgent(this);
            Matcher matcher = Pattern.compile("Chrome/([0-9.]+)").matcher(current);
            String chromeVersion = matcher.find() ? matcher.group(1) : "120.0.0.0";
            settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + chromeVersion + " Safari/537.36");
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
        }
    }

    private void toggleDesktopMode() {
        desktopMode = !desktopMode;
        getPreferences(MODE_PRIVATE).edit().putBoolean(PREF_DESKTOP, desktopMode).apply();
        applyUserAgent();
        updateModeButton();
        Toast.makeText(this, desktopMode ? "Mode desktop aktif" : "Mode seluler aktif", Toast.LENGTH_SHORT).show();
        webView.reload();
    }

    private void updateModeButton() {
        if (modeButton != null) modeButton.setText(desktopMode ? "Desktop ✓" : "Mode seluler");
    }

    private void reloadPage() {
        if (!isOnline()) {
            Toast.makeText(this, "Koneksi internet tidak terdeteksi.", Toast.LENGTH_SHORT).show();
        }
        webView.reload();
    }

    private boolean isOnline() {
        try {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network active = manager.getActiveNetwork();
                android.net.NetworkCapabilities capabilities = manager.getNetworkCapabilities(active);
                return capabilities != null && capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
            NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception ignored) {
            return true;
        }
    }

    private void openOfficialSiteInBrowser() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_WEB));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Browser tidak ditemukan.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(Menu.NONE, 1, 1, "Muat ulang WhatsApp Web");
        popup.getMenu().add(Menu.NONE, 2, 2, desktopMode ? "Aktifkan mode seluler" : "Aktifkan mode desktop");
        popup.getMenu().add(Menu.NONE, 3, 3, "Buka WhatsApp Web di browser");
        popup.getMenu().add(Menu.NONE, 4, 4, "Cara menautkan akun");
        popup.getMenu().add(Menu.NONE, 5, 5, "Tentang & batasan");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    reloadPage();
                    return true;
                case 2:
                    toggleDesktopMode();
                    return true;
                case 3:
                    openOfficialSiteInBrowser();
                    return true;
                case 4:
                    showInfo("Cara menautkan akun",
                            "1. Buka WhatsApp utama di ponsel.\n2. Pilih Perangkat tertaut > Tautkan perangkat.\n3. Pindai QR yang tampil di sini, atau gunakan metode nomor telepon jika pilihan itu tersedia di WhatsApp Web.\n\nGunakan hanya akun milikmu sendiri dan jangan bagikan kode tautan.");
                    return true;
                case 5:
                    showInfo("Tentang WA Cloning",
                            "Aplikasi pihak ketiga ini hanya membuka situs resmi web.whatsapp.com; bukan aplikasi resmi WhatsApp dan tidak memiliki akses ke server WhatsApp.\n\nCookie WebView disimpan oleh Android sehingga sesi biasanya bertahan saat aplikasi ditutup. WhatsApp tetap dapat meminta tautan ulang atau memutuskan sesi.\n\nUnduhan hanya bekerja untuk tautan HTTP/HTTPS yang disediakan halaman. Media status, tautan blob, dan pesan sekali lihat tidak bisa dijamin dapat disimpan. Aplikasi ini tidak melewati batasan privasi WhatsApp.");
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void showInfo(String title, String message) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Mengerti", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILE_PICKER) {
            if (fileChooserCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null) {
                    Uri uri = data.getData();
                    if (uri != null) results = new Uri[]{uri};
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                }
                fileChooserCallback.onReceiveValue(results);
                fileChooserCallback = null;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0;
        for (int result : grantResults) granted &= result == PackageManager.PERMISSION_GRANTED;

        if (requestCode == REQUEST_WEB_PERMISSIONS) {
            if (pendingWebPermission != null) {
                if (granted) pendingWebPermission.grant(pendingWebResources);
                else pendingWebPermission.deny();
            }
            pendingWebPermission = null;
            pendingWebResources = null;
        } else if (requestCode == REQUEST_DOWNLOAD_PERMISSION) {
            PendingDownload download = pendingDownload;
            pendingDownload = null;
            if (granted && download != null) enqueueDownload(download);
            else Toast.makeText(this, "Izin penyimpanan diperlukan pada Android lama untuk mengunduh file.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            webView.onPause();
            CookieManager.getInstance().flush();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(null);
            fileChooserCallback = null;
        }
        if (pendingWebPermission != null) {
            try { pendingWebPermission.deny(); } catch (Exception ignored) { }
            pendingWebPermission = null;
        }
        if (webView != null) {
            CookieManager.getInstance().flush();
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
