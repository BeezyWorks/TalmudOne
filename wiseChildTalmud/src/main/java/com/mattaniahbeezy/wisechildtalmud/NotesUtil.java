package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Mattaniah on 6/29/2015.
 */
public class NotesUtil {
    Activity context;
    int tractateIndex;

    public static final String KEY = "notes:key";
    public static final String DELETE_KEYS = "notes:deltelist";

    JSONObject notesJSON = new JSONObject();
    private Gson gson = new Gson();

    public void setTractateIndex(int tractateIndex) {
        this.tractateIndex = tractateIndex;
        String rawJSON = PreferenceManager.getDefaultSharedPreferences(context).getString(getKey(), null);
        if (rawJSON != null)
            try {
                notesJSON = new JSONObject(rawJSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }

    public void addKeyToDelete(String key) {

    }

    public List<String> getKeysToDelete() {
        List<String> list = new ArrayList<>();
        String rawList = PreferenceManager.getDefaultSharedPreferences(context).getString(DELETE_KEYS, null);
        if (rawList == null)
            return list;

        try {
            JSONArray jsonList = new JSONArray(rawList);
            for (int i = 0; i < jsonList.length(); i++)
                list.add(jsonList.getString(i));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    }

    public void setKeysToDelete(List<String> keysToDelete) {
        JSONArray jsonArray = new JSONArray();
        for (String key : keysToDelete)
            jsonArray.put(key);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(DELETE_KEYS, jsonArray.toString()).apply();
    }

    public NotesUtil(Activity context, int tractateIndex) {
        this.context = context;
        setTractateIndex(tractateIndex);
    }

    public boolean isCellHaveNote(int cell) {
        if (notesJSON == null)
            return false;
        try {
            notesJSON.get(getCellKey(cell));
        } catch (JSONException e) {
            return false;
        }
        return true;
    }


    public void saveNote(Note note) {
        if (note.getNoteText().length() < 1) {
            deleteNote(note.getKey());
            return;
        }
        try {
            notesJSON.put(note.getKey(), gson.toJson(note));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        saveAllNotes();
    }

    public Note getNote(String key) {
        try {
            return gson.fromJson(notesJSON.getString(key), Note.class);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Note getNote(int cell) {
        return getNote(getCellKey(cell));
    }

    public void deleteNote(String key) {
        notesJSON.remove(key);
        Log.d("Delete Note", "Note does not exist");

        saveAllNotes();
    }

    public void deleteNoteAfterSync(String key) {
        notesJSON.remove(key);
    }

    public void deleteNote(int cell) {
        deleteNote(getCellKey(cell));
    }


    public void updateNote(String key, Note note) {
        String rawJSON = gson.toJson(note);
        try {
            notesJSON.put(key, rawJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void saveAllNotes() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(getKey(), notesJSON.toString()).apply();
    }

    private String getCellKey(int cell) {
        return Tractate.getAmudNameEnglish(tractateIndex, cell);
    }

    public boolean hasNotes() {
        return notesJSON.keys().hasNext();
    }

    public String getKey() {
        return KEY + tractateIndex;
    }

    public static List<NotesUtil> getAllNotes(Activity context) {
        List<NotesUtil> allNotes = new ArrayList<>();
        for (int i = 0; i < Tractate.masechtosBavli.length; i++) {
            NotesUtil util = new NotesUtil(context, i);
            if (util.hasNotes())
                allNotes.add(util);
        }
        return allNotes;
    }

    public Iterator<String> getIterator() {
        return notesJSON.keys();
    }

    public static class Note {
        long lastUpdated;
        String noteText;
        String key;

        public Note(long lastUpdated, String noteText, String key) {
            this.lastUpdated = lastUpdated;
            this.noteText = noteText;
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void setLastUpdated(long lastUpdated) {
            this.lastUpdated = lastUpdated;
        }


        public String getNoteText() {
            return noteText;
        }

        public void setNoteText(String noteText) {
            this.noteText = noteText;
        }
    }
}
