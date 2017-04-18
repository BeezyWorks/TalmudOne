package com.mattaniahbeezy.wisechildtalmud;

import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;

import java.util.List;

/**
 * Created by Mattaniah on 6/10/2015.
 */
public interface HostActivity {
    public DrawerLayout getDrawer();

    public Toolbar getToolbar();

    public void setNewCellNumber(int cellNumber);

    public TabLayout getTablayout();

    public void tractateReady();

    public void updateColors();

    public void setBodyFragment(Tractate tractate);

    public void setBodyFragment(Tractate tractate, boolean isMainBody);

    public void updateBookmarks();

    public void onSearchCompleted(List<SearchUtil.Result> results, SearchUtil.Query query);

    public void showNoteDialog(NotesUtil notesUtil, int cell);
}
