package com.uscbus.uscbus;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setTitle("About");
        resize(0.75);
        setContentView(R.layout.activity_about);
        WebView webview = new WebView(this);
        webview.setWebViewClient(new WebViewClient());
        webview.getSettings().setJavaScriptEnabled(true);
        setContentView(webview);
        webview.loadUrl("https://uscbus.com/about/");
    }

    public void resize(double heightIdx) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        this.getWindow().setLayout((int) (width * 0.95), (int) (height * heightIdx));
    }
}
