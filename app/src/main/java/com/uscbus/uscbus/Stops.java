package com.uscbus.uscbus;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Stops extends AppCompatActivity {

    private SwipeRefreshLayout layout;
    List<String> stopList = new ArrayList<>();
    List<String> busList = new ArrayList<>();
    List<String> arrivalList = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    JSONObject mainObject;
    String JSONResult;
    String routeName;
    String routeId;
    Timer refreshTimer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stops);
        layout = findViewById(R.id.refreshStops);
        setTitle("Stops");
        Bundle bundle = getIntent().getExtras();
        routeName = bundle.getString("key");
        routeId = bundle.getString("routeId");
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, stopList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                text1.setTypeface(text1.getTypeface(), Typeface.BOLD);
                text1.setText(stopList.get(position));
                text2.setText(busList.get(position).equals("") ? arrivalList.get(position) :
                        busList.get(position) + " - " + arrivalList.get(position));
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

    @Override
    protected void onDestroy() {
        refreshTimer.cancel();
        super.onDestroy();
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

    private void updateList(){
        try {
            mainObject = new JSONObject(JSONResult);
            JSONArray stopArr = (JSONArray)mainObject.get(routeName+"%"+routeId);
            stopList.clear();
            busList.clear();
            arrivalList.clear();
            for (int i = 0; i < stopArr.length(); i++) {
                JSONObject eachStop = stopArr.getJSONObject(i);
                String stopName = eachStop.getString("stop");
                if (eachStop.has("prediction")){
                    busList.add("");
                    arrivalList.add("No Time Available");
                }
                else{
                    busList.add(eachStop.getString("busNum"));
                    String arrival = eachStop.getString("due");
                    if (arrival.equals("Arriving")){
                    }
                    else if (arrival.equals("1"))
                        arrival += " min";
                    else
                        arrival += " mins";
                    arrivalList.add(arrival);
                }
                stopList.add(stopName);
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
        protected String doInBackground(Void... voids) {
            JSONResult = new Utils().httpRequest("http://www.uscbus.com:8888");
            return JSONResult;
        }

        @Override
        protected void onPostExecute(String s) {
            updateList();
            layout.setRefreshing(false);
        }
    }
    protected void onError(){
        Toast.makeText(Stops.this, "There is an error loading data",
                Toast.LENGTH_LONG).show();
        layout.setRefreshing(false);
    }
}
