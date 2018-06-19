package com.uscbus.uscbus;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.LineHeightSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Stops extends AppCompatActivity {

    private SwipeRefreshLayout layout;
    List<String> stopList = new ArrayList<>();
    List<String> stopIdList = new ArrayList<>();
    List<List<String>> busList = new ArrayList<>();
    List<List<String>> arrivalList = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    String JSONResult;
    String routeName;
    String routeId;
    Timer refreshTimer;
    final int REFRESH_TIME = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("refresh", "on Create");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stops);
        layout = findViewById(R.id.refreshStops);
        Bundle bundle = getIntent().getExtras();
        routeName = bundle.getString("key");
        routeId = bundle.getString("routeId");
        setTitle(routeName);
        layout.setRefreshing(true);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, stopList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                updateListItems(position, text1, text2);
                return view;
            }
        };
        ((ListView) findViewById(R.id.listViewStop)).setAdapter(arrayAdapter);
        ((ListView) findViewById(R.id.listViewStop)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Stops.this);
                View layoutView = getLayoutInflater().inflate(R.layout.stop_detail, null);
                showDialog(stopList.get(position), stopIdList.get(position), busList.get(position), arrivalList.get(position), layoutView);
                builder.setView(layoutView);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        new FetchSchedule().execute();
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new FetchSchedule().execute();
            }
        });
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new refreshTask(), REFRESH_TIME, REFRESH_TIME);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void showDialog(final String stopName, final String stopId, final List<String> busList, final List<String> arrivalList, View mView) {
        ArrayAdapter arrayAdapter;
        ListView stopDetailLv;

        ((TextView) mView.findViewById(R.id.stopId)).setText("Stop " + stopId);
        ((TextView) mView.findViewById(R.id.stopName)).setText(stopName);
        Log.d("stopdetail", busList.toString());
        if (busList.isEmpty()) {
            busList.add("");
            arrivalList.add("");
        }
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, busList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (position == 0 && busList.get(position).equals("")) {
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
        stopDetailLv = mView.findViewById(R.id.stopDetail);
        stopDetailLv.setAdapter(arrayAdapter);
        stopDetailLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String bus = busList.get(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(Stops.this);
                View layoutView = getLayoutInflater().inflate(R.layout.confirm_tracking, null, false);
                TextView promptText = layoutView.findViewById(R.id.confirmPromp);
                if (bus.equals("")) {
                    return;
                }
                promptText.setText("Do yo want to track Bus " + bus + " at " + stopName + "?");
                builder.setView(layoutView);
                final AlertDialog dialog = builder.create();
                dialog.show();
                (layoutView.findViewById(R.id.cancelTrack)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                (layoutView.findViewById(R.id.confirmTrack)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        startBackgroundService(bus, stopId);
                    }
                });
            }
        });
        arrayAdapter.notifyDataSetChanged();
    }

    void startBackgroundService(String bus, String stopId){
        Intent intent = new Intent(Stops.this, NotificationService.class);
        intent.putExtra("busId", bus);
        intent.putExtra("stopId", stopId);
        intent.putExtra("routeId", routeId);
        intent.putExtra("routeName", routeName);
        intent.setAction(Constants.ACTION.START_FOREGROUND_ACTION);
        startService(intent);
    }

    private void updateListItems(int position, TextView text1, TextView text2) {
        text1.setTypeface(text1.getTypeface(), Typeface.BOLD);
        StringBuilder sbSubText = new StringBuilder();
        String stopNameString = stopList.get(position);
        String stopIdString = "Stop " + stopIdList.get(position);
        String arrivalString = "";
        String arrivalBus = "";
        List<String> eachBusList = busList.get(position);
        List<String> eachDueList = arrivalList.get(position);

        for (int i = 0; i < eachBusList.size(); i++) {
            // the first bus coming
            if (i == 0) {
                arrivalBus = "#" + eachBusList.get(i);
                if (eachDueList.get(i).equals("0")) {
                    arrivalString += ("Arriving");
                } else if (eachDueList.get(i).equals("1")) {
                    arrivalString += ("1 min");
                } else
                    arrivalString += (eachDueList.get(i) + " mins");
                continue;
            }
            sbSubText.append("#" + eachBusList.get(i));
            if (eachDueList.get(i).equals("0")) {
                sbSubText.append(" Arriving");
            } else if (eachDueList.get(i).equals("1")) {
                sbSubText.append(" 1 min");
            } else
                sbSubText.append(" " + eachDueList.get(i) + " mins");
            sbSubText.append("\n");
        }
        if (eachBusList.isEmpty()) {
            arrivalBus = "Not available";
        }
        text2.setTextColor(Color.parseColor("#a6a6a6"));
        setLeftRightString(text1, stopNameString, arrivalString);
        setLeftRightString(text2, stopIdString, arrivalBus);
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

    @Override
    protected void onPause() {
        refreshTimer.cancel();
        refreshTimer.purge();
        refreshTimer = null;
        Log.d("refresh", "on Pause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (refreshTimer == null) {
            refreshTimer = new Timer();
            refreshTimer.scheduleAtFixedRate(new refreshTask(), 0, REFRESH_TIME);
            Log.d("refresh", "on Resume");
        }
    }

    @Override
    protected void onRestart() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new refreshTask(), 0, REFRESH_TIME);
        Log.d("refresh", "on Restart");
        super.onRestart();
    }

    private class refreshTask extends TimerTask {
        @Override
        public void run() {
            new FetchSchedule().execute();
            Log.d("refresh", "refresh fired");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mapIcon) {
            Intent intent = new Intent(Stops.this, MapActivity.class);
            intent.putExtra("routeId", routeId);
            startActivity(intent);
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return true;
    }

    private void processJSON() {
        JSONArray arr;
        if (Constants.DEBUG_JSON)
            JSONResult = Constants.TEST_JSON;
        try {
            arr = new JSONArray(JSONResult);
            JSONObject routeObj = null;
            for (int i = 0; i < arr.length(); i++) {
                try {
                    JSONObject currObj = arr.getJSONObject(i);
                    if (currObj.getString("routeName").equals(routeName) &&
                            currObj.getString("routeId").equals(routeId)) {
                        routeObj = currObj;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            stopList.clear();
            busList.clear();
            arrivalList.clear();
            stopIdList.clear();
            if (routeObj == null) {
                Log.d("json", "no match");
                return;
            }
            JSONArray stopArr = routeObj.getJSONArray("routeTime");
            for (int i = 0; i < stopArr.length(); i++) {
                JSONObject eachStop = stopArr.getJSONObject(i);
                String stopName = eachStop.getString("stopName");
                String stopId = eachStop.getString("stopId");
                JSONArray arrivalArr = eachStop.getJSONArray("time");
                List<String> eachBusList = new ArrayList<>();
                List<String> eachDueList = new ArrayList<>();
                for (int j = 0; j < arrivalArr.length(); j++) {
                    JSONObject eachArrival = arrivalArr.getJSONObject(j);
                    eachBusList.add(eachArrival.getString("busNum"));
                    eachDueList.add(eachArrival.getString("due"));
                }
                busList.add(eachBusList);
                arrivalList.add(eachDueList);
                stopList.add(stopName);
                stopIdList.add(stopId);
            }
            arrayAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
            onError();
            return;
        }
    }

    public class FetchSchedule extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            JSONResult = new Utils().httpRequest(Constants.RELEASE_API_URL);
            return JSONResult;
        }

        @Override
        protected void onPostExecute(String s) {
            processJSON();
            layout.setRefreshing(false);
        }
    }

    protected void onError() {
        Toast.makeText(Stops.this, "There is an error loading data",
                Toast.LENGTH_LONG).show();
        layout.setRefreshing(false);
    }
}
