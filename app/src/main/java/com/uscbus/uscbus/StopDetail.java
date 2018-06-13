package com.uscbus.uscbus;

import android.content.res.Configuration;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.LineHeightSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class StopDetail extends AppCompatActivity {

    ArrayAdapter arrayAdapter;
    List<String> busList;
    List<String> arrivalList;
    ListView stopDetailLv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_detail);
        Bundle bundle = getIntent().getExtras();
        String stopName = bundle.getString("stopName");
        String stopId = bundle.getString("stopId");
        busList = bundle.getStringArrayList("busList");
        arrivalList = bundle.getStringArrayList("arrivalList");
        ((TextView) findViewById(R.id.stopId)).setText("Stop " + stopId);
        ((TextView) findViewById(R.id.stopName)).setText(stopName);
        Log.d("stopdetail", busList.toString());
        if (busList.isEmpty()) {
            busList.add("");
            arrivalList.add("");
            resize(0.3);
        }
        else
            resize(0.4);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, busList){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (position == 0 && busList.get(position).equals("")){
                    textView.setText("Currently there is no bus arrival time available.");
                    return view;
                }
                textView.setTextSize(20);
                String busName = "Bus " + busList.get(position);
                String arrival = arrivalList.get(position);
                if (arrival.equals("0"))
                    arrival = "Arriving";
                else if (arrival.equals("1"))
                    arrival = "1 min";
                else
                    arrival += " mins";
                setLeftRightString(textView, busName, arrival);
                return view;
            }
        };
        stopDetailLv = findViewById(R.id.stopDetail);
        stopDetailLv.setAdapter(arrayAdapter);
        arrayAdapter.notifyDataSetChanged();
    }

    public void resize(double heightIdx) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        this.getWindow().setLayout((int) (width * 0.95), (int) (height * heightIdx));
    }

    private void setLeftRightString(TextView view, String leftText, String rightText) {
        String fullText = leftText + "\n " + rightText;     // only works if  linefeed between them! "\n ";

        int fullTextLength = fullText.length();
        int leftEnd = leftText.length();

        final SpannableString s = new SpannableString(fullText);
        AlignmentSpan alignmentSpan = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE);
        s.setSpan(alignmentSpan, leftEnd, fullTextLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new SetLineOverlap(true), 1, fullTextLength - 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new SetLineOverlap(false), fullTextLength - 1, fullTextLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(s);
    }

    private static class SetLineOverlap implements LineHeightSpan {
        private int originalBottom = 15;        // init value ignored
        private int originalDescent = 13;       // init value ignored
        private Boolean overlap;                // saved state
        private Boolean overlapSaved = false;   // ensure saved values only happen once

        SetLineOverlap(Boolean overlap) {
            this.overlap = overlap;
        }

        @Override
        public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v,
                                 Paint.FontMetricsInt fm) {
            if (overlap) {
                if (!overlapSaved) {
                    originalBottom = fm.bottom;
                    originalDescent = fm.descent;
                    overlapSaved = true;
                }
                fm.bottom += fm.top;
                fm.descent += fm.top;
            } else {
                // restore saved values
                fm.bottom = originalBottom;
                fm.descent = originalDescent;
                overlapSaved = false;
            }
        }
    }
}
