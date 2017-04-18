package com.mattaniahbeezy.wisechildtalmud;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Mattaniah on 6/24/2015.
 */
public class AlertHelper {
    Context context;

    public static final String KEY = "alertsKey";
    public static final int NotificationID = 22;

    public AlertHelper(Context context) {
        this.context = context;
    }

    enum Personas {
        HAPPY_RABBI, JEWISH_MOTHER, EVIL_ROBOT, ANGRY_MASHGIACH;
        public Personas value = this;

        public CharSequence[] messages(Context context) {
            Resources res = context.getResources();
            switch (this) {
                case HAPPY_RABBI:
                    return res.getTextArray(R.array.HappyRabbi);
                case JEWISH_MOTHER:
                    return res.getTextArray(R.array.JewishMother);
                case EVIL_ROBOT:
                    return res.getTextArray(R.array.EvilRobot);
                case ANGRY_MASHGIACH:
                    return res.getTextArray(R.array.AngryMashgiach);
            }
            return res.getTextArray(R.array.HappyRabbi);
        }

        public String toString() {
            switch (this) {
                case HAPPY_RABBI:
                    return "Happy Rabbi";
                case JEWISH_MOTHER:
                    return "Jewish Mother";
                case EVIL_ROBOT:
                    return "Evil Robot";
                case ANGRY_MASHGIACH:
                    return "Angry Mashgiach";
                default:
                    return HAPPY_RABBI.toString();
            }
        }

    }

    public void createNotification(Alert alert) {
        ViewFactory viewFactory = new ViewFactory(context);
        Bitmap backgrouind = viewFactory.decodeSampledBitmapFromResource(R.drawable.notificationbackground, 400, 400);
        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setBackground(backgrouind);
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
        PendingIntent pi = PendingIntent.getActivity(context, 0, new Intent(context, Main.class), 0);
        CharSequence[] messages = alert.persona.messages(context);
        CharSequence nagText = messages[new Random().nextInt(messages.length)];
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle("Time to Learn");
        builder.setSound(Uri.parse(alert.sound));
        builder.setSmallIcon(R.drawable.notification_icon);
        builder.setColor(context.getResources().getColor(R.color.primary));
        builder.setContentText(nagText);
        builder.extend(wearableExtender);
        builder.setTicker(nagText);
        builder.setContentIntent(pi);
        Intent snoozeIntent = new Intent(context, AlertSnoozeService.class);
        snoozeIntent.putExtra(KEY, new Gson().toJson(alert, Alert.class));
        PendingIntent snoozePI = PendingIntent.getService(context, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozePI);

        mNotificationManager.notify(NotificationID, builder.build());
    }

    public List<Alert> getSavedAlerts() {
        List<Alert> retList = new ArrayList<>();
        Gson gson = new Gson();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String saved = sharedPreferences.getString(KEY, null);
        if (saved != null) {
            try {
                JSONArray array = new JSONArray(saved);
                for (int i = 0; i < array.length(); i++)
                    retList.add(gson.fromJson(array.getJSONObject(i).toString(), Alert.class));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            retList.add(getDefaultAlert());
            saveListToJSON(retList);
        }
        return retList;
    }

    public void saveListToJSON(List<Alert> saveList) {
        JSONArray array = new JSONArray();
        Gson gson = new Gson();
        for (Alert alert : saveList) {
            try {
                array.put(new JSONObject(gson.toJson(alert, Alert.class)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY, array.toString()).apply();
        context.startService(new Intent(context, AlertSetService.class));
    }

//    private static void setMidnightAlarm(Context context) {
//        AlarmManager armMan = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(context, AlertSetBroadcast.class), PendingIntent.FLAG_ONE_SHOT);
//        Calendar cal = Calendar.getInstance();
//        cal.set(Calendar.HOUR, 1);
//        armMan.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
//    }

    public Alert getDefaultAlert() {
        Alert defaultAlert = new Alert(10, 0, Personas.HAPPY_RABBI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
        defaultAlert.setActivated(false);
        return defaultAlert;
    }

    public static void deleteAlert(Alert alert, Context context){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alert.getPendingIntent(context));
    }

    public static class Alert {
        int hourOfDay;
        int minuteOfHour;
        Personas persona;
        String sound;
        private boolean activated;
        int id;


        public Alert(int hourOfDay, int minuteOfHour, Personas persona, String sound) {
            this.hourOfDay = hourOfDay;
            this.minuteOfHour = minuteOfHour;
            this.persona = persona;
            this.sound = sound;
            activated = true;
            id = UUID.randomUUID().hashCode();
        }


        public PendingIntent getPendingIntent(Context context) {
            Intent alertIntent = new Intent(context, AlertBroadcastReciever.class);
            alertIntent.putExtra(KEY, toJSON());
            return PendingIntent.getBroadcast(context, id, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        public Calendar getAlertTime(){
            Calendar alertTime = Calendar.getInstance();
            alertTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            alertTime.set(Calendar.MINUTE, minuteOfHour);
            return alertTime;
        }

        public static Alert fromJSON(String json) {
            return new Gson().fromJson(json, Alert.class);
        }

        public String toJSON() {
            return new Gson().toJson(this, Alert.class);
        }

        public boolean isActivated() {
            return activated;
        }

        public void setActivated(boolean activated) {
            this.activated = activated;
        }


    }

    public static class AlertBroadcastReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String alertJSON = intent.getStringExtra(KEY);
            Log.d("Alert recieved", alertJSON);
            AlertHelper helper = new AlertHelper(context);
            Alert alert = Alert.fromJSON(alertJSON);
            if (alert.isActivated())
                helper.createNotification(alert);
        }
    }

    public static class AlertSetService extends Service {


        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            AlertHelper alertHelper = new AlertHelper(this);
            List<Alert> alerts = alertHelper.getSavedAlerts();
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Gson gson = new Gson();

            for (Alert alert : alerts) {
                Calendar alertTime=alert.getAlertTime();
                if (!Calendar.getInstance().getTime().after(alertTime.getTime()))
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alertTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alert.getPendingIntent(this));
                Log.d("Alert Set ", gson.toJson(alert, Alert.class));
            }

            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public static class AlertSnoozeService extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            NotificationManagerCompat.from(this).cancel(NotificationID);

            String alertJSON = intent.getStringExtra(KEY);

            Intent alertIntent = new Intent(this, AlertBroadcastReciever.class);
            alertIntent.putExtra(KEY, alertJSON);
            Calendar inTenMinutes = Calendar.getInstance();
            inTenMinutes.add(Calendar.MINUTE, 10);
            alarmManager.set(AlarmManager.RTC_WAKEUP, inTenMinutes.getTimeInMillis(), PendingIntent.getBroadcast(this, 0, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));
            Log.d("Snooze set with", alertJSON);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
    }

    public static class AlertBootReciever extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            context.startService(new Intent(context, AlertSetService.class));
            Log.d("Boot Service Sent", "No Data");
        }
    }

}
