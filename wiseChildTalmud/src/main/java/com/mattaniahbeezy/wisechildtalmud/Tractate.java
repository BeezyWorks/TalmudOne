package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

import net.sourceforge.zmanim.hebrewcalendar.HebrewDateFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class Tractate {
    public static final int KinimIndex = 36;
    public static final int TamidIndex = 37;
    public static final int MidosIndex = 38;

    static final int[] dapim = {64, 157, 105, 121, 22, 88, 56, 40, 35, 31, 32, 29, 27, 122, 112, 91, 66, 49, 90, 82, 119, 119, 176, 113, 24, 49, 76, 14, 120, 110, 142, 61, 34, 34, 28, 22, 25, 33, 37, 73};
    static final boolean[] lastBet = {false, true, false, true, true, false, true, true, false, false, false, false, false, true, true, true, true, true, true, true,
            true, false, true, true, true, true, true, false,
            true, false, false, false, false, false, true, false, false, true, true, false};


    private int index;
    private int cellNumber;
    private Context context;

    public static int[] tractateLengths = {125, 312, 207, 240, 42, 173, 110, 78, 67, 59, 61, 55, 51, 242, 222, 180, 130, 96, 178, 162, 236, 235, 350, 224, 46, 96, 150, 25, 238, 217, 281, 119, 65, 65, 54, 41, 7, 18, 7, 143,};

    private static String[] masechtosBavliTransliterated = {"Berachos", "Shabbos", "Eruvin", "Pesachim", "Shekalim",
            "Yoma", "Sukkah", "Beitzah", "Rosh Hashana", "Taanis", "Megillah", "Moed Katan", "Chagigah", "Yevamos",
            "Kesubos", "Nedarim", "Nazir", "Sotah", "Gitin", "Kiddushin", "Bava Kamma", "Bava Metzia", "Bava Basra",
            "Sanhedrin", "Makkos", "Shevuos", "Avodah Zarah", "Horiyos", "Zevachim", "Menachos", "Chullin", "Bechoros",
            "Arachin", "Temurah", "Kerisos", "Meilah", "Kinnim", "Tamid", "Midos", "Niddah"};

    public static String[] masechtosBavli = {"ברכות", "שבת",
            "עירובין", "פסחים",
            "שקלים", "יומא", "סוכה",
            "ביצה", "ראש השנה",
            "תענית", "מגילה",
            "מועד קטן", "חגיגה",
            "יבמות", "כתובות", "נדרים",
            "נזיר", "סוטה", "גיטין",
            "קידושין", "בבא קמא",
            "בבא מציעא", "בבא בתרא",
            "סנהדרין", "מכות",
            "שבועות", "עבודה זרה",
            "הוריות", "זבחים", "מנחות",
            "חולין", "בכורות", "ערכין",
            "תמורה", "כריתות", "מעילה",
            "קינים",
            "תמיד", "מידות",
            "נדה"};


    JSONObject tractateJSON;
    public JSONArray bookmarkedCells;
    private boolean isLoadingJSON = false;
    private HostActivity hostActivity;

    public Tractate(Activity activity, int tractateIndex, int cellNumber) {
        this.context = activity;
        this.hostActivity = (HostActivity) activity;
        setIndex(tractateIndex);
        setCellNumber(cellNumber);
    }

    public Tractate(Activity activity, String name) {
        this.context = activity;
        this.hostActivity = (HostActivity) activity;
        int impliedIndex = 0;
        for (int i = 0; i < masechtosBavli.length; i++) {
            if (masechtosBavli[i].equals(name))
                impliedIndex = i;
        }
        setIndex(impliedIndex);
        setCellNumber(0);
    }

    public void nextAmud() {
        if (!isLastAmud())
            cellNumber++;
    }

    public void previousAmud() {
        if (!isFirstAmud())
            cellNumber--;
    }

    public String getAmudName(int cellNumber) {
        HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
        hebrewDateFormatter.setUseGershGershayim(false);
        String bet = isCellNumberBet(cellNumber) ? ":" : ".";
        return hebrewDateFormatter.formatHebrewNumber(cellNumberToDaf(cellNumber)) + bet;
    }

    public static String getAmudNameEnglish(int tractateIndex, int cellNumber){
        String bet = isCellNumberBet(tractateIndex, cellNumber) ? "b" : "a";
        return cellNumberTodaf(tractateIndex, cellNumber) + bet;
    }

    public static int cellNumberTodaf(int index, int cellNumber){
        /* Meila, Kinim, Tamid, and Midos are printed together in Shas Vilna, so we have to correct the daf number. Since Tamid starts on 25b, we have to add one to the cell number right off the bat.*/
        if (index == TamidIndex)
            cellNumber += 1;
        int blatt = cellNumber / 2 + 2;
        if (index == KinimIndex) {
            blatt += 20;
        } else if (index == TamidIndex) {
            blatt += 23;
        } else if (index == MidosIndex) {
            blatt += 32;
        }
        return blatt;
    }

    public int cellNumberToDaf(int cellNumber) {
       return cellNumberTodaf(index, cellNumber);
    }

    public static int getCellCount(int index) {
        return tractateLengths[index];
    }

    public int getCellCount() {
        return getCellCount(index);
    }

    public static boolean isCellNumberBet(int tractateIndex, int cellNumber){
         /*Since the first daf of Tamid is only amud bet, we have to correct it here*/
        if (tractateIndex != TamidIndex)
            return !(cellNumber % 2 == 0);
        return cellNumber % 2 == 0;
    }

    public boolean isCellNumberBet(int cellNumber) {
       return isCellNumberBet(index, cellNumber);
    }

    public int getCellNumber(int daf, boolean amudBet) {
        /* Meila, Kinim, Tamid, and Midos are printed together in Shas Vilna, so we have to correct the daf number*/
        if (index == KinimIndex) {
            daf -= 20;
        } else if (index == TamidIndex) {
            daf -= 23;
        } else if (index == MidosIndex) {
            daf -= 32;
        }
        int amud = amudBet ? 3 : 4;
        return daf * 2 - amud;
    }


    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isLoadingJSON(){
        return isLoadingJSON;
    }

    public void initializeJSON() {
        if (!isLoadingJSON) {
            isLoadingJSON = true;
            try {
                new JSONInitilizer().execute();
            } catch (OutOfMemoryError error) {
                Log.d("Out of Memory", error.getMessage());
            }
        }
    }


    private class JSONInitilizer extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] params) {
            Tractate.this.tractateJSON = loadJSONFromAsset();
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            hostActivity.tractateReady();
            isLoadingJSON = false;
        }
    }


    public void setCellNumber(int cellNumber) {
        this.cellNumber = cellNumber;
    }

    public int getCellNumber() {
        return this.cellNumber;
    }

    public boolean isFirstAmud() {
        return cellNumber == 0;
    }

    public boolean isLastAmud() {
        return cellNumber == getCellCount() - 1;
    }


    public CharSequence getText(ViewFactory.Text text) {
        return Html.fromHtml(rawHTMLtext(text));
    }

    public static CharSequence highlightSearchText(CharSequence searchTerm, String rawText){
        CharSequence highlightOpen = "<font color='red'>";
        CharSequence highlightClose = "</font>";
        StringBuilder replaceText = new StringBuilder(highlightOpen);
        replaceText.append(searchTerm);
        replaceText.append(highlightClose);
        rawText = rawText.replaceAll(searchTerm.toString(), replaceText.toString());
        return Html.fromHtml(rawText);
    }

    public CharSequence getTextWithQueryHighlight(String query, ViewFactory.Text text) {
//        CharSequence highlightOpen = "<font color='red'>";
//        CharSequence highlightClose = "</font>";
//        StringBuilder replaceText = new StringBuilder(highlightOpen);
//        replaceText.append(query);
//        replaceText.append(highlightClose);
//        String rawHtml = rawHTMLtext(text);
//        rawHtml = rawHtml.replaceAll(query.toString(), replaceText.toString());
//        return Html.fromHtml(rawHtml);
        return highlightSearchText(query, rawHTMLtext(text));
    }

    public String rawHTMLtext(ViewFactory.Text text) {
        try {
            if (tractateJSON != null) {
                JSONArray textArray = tractateJSON.getJSONArray(text.getTextKey());
                String rawString = textArray.getString(cellNumber);
                return rawString.trim();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "Error getting text";
    }


    public JSONObject loadJSONFromAsset() {
        try {
            InputStream is = context.getAssets().open(index + ".json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String rawJson = new String(buffer, "UTF-8");
            return new JSONObject(rawJson);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Tractate clone() {
        return new Tractate((Activity) context, index, cellNumber);
    }

    public String getName() {
        try {
            return masechtosBavli[index];
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(context.getString(R.string.saveedTractateIndexKey), 0).apply();
            return "error";
        }
    }

    public String toString() {
        String amud = isCellNumberBet(cellNumber) ? "ב" : "א";
        return getTitleNoAmud() + " " + amud;
    }

    public String getTitleNoAmud() {
        HebrewDateFormatter hFat = new HebrewDateFormatter();
        hFat.setUseGershGershayim(false);
        return getName() + " " + hFat.formatHebrewNumber(cellNumberToDaf(cellNumber));
    }

}
