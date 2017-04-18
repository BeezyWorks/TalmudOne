package com.mattaniahbeezy.wisechildtalmud;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.parse.ParseUser;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Mattaniah on 8/4/2015.
 */
public class TimeLearnedUtil {
    Context context;
    public static String TotalTimeKeyOld = "key:timeinapp";
    public static String TotalTimeKey = "key_timeinapp";
    public static String DaysKey = "key:days";

    public TimeLearnedUtil(Context context) {
        this.context = context;
    }

    public void addTime(long timeOpened) {
        ParseUser user = ParseUser.getCurrentUser();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.contains(TotalTimeKeyOld)) {
            user.put(TotalTimeKey, sharedPreferences.getLong(TotalTimeKeyOld, 0));
            sharedPreferences.edit().remove(TotalTimeKeyOld).apply();
        }

        long savedTime = user.getLong(TotalTimeKey);
        savedTime += (Calendar.getInstance().getTimeInMillis() - timeOpened);
        user.put(TotalTimeKey, savedTime);
        user.saveInBackground(null);
//        try {
//            addDayToList(timeOpened);
//        } catch (JSONException e) {
//        }
    }

    public long getMilisLearned(){
        return ParseUser.getCurrentUser().getLong(TotalTimeKey);
    }

    public void addDayToList(long day) throws JSONException {
//        JSONArray jsonArray = getSavedDaysJSON();
//        jsonArray.put(day);
//        sharedPreferences.edit().putString(DaysKey, jsonArray.toString()).apply();
    }

    public List<Calendar> getDaysLearned() throws JSONException {
        List<Calendar> calendars = new ArrayList<>();
//        String savedArray = sharedPreferences.getString(DaysKey, null);
//        JSONArray savedDaysJSON = savedArray != null ? new JSONArray(savedArray) : new JSONArray();
//        for (int i = 0; i < savedDaysJSON.length(); i++) {
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTimeInMillis(savedDaysJSON.getLong(i));
//            calendars.add(calendar);
//        }
        return calendars;
    }

//    private JSONArray getSavedDaysJSON() throws JSONException {
//        String savedArray = sharedPreferences.getString(DaysKey, null);
//        return savedArray != null ? new JSONArray(savedArray) : new JSONArray();
//    }

    public String getTotalTimeAsString(){
        long timeInMilliSeconds = getMilisLearned();

        long seconds = timeInMilliSeconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        String dayString = days > 0 ? String.valueOf(days) : null;
        String hourString = hours > 0 ? String.valueOf(hours % 24) : null;
        String minutesString = minutes > 0 ? String.valueOf(minutes % 60) : null;
        String secondsString = String.valueOf(seconds % 60);

        StringBuilder builder = new StringBuilder();
        if (dayString != null)
            builder.append(dayString).append(" days, ");
        if (hourString != null)
            builder.append(hourString).append(" hours, ");
        if (minutesString != null)
            builder.append(minutesString).append(" minutes, ");
        builder.append(secondsString).append(" seconds");
        return builder.toString();
    }
}
