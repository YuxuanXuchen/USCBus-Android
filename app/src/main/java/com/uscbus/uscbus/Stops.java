package com.uscbus.uscbus;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("refresh", "on Create");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stops);
        layout = findViewById(R.id.refreshStops);
        setTitle("Stops");
        Bundle bundle = getIntent().getExtras();
        routeName = bundle.getString("key");
        routeId = bundle.getString("routeId");
        layout.setRefreshing(true);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, stopList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                text1.setTypeface(text1.getTypeface(), Typeface.BOLD);
                StringBuilder sbSubText = new StringBuilder();
                String stopNameString = stopList.get(position);
                String stopIdString = "Stop "+ stopIdList.get(position);
                String arrivalString = "";
                String arrivalBus = "";
                List<String> eachBusList = busList.get(position);
                List<String> eachDueList = arrivalList.get(position);

                for (int i = 0; i < eachBusList.size(); i++) {
                    // the first bus coming
                    if (i == 0){
                        arrivalBus = "#"+eachBusList.get(i);
                        if (eachDueList.get(i).equals("0")) {
                            arrivalString += ("Arriving");
                        } else if (eachDueList.get(i).equals("1")) {
                            arrivalString += ("1 min");
                        } else
                            arrivalString += (eachDueList.get(i) + " mins");
                        continue;
                    }
                    sbSubText.append("#"+eachBusList.get(i));
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
                return view;
            }
        };
        ((ListView) findViewById(R.id.listViewStop)).setAdapter(arrayAdapter);
        new FetchSchedule().execute();
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new FetchSchedule().execute();
            }
        });
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new refreshTask(), 30000, 30000);
    }

    private void setLeftRightString(TextView view, String leftText, String rightText){
        String fullText = leftText + "\n " + rightText;     // only works if  linefeed between them! "\n ";

        int fullTextLength = fullText.length();
        int leftEnd = leftText.length();
        int rightTextLength = rightText.length();

        final SpannableString s = new SpannableString(fullText);
        AlignmentSpan alignmentSpan = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE);
        s.setSpan(alignmentSpan, leftEnd, fullTextLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new SetLineOverlap(true), 1, fullTextLength-2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new SetLineOverlap(false), fullTextLength-1, fullTextLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
    protected void onRestart() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new refreshTask(), 0, 30000);
        Log.d("refresh", "on Restart");
        super.onRestart();
    }

    private class refreshTask extends TimerTask {
        @Override
        public void run() {
            Stops.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Stops.this, "Data Updated",
                            Toast.LENGTH_SHORT).show();
                }
            });
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
        Intent intent = new Intent(Stops.this, MapActivity.class);
        intent.putExtra("routeId", routeId);
        startActivity(intent);
        return true;
    }

    private void updateList() {
        JSONArray arr;
//        JSONResult = "[{\"routeName\": \"Alhambra \", \"routeTime\": [{\"stopId\": \"500974\", \"time\": [], \"stopName\": \"Alhambra\"}, {\"stopId\": \"292024\", \"time\": [], \"stopName\": \"Norris Cancer Hospital\"}], \"routeId\": \"1391\"}, {\"routeName\": \"C Route Summer 2018\", \"routeTime\": [{\"stopId\": \"2309555\", \"time\": [], \"stopName\": \"Leavey Library\"}, {\"stopId\": \"384502\", \"time\": [], \"stopName\": \"JEP House\"}, {\"stopId\": \"2309556\", \"time\": [], \"stopName\": \"Dental School\"}, {\"stopId\": \"39109\", \"time\": [], \"stopName\": \"34th/McClintock\"}, {\"stopId\": \"1113621\", \"time\": [], \"stopName\": \"Cardinal Gardens\"}, {\"stopId\": \"3720219\", \"time\": [], \"stopName\": \"30th & Orchard\"}, {\"stopId\": \"44601\", \"time\": [], \"stopName\": \"Terrace Apts.\"}, {\"stopId\": \"44561\", \"time\": [], \"stopName\": \"2726-2816 Menlo Ave\"}, {\"stopId\": \"44562\", \"time\": [], \"stopName\": \"2658 Menlo Ave\"}, {\"stopId\": \"395045\", \"time\": [], \"stopName\": \"Adams/Menlo\"}, {\"stopId\": \"44604\", \"time\": [], \"stopName\": \"2632 Ellendale Pl.\"}, {\"stopId\": \"44603\", \"time\": [], \"stopName\": \"2700 Ellendale Pl.\"}, {\"stopId\": \"44616\", \"time\": [], \"stopName\": \"28th & Orchard Ave.\"}, {\"stopId\": \"44607\", \"time\": [], \"stopName\": \"University Regents Apts.\"}, {\"stopId\": \"498234\", \"time\": [], \"stopName\": \"25th and Magnolia\"}, {\"stopId\": \"498235\", \"time\": [], \"stopName\": \"24th and Magnolia\"}, {\"stopId\": \"498230\", \"time\": [], \"stopName\": \"24th St Theater\"}, {\"stopId\": \"2578359\", \"time\": [], \"stopName\": \"23rd and Portland\"}, {\"stopId\": \"44572\", \"time\": [], \"stopName\": \"2341 Portland\"}, {\"stopId\": \"2578361\", \"time\": [], \"stopName\": \"Portland and Adams\"}, {\"stopId\": \"44624\", \"time\": [], \"stopName\": \"Sierra Apts.\"}, {\"stopId\": \"44625\", \"time\": [], \"stopName\": \"Founders Apts.\"}, {\"stopId\": \"498262\", \"time\": [], \"stopName\": \"Hillview Apts\"}, {\"stopId\": \"498263\", \"time\": [], \"stopName\": \"Pacific Apts\"}, {\"stopId\": \"44631\", \"time\": [], \"stopName\": \"28th St. & University\"}, {\"stopId\": \"44634\", \"time\": [], \"stopName\": \"Annenberg House\"}, {\"stopId\": \"44635\", \"time\": [], \"stopName\": \"Stardust Apts.\"}], \"routeId\": \"8559\"}, {\"routeName\": \"Circuit Tram\", \"routeTime\": [{\"stopId\": \"1694684\", \"time\": [], \"stopName\": \"Lot 71\"}, {\"stopId\": \"292023\", \"time\": [{\"arriveTime\": \"2:20 PM\", \"busNum\": \"A801\", \"due\": \"1\"}], \"stopName\": \"Eastlake & Biggy\"}, {\"stopId\": \"1694687\", \"time\": [{\"arriveTime\": \"2:25 PM\", \"busNum\": \"A801\", \"due\": \"5\"}], \"stopName\": \"Outpatient (OPD)\"}, {\"stopId\": \"438551\", \"time\": [{\"arriveTime\": \"2:28 PM\", \"busNum\": \"A801\", \"due\": \"8\"}], \"stopName\": \"Busway\"}, {\"stopId\": \"1694688\", \"time\": [{\"arriveTime\": \"2:31 PM\", \"busNum\": \"A801\", \"due\": \"11\"}], \"stopName\": \"Healthcare Research (HRA)\"}, {\"stopId\": \"438553\", \"time\": [{\"arriveTime\": \"2:33 PM\", \"busNum\": \"A801\", \"due\": \"13\"}], \"stopName\": \"LAC/USC Med Center\"}], \"routeId\": \"3420\"}, {\"routeName\": \"City Center (ATT)\", \"routeTime\": [{\"stopId\": \"2475284\", \"time\": [], \"stopName\": \"Jefferson & Hoover\"}, {\"stopId\": \"1769467\", \"time\": [], \"stopName\": \"City Center (ATT)\"}], \"routeId\": \"3630\"}, {\"routeName\": \"Football Shuttle\", \"routeTime\": [{\"stopId\": \"2908675\", \"time\": [], \"stopName\": \"Grand Ave Structure\"}, {\"stopId\": \"2908676\", \"time\": [], \"stopName\": \"Pardee Entrance\"}, {\"stopId\": \"2908677\", \"time\": [], \"stopName\": \"Expo at Pardee\"}, {\"stopId\": \"2553818\", \"time\": [], \"stopName\": \"PC on Hope\"}], \"routeId\": \"6518\"}, {\"routeName\": \"Marina Del Rey\", \"routeTime\": [{\"stopId\": \"2553842\", \"time\": [], \"stopName\": \"37th Place and Watt Way\"}, {\"stopId\": \"1887958\", \"time\": [], \"stopName\": \"ICT\"}, {\"stopId\": \"2868675\", \"time\": [], \"stopName\": \"ISI (new stop)\"}], \"routeId\": \"1267\"}, {\"routeName\": \"Parking Center Summer 2018\", \"routeTime\": [{\"stopId\": \"384508\", \"time\": [], \"stopName\": \"Parking Center\"}, {\"stopId\": \"395067\", \"time\": [], \"stopName\": \"CAL North\"}, {\"stopId\": \"2309555\", \"time\": [], \"stopName\": \"Leavey Library\"}, {\"stopId\": \"384502\", \"time\": [{\"arriveTime\": \"2:24 PM\", \"busNum\": \"A805\", \"due\": \"4\"}], \"stopName\": \"JEP House\"}, {\"stopId\": \"2309556\", \"time\": [{\"arriveTime\": \"2:26 PM\", \"busNum\": \"A805\", \"due\": \"6\"}], \"stopName\": \"Dental School\"}, {\"stopId\": \"39109\", \"time\": [{\"arriveTime\": \"2:26 PM\", \"busNum\": \"A805\", \"due\": \"6\"}], \"stopName\": \"34th/McClintock\"}, {\"stopId\": \"2868672\", \"time\": [{\"arriveTime\": \"2:27 PM\", \"busNum\": \"A805\", \"due\": \"7\"}], \"stopName\": \"Childs and McClintock\"}, {\"stopId\": \"2553842\", \"time\": [{\"arriveTime\": \"2:29 PM\", \"busNum\": \"A805\", \"due\": \"9\"}], \"stopName\": \"37th Place and Watt Way\"}, {\"stopId\": \"1055409\", \"time\": [{\"arriveTime\": \"\", \"busNum\": \"A750\", \"due\": \"0\"}, {\"arriveTime\": \"2:32 PM\", \"busNum\": \"A805\", \"due\": \"12\"}], \"stopName\": \"Gate 2\"}, {\"stopId\": \"395069\", \"time\": [{\"arriveTime\": \"2:23 PM\", \"busNum\": \"A750\", \"due\": \"3\"}, {\"arriveTime\": \"2:35 PM\", \"busNum\": \"A805\", \"due\": \"15\"}], \"stopName\": \"Transit Ctr (7am-6pm)\"}, {\"stopId\": \"384507\", \"time\": [{\"arriveTime\": \"2:24 PM\", \"busNum\": \"A750\", \"due\": \"4\"}, {\"arriveTime\": \"2:36 PM\", \"busNum\": \"A805\", \"due\": \"16\"}], \"stopName\": \"RAN (7am-6pm)\"}, {\"stopId\": \"384499\", \"time\": [{\"arriveTime\": \"2:26 PM\", \"busNum\": \"A750\", \"due\": \"6\"}, {\"arriveTime\": \"2:37 PM\", \"busNum\": \"A805\", \"due\": \"17\"}], \"stopName\": \"CAL South (7am-6pm)\"}], \"routeId\": \"4598\"}, {\"routeName\": \"Soto\", \"routeTime\": [{\"stopId\": \"318319\", \"time\": [], \"stopName\": \"Soto\"}, {\"stopId\": \"292023\", \"time\": [], \"stopName\": \"Eastlake & Biggy\"}, {\"stopId\": \"318343\", \"time\": [], \"stopName\": \"Keck Medical Center\"}, {\"stopId\": \"2658491\", \"time\": [], \"stopName\": \"CSC\"}, {\"stopId\": \"1688105\", \"time\": [], \"stopName\": \"Soto II\"}], \"routeId\": \"3404\"}]";
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
            JSONResult = new Utils().httpRequest("http://apidata.uscbus.com:8888");
            return JSONResult;
        }

        @Override
        protected void onPostExecute(String s) {
            updateList();
            layout.setRefreshing(false);
        }
    }

    protected void onError() {
        Toast.makeText(Stops.this, "There is an error loading data",
                Toast.LENGTH_LONG).show();
        layout.setRefreshing(false);
    }
}
