package com.uscbus.uscbus;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.JsonReader;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView listViewObj;
    private SwipeRefreshLayout layout;
    List<String> routeList = new ArrayList<>();
    List<String> routeIdList = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    JSONObject mainObject;
    String JSONResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Routes");
        listViewObj = findViewById(R.id.listViewDis);
        layout = findViewById(R.id.refreshMain);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, routeIdList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                text1.setTypeface(text1.getTypeface(), Typeface.BOLD);
                text1.setText(routeList.get(position));
                text1.setTextSize(18);
                text2.setText("Route " + routeIdList.get(position));
                return view;
            }
        };
        listViewObj.setAdapter(arrayAdapter);
        new FetchSchedule().execute();
        listViewObj.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, Stops.class);
                intent.putExtra("key", routeList.get(position));
                intent.putExtra("routeId", routeIdList.get(position));
                startActivity(intent);
            }
        });
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new FetchSchedule().execute();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.twitter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(MainActivity.this, Twitter.class);
        startActivity(intent);
        return true;
    }

    public class FetchSchedule extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            JSONResult = new Utils().httpRequest("http://www.uscbus.com:8888");
            return JSONResult;
        }

        @Override
        protected void onPostExecute(String s) {
            if (s == null || s.equals("")){
                onError();
                return;
            }
            if (s != null){
                try{
                    mainObject = new JSONObject(s);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                Iterator<?> keys = mainObject.keys();
                routeList.clear();
                routeIdList.clear();
                while( keys.hasNext() ) {
                    String key = (String)keys.next();
                    try{
                        if (mainObject.get(key) instanceof JSONObject || mainObject.get(key) instanceof JSONArray) {
                            Log.d("List",key);
                            String routeName = key.split("%")[0];
                            routeList.add(routeName);
                            String routeId = key.split("%")[1];
                            routeIdList.add(routeId);
                        }
                    }
                    catch (JSONException e) {
                        onError();
                        e.printStackTrace();
                    }
                }
                arrayAdapter.notifyDataSetChanged();
                layout.setRefreshing(false);
            }
        }

        protected void onError(){
            Toast.makeText(MainActivity.this, "There is an error loading data",
                    Toast.LENGTH_LONG).show();
            layout.setRefreshing(false);
        }
    }
}
