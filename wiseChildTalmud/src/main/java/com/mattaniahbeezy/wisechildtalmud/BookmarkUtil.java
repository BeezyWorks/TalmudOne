package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Mattaniah on 6/23/2015.
 */
public class BookmarkUtil {
    public static final String bookmarksKey = "bookmarkskey";
    int tractateIndex;
    private JSONObject bookmarksJSON;
    Activity context;

    public BookmarkUtil(Activity context, int tractateIndex) {
        this.context = context;
        setTractateIndex(tractateIndex);
    }

    public void setTractateIndex(int tractateIndex) {
        this.tractateIndex = tractateIndex;
        loadAllBookmarks();
    }

    public boolean hasBookmarks() {
        return bookmarksJSON.keys().hasNext();
    }

    public boolean isCellBookmarked(int cellNumber) {
        try {
            bookmarksJSON.get(getKeyforCell(cellNumber));
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    public void toggleBookmark(int cellNumber) {
        if (isCellBookmarked(cellNumber))
            bookmarksJSON.remove(getKeyforCell(cellNumber));
        else
            try {
                bookmarksJSON.put(getKeyforCell(cellNumber), true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        persistBookmarks();
    }

    public void persistBookmarks() {
        ParseUser.getCurrentUser().put(getBookmarksKey(), bookmarksJSON.toString());
        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e==null)
                    Log.d("Bookmark Save", "Success");
                else
                    Log.d("Bookmark Save", e.toString());
            }
        });
    }

    private void loadAllBookmarks() {
        bookmarksJSON = new JSONObject();
        try {
            String raw = ParseUser.getCurrentUser().getString(getBookmarksKey());
            if (raw != null)
                bookmarksJSON = new JSONObject(raw);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getBookmarksKey() {
        return bookmarksKey + tractateIndex;
    }

    private String getKeyforCell(int cell) {
        return Integer.toString(cell);
    }

    public String getJSONAsString() {
        return bookmarksJSON.toString();
    }

    public void restoreBookmarks(String oldJson) {
        try {
            this.bookmarksJSON = new JSONObject(oldJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        persistBookmarks();
    }

    public void deleteAllBookmarks() {
        ParseUser.getCurrentUser().put(getBookmarksKey(), null);
        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e==null)
                    Log.d("Delete Bookmarks", "Success");
                else
                    Log.d("Delete Bookmarks", "Failed");
            }
        });
    }

    public List<Tractate> getAllBookmarks() {
        List<Tractate> allBookmarks = new ArrayList<>();
        Iterator<String> iterator = bookmarksJSON.keys();
        while (iterator.hasNext()) {
            allBookmarks.add(new Tractate(context, tractateIndex, Integer.valueOf(iterator.next())));
        }
        return allBookmarks;
    }

    public static void deleteBookmark(Activity context, Tractate tractate) {
        BookmarkUtil util = new BookmarkUtil(context, tractate.getIndex());
        if (util.isCellBookmarked(tractate.getCellNumber()))
            util.toggleBookmark(tractate.getCellNumber());
    }

    public static void addBookmark(Activity activity, Tractate tractate){
        BookmarkUtil util = new BookmarkUtil(activity, tractate.getIndex());
        if (!util.isCellBookmarked(tractate.getCellNumber()))
            util.toggleBookmark(tractate.getCellNumber());
    }

    public static List<BookmarkUtil> getAllBookmarks(Activity context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        List<BookmarkUtil> allBookmarks = new ArrayList<>();

        for (int i = 0; i < Tractate.masechtosBavli.length; i++) {
            BookmarkUtil helper = new BookmarkUtil(context, i);
            if (helper.hasBookmarks())
                allBookmarks.add(helper);
        }
        return allBookmarks;
    }

}