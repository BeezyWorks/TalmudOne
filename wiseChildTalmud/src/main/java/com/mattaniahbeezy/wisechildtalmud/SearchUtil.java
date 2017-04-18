package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.drive.query.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Mattaniah on 6/23/2015.
 */
public class SearchUtil {
    Activity context;
    HostActivity hostActivity;
    ProgressDialog progressDialog;

    public SearchUtil(Context context) {
        this.context = (Activity) context;
        this.hostActivity = (HostActivity) context;
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Loading results...");
        progressDialog.setIndeterminate(true);
    }

    public void execute(Query query) {
        progressDialog.show();
        new SearchExecutor(query).execute();
    }

    private class SearchExecutor extends AsyncTask {
        Query query;
        List<Result> results = new ArrayList<>();

        public SearchExecutor(Query query) {
            this.query = query;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            for (int i = 0; i < query.scope.length; i++) {
                Tractate tractate = new Tractate(context, query.scope[i], 0);
                JSONObject tractateJson = tractate.loadJSONFromAsset();
                try {
                    JSONArray searchArray = tractateJson.getJSONArray(query.text.getTextKey());
                    if (searchArray.toString().contains(query.searchTerm)) {
                        for (int j = 0; j < searchArray.length(); j++) {
                            CharSequence amudString = searchArray.getString(j);
                            if (amudString.toString().contains(query.searchTerm))
                                results.add(new Result(new Tractate(context, i, j), Tractate.highlightSearchText(query.searchTerm, convertTermToBlurb(amudString).toString())));
                        }
                    }
                } catch (JSONException e) {
                }
            }
                return null;

            }


        private CharSequence convertTermToBlurb(CharSequence resultText) {
            int resultIndex = resultText.toString().indexOf(query.searchTerm);
            if (resultIndex > -1) {
                int buffer = 80;

                int startInt = resultIndex - buffer;
                int endInt = resultIndex + buffer;

                if (startInt < 0)
                    startInt = 0;
                if (!(endInt < resultText.length()))
                    endInt = resultText.length() - 1;

                while (endInt < resultText.length() && !Character.isWhitespace(resultText.charAt(endInt)))
                    endInt++;
                while (startInt > 0 && !Character.isWhitespace(resultText.charAt(startInt)))
                    startInt--;


                return resultText.subSequence(startInt, endInt);
            }
            return "error";
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            progressDialog.dismiss();
            hostActivity.onSearchCompleted(results, query);
        }
    }


    public static class Query {
        String searchTerm;
        ViewFactory.Text text;
        int[] scope;

        public Query(String searchTerm, ViewFactory.Text text, int[] scope) {
            this.searchTerm = searchTerm;
            this.text = text;
            this.scope = scope;
        }
    }

    public static class Result {
        Tractate tractate;
        CharSequence resultString;

        public Result(Tractate tractate, CharSequence resultString) {
            this.tractate = tractate;
            this.resultString = resultString;
        }
    }
}
