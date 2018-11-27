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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

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
    List<List<String>> arrivalTimeList = new ArrayList<>();
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
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.stop_list_item, android.R.id.text1, stopList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.stop_list_item, null,true);
                updateListItems(position, view);
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
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.stop_detail_item, busList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.stop_detail_item, null,true);
                TextView busId = view.findViewById(R.id.stop_detail_bus_id);
                if (position == 0 && busList.get(position).equals("")) {
                    busId.setText("Currently there is no bus arrival time available.");
                    return view;
                }
                String busName = "Bus " + busList.get(position);
                busId.setText(busName);
                String arrival = arrivalList.get(position);
                if (arrival.equals("0"))
                    arrival = "Arriving";
                else if (arrival.equals("1"))
                    arrival = "1 min";
                else
                    arrival += " mins";
                TextView arrivalTextView = view.findViewById(R.id.stop_detail_time);
                arrivalTextView.setText(arrival);
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
                        startBackgroundService(bus, stopId, stopName);
                    }
                });
            }
        });
        arrayAdapter.notifyDataSetChanged();
    }

    void startBackgroundService(String bus, String stopId, String stopName){
        Intent intent = new Intent(Stops.this, NotificationService.class);
        intent.putExtra("busId", bus);
        intent.putExtra("stopId", stopId);
        intent.putExtra("routeId", routeId);
        intent.putExtra("routeName", routeName);
        intent.putExtra("stopName", stopName);
        intent.setAction(Constants.ACTION.START_FOREGROUND_ACTION);
        startService(intent);
    }

    private void updateListItems(int position, View view) {
        String stopNameString = stopList.get(position);
        String stopIdString = "Stop " + stopIdList.get(position);
        String arrivalString = "";
        String arrivalBus = "";
        String arrivalTimeStr = "";
        List<String> eachBusList = busList.get(position);
        List<String> eachDueList = arrivalList.get(position);
        List<String> eachArrivalTimeList = arrivalTimeList.get(position);

        for (int i = 0; i < eachBusList.size(); i++) {
            arrivalTimeStr = eachArrivalTimeList.get(i);
            arrivalBus = "#" + eachBusList.get(i);
            if (eachDueList.get(i).equals("0")) {
                arrivalString += ("Arriving");
            } else if (eachDueList.get(i).equals("1")) {
                arrivalString += ("1 min");
            } else
                arrivalString += (eachDueList.get(i) + " mins");
            break;
        }
        if (eachBusList.isEmpty()) {
            arrivalBus = "Not available";
        }
        TextView stopName = view.findViewById(R.id.stop_name);
        TextView arrivalEta = view.findViewById(R.id.bus_eta);
        TextView busId = view.findViewById(R.id.bus_id);
        TextView arrivalTime = view.findViewById(R.id.arrival_time);
        stopName.setText(stopNameString);
        arrivalEta.setText(arrivalString);
        busId.setText(arrivalBus);
        arrivalTime.setText(arrivalTimeStr);
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
            arrivalTimeList.clear();
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
                List<String> eachArrivalTimeList = new ArrayList<>();
                for (int j = 0; j < arrivalArr.length(); j++) {
                    JSONObject eachArrival = arrivalArr.getJSONObject(j);
                    eachBusList.add(eachArrival.getString("busNum"));
                    eachDueList.add(eachArrival.getString("due"));
                    eachArrivalTimeList.add(eachArrival.getString("arriveTime"));
                }
                busList.add(eachBusList);
                arrivalList.add(eachDueList);
                arrivalTimeList.add(eachArrivalTimeList);
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
