package com.uscbus.uscbus;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Route Map");
        setContentView(R.layout.activity_map);
        Bundle bundle = getIntent().getExtras();
        String routeId = bundle.getString("routeId");
        WebView webview = new WebView(this);
        webview.setWebViewClient(new WebViewClient());
        webview.getSettings().setJavaScriptEnabled(true);
        setContentView(webview);
        String url = "https://www.uscbuses.com/m/regions/0/routes/" + routeId + "/map";
        webview.loadUrl(url);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
