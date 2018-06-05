package com.uscbus.uscbus;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Stops extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stops);
        setTitle("Stops");
        Bundle bundle = getIntent().getExtras();
        String JSONResult = bundle.getString("json");
        String routeName = bundle.getString("key");
        JSONObject mainObject;
        List<String> stopList = new ArrayList<>();
        ArrayAdapter<String> arrayAdapter;
        arrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, stopList);
        ((ListView)findViewById(R.id.listViewStop)).setAdapter(arrayAdapter);
        try {
            mainObject = new JSONObject(JSONResult);
            JSONArray stopArr = (JSONArray)mainObject.get(routeName);
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
}
