package com.uscbus.uscbus;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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

public class Stops extends AppCompatActivity {

    private SwipeRefreshLayout layout;
    List<String> stopList = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    JSONObject mainObject;
    String JSONResult;
    String routeName;
    String routeId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stops);
        layout = findViewById(R.id.refreshStops);
        setTitle("Stops");
        Bundle bundle = getIntent().getExtras();
        JSONResult = bundle.getString("json");
        routeName = bundle.getString("key");
        routeId = bundle.getString("routeId");
        arrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, stopList);
        ((ListView)findViewById(R.id.listViewStop)).setAdapter(arrayAdapter);
        updateList();
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new FetchSchedule().execute();
                updateList();
            }
        });
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
            for (int i = 0; i < stopArr.length(); i++) {
                JSONObject eachStop = stopArr.getJSONObject(i);
                String result = eachStop.getString("stop");
                if (eachStop.has("prediction")){
                    result += " - No Arriving Time";
                }
                else{
                    result += " - " + eachStop.getString("busNum") + " - ";
                    String arrival = eachStop.getString("due");
                    if (arrival.equals("Arriving")){
                        result += arrival;
                    }
                    else if (arrival.equals("1"))
                        result += arrival + "min";
                    else
                        result += arrival + "mins";
                }
                stopList.add(result);
            }
            arrayAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }

    public class FetchSchedule extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            URL uscBusUrl = null;
            StringBuffer response = new StringBuffer();
            try{
                uscBusUrl = new URL("http://www.uscbus.com:8888");
            }
            catch (MalformedURLException e){
                e.printStackTrace();
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) uscBusUrl.openConnection();
                conn.setDoOutput(false);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

                // handle the response
                int status = conn.getResponseCode();
                if (status != 200) {
                    throw new IOException("Post failed with error code " + status);
                } else {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                JSONResult = response.toString();
                Log.d("JSON", JSONResult);
                return JSONResult;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            updateList();
            layout.setRefreshing(false);
        }
    }
}
