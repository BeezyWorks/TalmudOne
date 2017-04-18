package com.mattaniahbeezy.wisechildtalmud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import net.sourceforge.zmanim.hebrewcalendar.Daf;
import net.sourceforge.zmanim.hebrewcalendar.HebrewDateFormatter;
import net.sourceforge.zmanim.hebrewcalendar.JewishCalendar;

import java.util.Calendar;
import java.util.List;

import io.fabric.sdk.android.Fabric;

import static com.mattaniahbeezy.wisechildtalmud.SearchUtil.Result;


public class Main extends AppCompatActivity implements HostActivity {
    private DrawerLayout drawer;
    private SharedPreferences sharedPref;
    Toolbar toolbar;
    ActionBarDrawerToggle drawerToggle;
    BodyFragment bodyFragment;

    ListView tractatesListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        uiStuff();
        try {
            NotificationManagerCompat.from(this).cancel(AlertHelper.NotificationID);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.content_drawer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        setDrawerToggle();
        JewishCalendar jDate = new JewishCalendar();

        int defaultCellNumber = new Tractate(Main.this, jDate.getDafYomiBavli().getMasechtaNumber(), 0).getCellNumber(jDate.getDafYomiBavli().getDaf(), false);
        int savedTractateIndex = sharedPref.getInt(getString(R.string.saveedTractateIndexKey), jDate.getDafYomiBavli().getMasechtaNumber());
        Tractate bodyTractate = new Tractate(this, savedTractateIndex, sharedPref.getInt(getString(R.string.savedCellNumberKey), defaultCellNumber));

        ViewFactory.setDrawerWidths(findViewById(R.id.tractateNavigationDrawer), this);
        tractatesListView = (ListView) findViewById(R.id.tractateList);
        View headerView = View.inflate(Main.this, R.layout.tractate_list_header, null);
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Main.this, SignInActivity.class));
            }
        });
        ImageView headerImageView = (ImageView) headerView.findViewById(R.id.headerViewImage);
        headerImageView.setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.MULTIPLY);
        tractatesListView.addHeaderView(headerView, null, false);

        TractateArrayAdapter adapter = new TractateArrayAdapter();
        tractatesListView.setAdapter(adapter);
        ViewFactory.setListViewColors(tractatesListView, this);
        int startPosition = 0;

        tractatesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HeaderViewListAdapter headerViewListAdapter = (HeaderViewListAdapter) tractatesListView.getAdapter();
                TractateArrayAdapter adapter = (TractateArrayAdapter) headerViewListAdapter.getWrappedAdapter();
                Tractate selectedTractate = (Tractate) adapter.dataSet[position - 1];

                setBodyFragment(selectedTractate);

            }
        });
        setBodyFragment(bodyTractate);
        createBottomItems();

        new NoteSyncer().execute();
    }

    private void addUserInfoToHeader() {
        if (tractatesListView.getAdapter() == null)
            return;
        View headerView = tractatesListView.getAdapter().getView(0, null, null);
        ImageView profileImageView = (ImageView) headerView.findViewById(R.id.profilePicture);
        TextView profileNameView = (TextView) headerView.findViewById(R.id.profileName);
        TextView profileEmailView = (TextView) headerView.findViewById(R.id.profileEmail);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Main.this);
        String imageURL = sharedPreferences.getString(getString(R.string.profileImageURLKey), null);
        String profileName = sharedPreferences.getString(getString(R.string.profileNameKey), null);
        String profileEmail = sharedPreferences.getString(getString(R.string.profileEmailKey), null);

        if (imageURL != null) {
            profileImageView.setVisibility(View.VISIBLE);
            new ViewFactory.URLToImageView(profileImageView, imageURL, (int) getResources().getDimension(R.dimen.profileImageDiameter)).execute();
            profileNameView.setText(profileName);
            profileEmailView.setText(profileEmail);
            profileEmailView.setVisibility(View.VISIBLE);
            profileNameView.setVisibility(View.VISIBLE);
        } else {
            profileImageView.setVisibility(View.INVISIBLE);
            profileNameView.setVisibility(View.INVISIBLE);
            profileEmailView.setText("Not signed in");
        }
    }

    private class NoteSyncer extends AsyncTask {
        GoogleDriveHelper googleDriveHelper;

        @Override
        protected Object doInBackground(Object[] params) {
            googleDriveHelper = new GoogleDriveHelper(Main.this);
            googleDriveHelper.connect();
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            googleDriveHelper.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        addUserInfoToHeader();
    }

    @Override
    public void tractateReady() {
        bodyFragment.initilizeView();
    }


    @Override
    public void updateBookmarks() {
        bodyFragment.notifyDafDrawerData();
    }

    public void setBodyFragment(Tractate tractate) {
        setBodyFragment(tractate, true);
    }

    public void setBodyFragment(Tractate tractate, boolean isPrimaryBody) {
        if (isPrimaryBody) {
            sharedPref.edit().putInt(getString(R.string.savedCellNumberKey), tractate.getCellNumber()).apply();
            sharedPref.edit().putInt(getString(R.string.saveedTractateIndexKey), tractate.getIndex()).apply();
        }
        TractateArrayAdapter adapter = (TractateArrayAdapter) ((HeaderViewListAdapter) tractatesListView.getAdapter()).getWrappedAdapter();
        for (int i = 0; i < adapter.dataSet.length; i++) {
            if (adapter.dataSet[i] instanceof Tractate)
                if (((Tractate) adapter.dataSet[i]).getIndex() == tractate.getIndex()) {
                    tractatesListView.setSelection(i + 1);
                    tractatesListView.setItemChecked(i + 1, true);
                }
        }
        bodyFragment = BodyFragment.getInstance(this, tractate, isPrimaryBody);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.contentArea, bodyFragment);
        if (!isPrimaryBody)
            transaction.addToBackStack("");
        transaction.commit();
        drawer.closeDrawer(Gravity.LEFT);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(Gravity.LEFT)) {
            drawer.closeDrawer(Gravity.LEFT);
            return;
        }
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return;
        }

        super.onBackPressed();
    }

    private void createBottomItems() {
        final LinearLayout bottomItems = (LinearLayout) findViewById(R.id.bottomItemContainer);
        bottomItems.removeAllViews();
        final ViewFactory viewFactory = new ViewFactory(this);
        bottomItems.setBackgroundColor(viewFactory.isStyleDark() ? getResources().getColor(R.color.background_material_dark) : Color.WHITE);

        View goToRow = viewFactory.getRowWithIcon(R.drawable.ic_hand_pointing_right, "Go To", false);
        goToRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTrcatatePickerView();
            }
        });

        View moreRow = viewFactory.getRowWithIcon(R.drawable.ic_dots_horizontal, "More", false);
        moreRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions();
            }
        });

        bottomItems.addView(goToRow);
        bottomItems.addView(moreRow);
    }

    private void showMoreOptions() {
        final AlertDialog builder = new AlertDialog.Builder(Main.this)
                .setNegativeButton("Dismiss", null).create();

        final ViewFactory viewFactory = new ViewFactory(this);
        viewFactory.setOverrideStyle(ViewFactory.Style.DAY);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) getResources().getDimension(R.dimen.padding_left_and_right);
        ll.setPadding(0, padding / 2, 0, 0);

        /*create view*/
        View tefilasBeisMedarshRow = viewFactory.getRowWithIcon(R.drawable.alef, "תפילת בית מדרש", false);
        View fullSearchRow = viewFactory.getRowWithIcon(R.drawable.ic_magnify, "Full Search", true);
        View allBookmarksRow = viewFactory.getRowWithIcon(R.drawable.ic_bookmark, "All Bookmarks", true);
        View nagSettingsRow = viewFactory.getRowWithIcon(R.drawable.ic_alarm, "עתים קבועים", true);

        /* give views on click actions*/
        tefilasBeisMedarshRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tefilasBeisMedrash();
                builder.dismiss();
            }
        });

        allBookmarksRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction().replace(R.id.contentArea, new BookmarksFragment()).addToBackStack(null).commit();
                builder.dismiss();
                drawer.closeDrawer(Gravity.LEFT);
            }
        });

        fullSearchRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.dismiss();
                viewFactory.searchDialog().show();
            }
        });

        nagSettingsRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction().replace(R.id.contentArea, new AlertFragment()).addToBackStack(null).commit();
                builder.dismiss();
                drawer.closeDrawer(Gravity.LEFT);
            }
        });

        /*add views to the Linear Layout*/
        ll.addView(tefilasBeisMedarshRow);
        ll.addView(fullSearchRow);
        ll.addView(allBookmarksRow);
        ll.addView(nagSettingsRow);

        builder.setView(ll);
        builder.show();

    }

    @Override
    public void onSearchCompleted(List<Result> results, SearchUtil.Query query) {
        getFragmentManager().beginTransaction().replace(R.id.contentArea, SearchResultsFragment.getInstance(results, query)).addToBackStack(null).commit();
        drawer.closeDrawer(Gravity.LEFT);
    }

    private void tefilasBeisMedrash() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
        builder.setMessage(getResources().getText(R.string.tefilas_beis_medrash));
        builder.setNegativeButton("Dismiss", null);
        builder.create().show();
    }


    @Override
    public void updateColors() {
        ViewFactory.setListViewColors(tractatesListView, Main.this);
        HeaderViewListAdapter headerViewListAdapter = (HeaderViewListAdapter) tractatesListView.getAdapter();
        ArrayAdapter adapter = (ArrayAdapter) headerViewListAdapter.getWrappedAdapter();
        adapter.notifyDataSetChanged();
        createBottomItems();

    }

    @Override
    public void showNoteDialog(final NotesUtil notesUtil, final int cell) {
        final EditText editText = new EditText(Main.this);
        editText.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        ScrollView scrollView = new ScrollView(Main.this);
        scrollView.setMinimumHeight((int) getResources().getDimension(R.dimen.noteDialogHeight));
        int padding = (int) getResources().getDimension(R.dimen.padding_left_and_right);
        editText.setPadding(padding, padding, padding, padding);
        scrollView.addView(editText);
        NotesUtil.Note note = notesUtil.getNote(cell);
        if (note != null)
            editText.setText(notesUtil.getNote(cell).getNoteText());
        AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);

        final String cellKey = Tractate.getAmudNameEnglish(bodyFragment.tractate.getIndex(), cell);
        builder.setView(scrollView)
                .setPositiveButton("Save", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        notesUtil.saveNote(new NotesUtil.Note(Calendar.getInstance().getTimeInMillis(), editText.getText().toString(), cellKey));
                        bodyFragment.notifyDafDrawerData();
                    }
                });
        if (note != null)
            builder.setNegativeButton("Delete Note", new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final List<String> deleteKeys = notesUtil.getKeysToDelete();
                    deleteKeys.add(cellKey);
                    notesUtil.setKeysToDelete(deleteKeys);
                    final NotesUtil.Note note = notesUtil.getNote(cell);
                    notesUtil.deleteNote(cell);
                    Snackbar.make(Main.this.findViewById(android.R.id.content), "Note Deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    notesUtil.saveNote(note);
                                    bodyFragment.notifyDafDrawerData();
                                    deleteKeys.remove(cellKey);
                                    notesUtil.setKeysToDelete(deleteKeys);
                                }
                            })
                            .show();
                    bodyFragment.notifyDafDrawerData();
                }
            });
        builder.create()
                .show();

    }

    private class TractateArrayAdapter extends ArrayAdapter {
        final public Object[] dataSet = {
                "זרעים",
                new Tractate(Main.this, 0, 0),
                "מועד",
                new Tractate(Main.this, 1, 0),
                new Tractate(Main.this, 2, 0),
                new Tractate(Main.this, 3, 0),
                new Tractate(Main.this, 4, 0),
                new Tractate(Main.this, 5, 0),
                new Tractate(Main.this, 6, 0),
                new Tractate(Main.this, 7, 0),
                new Tractate(Main.this, 8, 0),
                new Tractate(Main.this, 9, 0),
                new Tractate(Main.this, 10, 0),
                new Tractate(Main.this, 11, 0),
                new Tractate(Main.this, 12, 0),
                "נשים",
                new Tractate(Main.this, 13, 0),
                new Tractate(Main.this, 14, 0),
                new Tractate(Main.this, 15, 0),
                new Tractate(Main.this, 16, 0),
                new Tractate(Main.this, 17, 0),
                new Tractate(Main.this, 18, 0),
                new Tractate(Main.this, 19, 0),
                "נזיקין",
                new Tractate(Main.this, 20, 0),
                new Tractate(Main.this, 21, 0),
                new Tractate(Main.this, 22, 0),
                new Tractate(Main.this, 23, 0),
                new Tractate(Main.this, 24, 0),
                new Tractate(Main.this, 25, 0),
                new Tractate(Main.this, 26, 0),
                new Tractate(Main.this, 27, 0),
                "קדשים",
                new Tractate(Main.this, 28, 0),
                new Tractate(Main.this, 29, 0),
                new Tractate(Main.this, 30, 0),
                new Tractate(Main.this, 31, 0),
                new Tractate(Main.this, 32, 0),
                new Tractate(Main.this, 33, 0),
                new Tractate(Main.this, 34, 0),
                new Tractate(Main.this, 35, 0),
                new Tractate(Main.this, 36, 0),
                new Tractate(Main.this, 37, 0),
                new Tractate(Main.this, 38, 0),
                "טהרות",
                new Tractate(Main.this, 39, 0)

        };


        public TractateArrayAdapter() {
            super(Main.this, android.R.layout.simple_dropdown_item_1line);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return (dataSet[position] instanceof String) ?
                    getSederHeaderView((String) dataSet[position]) :
                    getViewForTractate((Tractate) dataSet[position]);
        }

        private View getSederHeaderView(String title) {
            View view = View.inflate(Main.this, R.layout.seder_header, null);
            ImageView divider = (ImageView) view.findViewById(R.id.divider);
            if (title.equals(dataSet[0]))
                divider.setVisibility(View.GONE);
            divider.setColorFilter(new ViewFactory(Main.this).isStyleDark() ? R.color.dividerColorDark : R.color.dividerColorLight);
            TextView titleView = (TextView) view.findViewById(R.id.sederTitle);
            titleView.setTextColor(new ViewFactory(Main.this).getPrimaryHighlightColor());
            titleView.setText(title);
            return view;
        }

        private View getViewForTractate(Tractate tractate) {
            TextView textView = (TextView) View.inflate(Main.this, R.layout.simple_listview_item, null);
            textView.setText(tractate.getName());
            textView.setTextColor(new ViewFactory(Main.this).getListViewTextColor());
            textView.setBackground(new ViewFactory(Main.this).getListViewIemBackground());
            return textView;
        }

        @Override
        public int getCount() {
            return dataSet.length;
        }

    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    private void setDrawerToggle() {
        drawerToggle = new android.support.v7.app.ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        drawer.setDrawerListener(drawerToggle);
    }

    public DrawerLayout getDrawer() {
        return drawer;
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    public void setNewCellNumber(int cellNumber) {
        if (bodyFragment != null && bodyFragment.isAdded())
            bodyFragment.setNewCellNumber(cellNumber);
    }

    @Override
    public TabLayout getTablayout() {
        return (TabLayout) findViewById(R.id.tabLayout);
    }

    private void toggleFullscreen() {
        String key = getString(R.string.fullscreenKey);
        sharedPref.edit().putBoolean(key, !sharedPref.getBoolean(key, false)).apply();
        restartActivity();
    }

    protected void restartActivity() {
        Intent intent = new Intent(Main.this, Main.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
    }


    private void getTrcatatePickerView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout ll = new LinearLayout(Main.this);
        ll.setGravity(Gravity.CENTER);
        int padding = (int) getResources().getDimension(R.dimen.small_padding_top);
        ll.setPadding(padding * 2, padding, padding * 2, padding);

        Tractate selectTracate = new Tractate(Main.this, 0, 0);
        final HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
        hebrewDateFormatter.setUseGershGershayim(false);


        final NumberPicker amudPicker = new NumberPicker(Main.this);
        final String[] amudValues = new String[360];
        for (int i = 0; i < amudValues.length; i++) {
            amudValues[i] = selectTracate.getAmudName(i);
        }

        amudPicker.setDisplayedValues(amudValues);
        amudPicker.setMaxValue(selectTracate.getCellCount());

        final NumberPicker tractatePicker = new NumberPicker(Main.this);
        tractatePicker.setDisplayedValues(Tractate.masechtosBavli);
        tractatePicker.setMinValue(0);
        tractatePicker.setMaxValue(Tractate.masechtosBavli.length - 1);

        tractatePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                amudPicker.setDisplayedValues(amudValues);
                amudPicker.setValue(0);
                amudPicker.setMaxValue(Tractate.getCellCount(newVal) - 1);
                /*These three mosechtot are all kinds of messed up, so we have to do some monkeying around. @See com.mattaniahbeezy.wisechildtalmud.Tractate#cellNumberToDaf() for more information*/
                if (newVal == Tractate.KinimIndex || newVal == Tractate.TamidIndex || newVal == Tractate.MidosIndex) {
                    Tractate tractate = new Tractate(Main.this, newVal, 0);
                    String[] newValArray = new String[tractate.getCellCount() + 1];
                    for (int i = 0; i < newValArray.length; i++)
                        newValArray[i] = tractate.getAmudName(i);
                    amudPicker.setDisplayedValues(newValArray);
                }
            }
        });

        ll.addView(amudPicker);
        ll.addView(tractatePicker);

        builder.setView(ll);
        builder.setPositiveButton("Set", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setBodyFragment(new Tractate(Main.this, tractatePicker.getValue(), amudPicker.getValue()));
                drawer.closeDrawer(Gravity.LEFT);
            }
        });
        builder.setNeutralButton("דף היומי", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Daf daf = new JewishCalendar().getDafYomiBavli();
                Tractate dafYomiTracate = new Tractate(Main.this, daf.getMasechtaNumber(), 0);
                int cellNumber= dafYomiTracate.getCellNumber(daf.getDaf(), false);
                dafYomiTracate.setCellNumber(cellNumber);
                setBodyFragment(dafYomiTracate);
//                amudPicker.setValue(Tractate.getCellNumber(daf.getDaf(),false));
            }
        });
        builder.create().show();
    }


    @SuppressLint("InlinedApi")
    private void uiStuff() {

        if (sharedPref.getBoolean(getString(R.string.portraitLockKey), false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        if (sharedPref.getBoolean(getString(R.string.screenOnKey), false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        View decorView = getWindow().getDecorView();

        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (sharedPref.getBoolean(getString(R.string.fullscreenKey), false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }

        if (!sharedPref.getBoolean(getString(R.string.fullscreenKey), false)) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            uiStuff();
        }
    }


}