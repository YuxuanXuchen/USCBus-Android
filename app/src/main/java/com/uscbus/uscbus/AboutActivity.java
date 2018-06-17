package com.uscbus.uscbus;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.w3c.dom.Text;

public class AboutActivity extends AppCompatActivity {

    TextView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resize(0.75);
        setContentView(R.layout.activity_about);
        view = findViewById(R.id.aboutText);
        String about = "The developers and maintainers of the USC Bus mobile app are not affiliated with the University of Southern California.\n" +
                "\n" +
                "Privacy policy of the USC Bus app can be found at \nuscbus.com/usc-bus-app-privacy-policy\n" +
                "\n" +
                "Please send feedback and suggestions to yuxuanchen1995@gmail.com\n" +
                "\n" +
                "We also welcome help to build the iOS version of the app.\n" +
                "\n" +
                "Yuxuan Chen\n";
        view.setText(about);
    }

    public void resize(double heightIdx) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        this.getWindow().setLayout((int) (width * 0.95), (int) (height * heightIdx));
    }

}
