package com.uscbus.uscbus;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class NotificationService extends Service {
    final int REFRESH_TIME = 5000;
    Timer refreshTimer;
    String routeName;
    String routeId;
    String stopId;
    String busId;
    String JSONResult;
    String currDue;
    Boolean sentArrival;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.START_FOREGROUND_ACTION)) {
            sentArrival = false;
            refreshTimer = new Timer();
            refreshTimer.scheduleAtFixedRate(new NotificationService.refreshTask(), 0, REFRESH_TIME);
            Bundle bundle = intent.getExtras();
            routeId = bundle.getString("routeId");
            busId = bundle.getString("busId");
            stopId = bundle.getString("stopId");
            routeName = bundle.getString("routeName");
            Log.d("service", busId + " " + stopId + " " + routeId);
        }
        else if (intent.getAction().equals(Constants.ACTION.STOP_FOREGROUND_ACTION)){
            stopForeground(true);
            stopSelf();
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.d("service", "onDestroy");
        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null)
            manager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
        refreshTimer.cancel();
        refreshTimer.purge();
        refreshTimer = null;
    }

    private void updateForegroundNotification(String s) {
        Intent intent = new Intent(this, Stops.class);
        intent.putExtra("key", routeName);
        intent.putExtra("routeId", routeId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_ID.PERSISTENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.buses)
                .setContentTitle("Real-time Tracking")
                .setContentText(s)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(s))
                .setOngoing(true)
                .setContentIntent(pendingIntent);
        Intent cancelIntent = new Intent(NotificationService.this, NotificationService.class);
        cancelIntent.setAction(Constants.ACTION.STOP_FOREGROUND_ACTION);
        PendingIntent cancelPendingIntent = PendingIntent.getService(this, 0, cancelIntent,0);
        mBuilder.addAction(0, "Stop Tracking", cancelPendingIntent);
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, mBuilder.build());
    }

    private void sendArrivalNotification(String s){
        Intent intent = new Intent(this, Stops.class);
        intent.putExtra("key", routeName);
        intent.putExtra("routeId", routeId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_ID.ARRIVAL_CHANNEL_ID)
                .setSmallIcon(R.drawable.buses)
                .setContentTitle("Bus Arriving")
                .setContentText(s)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(s))
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(Constants.NOTIFICATION_ID.ARRIVAL_NOTIFICATION_ID, mBuilder.build());
        }
    }

    public class refreshTask extends TimerTask{
        @Override
        public void run() {
            new FetchSchedule().execute();
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
            String due = processJSON();
            if (due == null) {
                Log.d("service", "due is null");
                sendArrivalNotification("Bus " + busId + " has arrived");
                stopSelf();
                return;
            }
            Log.d("service", "due " + due);
            String arrivalString = due;
            if (!due.equals(currDue)) {
                currDue = due;
                if (arrivalString.equals("0"))
                    arrivalString = " is arriving";
                else if (arrivalString.equals("1"))
                    arrivalString = " will arrive in 1 min";
                else
                    arrivalString = String.format(" will arrive in %s mins", arrivalString);
                String notificationString = "Bus " + busId + arrivalString;
                updateForegroundNotification(notificationString);
            }
            if (sentArrival.equals(false)){
                try{
                    int arrivalTime = Integer.parseInt(due);
                    if (arrivalTime < 3){
                        sendArrivalNotification("Bus "+ busId + " is arriving");
                        sentArrival = true;
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private String processJSON() {
        JSONArray arr;
        if (Constants.DEBUG_JSON)
            JSONResult = Constants.TEST_JSON;
        try {
            arr = new JSONArray(JSONResult);
            JSONObject routeObj = null;
            for (int i = 0; i < arr.length(); i++) {
                try {
                    JSONObject currObj = arr.getJSONObject(i);
                    if (currObj.getString("routeId").equals(routeId)) {
                        routeObj = currObj;
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (routeObj == null) {
                Log.d("service", "no route id match");
                return null;
            }
            JSONArray stopArr = routeObj.getJSONArray("routeTime");
            for (int i = 0; i < stopArr.length(); i++) {
                JSONObject eachStop = stopArr.getJSONObject(i);
                String stopId = eachStop.getString("stopId");
                if (eachStop.getString("stopId").equals(this.stopId)) {
                    JSONArray arrivalArr = eachStop.getJSONArray("time");
                    for (int j = 0; j < arrivalArr.length(); j++) {
                        JSONObject eachArrival = arrivalArr.getJSONObject(j);
                        if (eachArrival.getString("busNum").equals(busId)) {
                            return eachArrival.getString("due");
                        }
                    }
                    Log.d("service", "no bus id match");
                }
            }
            Log.d("service", "no stop id match");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
