package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextSwitcher;
import android.widget.ViewSwitcher;

import java.util.Calendar;

/**
 * Created by Mattaniah on 6/10/2015.
 */
public class BodyFragment extends Fragment implements MyGestureDetector.GestureHost {
    Tractate tractate;
    ViewFactory.Text currentText;

    ViewFactory viewFactory;
    HostActivity hostActivity;
    Toolbar toolbar;
    View rightDrawerToggle;

    int numberOfTexts = ViewFactory.Text.values().length;

    private DrawerLayout drawer;
    private View mainView;
    private FrameLayout frameLayout;
    private ListView dafListView;

    private SearchView searchView;

    TextSwitcher[] textSwitchers = new TextSwitcher[numberOfTexts];
    ScrollView[] scrollViews = new ScrollView[numberOfTexts];

    long timeStarted;

    private boolean isPrimaryBody;

    public static BodyFragment getInstance(Activity activity, Tractate tractate, boolean isPrimaryBody) {
        BodyFragment f = new BodyFragment();
        f.hostActivity = (HostActivity) activity;
        f.viewFactory = new ViewFactory(activity);
        f.toolbar = f.hostActivity.getToolbar();
        f.setTractate(tractate);
        f.isPrimaryBody = isPrimaryBody;
        return f;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.bodyfragment_layout, null);
        this.drawer = (DrawerLayout) mainView.findViewById(R.id.body_fragment_drawer_layout);
        ViewFactory.setDrawerWidths(mainView.findViewById(R.id.dafDrawerLayout), getActivity());
        if (toolbar == null) {
            startActivity(new Intent(getActivity(), Main.class));
            getActivity().finish();
            return new TextSwitcher(getActivity());
        }

        rightDrawerToggle = toolbar.findViewById(R.id.rightDrawerToggle);
        rightDrawerToggle.setVisibility(View.VISIBLE);
        rightDrawerToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawer.isDrawerOpen(Gravity.RIGHT)) {
                    drawer.closeDrawer(Gravity.RIGHT);
                } else {
                    drawer.openDrawer(Gravity.RIGHT);
                }
            }
        });

        drawer.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                rightDrawerToggle.setRotation(slideOffset * 180 + 180);
            }
        });
        frameLayout = (FrameLayout) mainView.findViewById(R.id.body_content_frame);
        dafListView = (ListView) mainView.findViewById(R.id.dafListView);

        searchView = (SearchView) toolbar.findViewById(R.id.toolbarSearchView);
        searchView.setVisibility(View.VISIBLE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                for (ViewFactory.Text text : ViewFactory.Text.values())
                    textSwitchers[text.ordinal()].setText(tractate.getTextWithQueryHighlight(query, text));
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        frameLayout.setBackgroundColor(viewFactory.getBackgroundColor());
        if (!tractate.isLoadingJSON())
            initilizeView();
        return mainView;
    }



    public void initilizeView() {
        if (!isAdded())
            return;
        ViewFactory.setListViewColors(dafListView, getActivity());
        for (int i = 0; i < numberOfTexts; i++) {
            final int j = i;
            textSwitchers[i] = new TextSwitcher(getActivity());
            textSwitchers[i].setFactory(new ViewSwitcher.ViewFactory() {
                @Override
                public View makeView() {
                    return viewFactory.getTextView(ViewFactory.Text.values()[j]);
                }
            });
            textSwitchers[i].setMeasureAllChildren(false);
            scrollViews[i] = viewFactory.getScrollView();
            scrollViews[i].addView(textSwitchers[i]);
            textSwitchers[i].setText(tractate.getText(ViewFactory.Text.values()[i]));
            scrollViews[i].setOnTouchListener(MyGestureDetector.getOnTouchListener(getActivity(), this));

            scrollViews[i].post(new Runnable() {
                @Override
                public void run() {
                    scrollViews[j].scrollTo(0, PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(getString(R.string.scrollKey) + ViewFactory.Text.values()[j].getLable(), 0));
                }
            });
        }
        setTitles();
        initTabBar();
        dafListView.setAdapter(new DafListViewAdapter(this, tractate));
        dafListView.setItemChecked(PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(getString(R.string.savedCellNumberKey), 0), true);
        dafListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dafListView.setSelection(position);
                if (position < tractate.getCellCount())
                    setNewCellNumber(position);
                drawer.closeDrawer(Gravity.RIGHT);
            }
        });
        dafListView.setItemChecked(tractate.getCellNumber(), true);
        dafListView.setSelection(tractate.getCellNumber());
        setCurrentText(ViewFactory.Text.GEMARA);
        setupDafBottomContainer();
    }

    private void hadranAlach() {
        StringBuilder hadranAlach = new StringBuilder(getText(R.string.hadranAlach));
        String replaceString = "TRACTATE";
        String tractateName = tractate.getIndex() == Tractate.MidosIndex ? "סדר קדשים" : tractate.getName();
        tractateName = "<b>" + tractateName + "</b>";
        while (hadranAlach.indexOf(replaceString) != -1)
            hadranAlach.replace(hadranAlach.indexOf(replaceString), hadranAlach.indexOf(replaceString) + replaceString.length(), tractateName);
        for (TextSwitcher switcher : textSwitchers)
            switcher.setText(Html.fromHtml(hadranAlach.toString().replaceAll("\n", "<br>")));
        toolbar.setTitle(getString(R.string.hadranAlachString));
    }

    private void initTabBar() {
        TabLayout tabLayout = hostActivity.getTablayout();
        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.removeAllTabs();
        tabLayout.setTabTextColors(getResources().getColorStateList(R.color.tab_text_color));
        for (ViewFactory.Text text : ViewFactory.Text.values()) {
            tabLayout.addTab(tabLayout.newTab().setTag(text).setText(text.getLable()), text == ViewFactory.Text.GEMARA);
        }
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!isAdded())
                    return;
                setCurrentText((ViewFactory.Text) tab.getTag());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    public void notifyDafDrawerData() {
        if (!isAdded())
            return;
        dafListView.setAdapter(new DafListViewAdapter(this, tractate));
        dafListView.setItemChecked(tractate.getCellNumber(), true);
        dafListView.setSelection(tractate.getCellNumber());
    }

    public void setupDafBottomContainer() {
        if (!isAdded())
            return;
        LinearLayout container = (LinearLayout) mainView.findViewById(R.id.dafBottomContainer);
        container.removeAllViews();
        container.setBackgroundColor(viewFactory.isStyleDark() ? getResources().getColor(R.color.background_material_dark) : Color.WHITE);

        container.addView(getDisplayOptionsRow());
        if (tractate.getIndex() != Tractate.KinimIndex)
            container.addView(getHadranRow());
    }

    private View getHadranRow() {
        View hadranRow = viewFactory.getRowWithIcon(R.drawable.ic_undo, getString(R.string.hadranAlachString), false);
        hadranRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.closeDrawer(Gravity.RIGHT);
                hadranAlach();
            }
        });
        return hadranRow;
    }

    public void closeDrawer(){
        drawer.closeDrawer(Gravity.RIGHT);
    }

    private View getDisplayOptionsRow() {
        View displayOptionsRow = viewFactory.getRowWithIcon(R.drawable.ic_invert_colors_grey600, "Display Options", false);
        displayOptionsRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.closeDrawer(Gravity.RIGHT);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(viewFactory.getDisplayOptions());
                builder.setPositiveButton("Close", null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        setupDafBottomContainer();
                        hostActivity.updateColors();
                        updateColors();
                    }
                });
                builder.create().show();
            }
        });
        return displayOptionsRow;
    }

    private void updateColors() {
        for (final ViewFactory.Text text : ViewFactory.Text.values()) {
            textSwitchers[text.ordinal()].removeAllViews();
            textSwitchers[text.ordinal()].setFactory(new ViewSwitcher.ViewFactory() {
                @Override
                public View makeView() {
                    return viewFactory.getTextView(text);
                }
            });
            textSwitchers[text.ordinal()].setText(tractate.getText(text));
            scrollViews[text.ordinal()].post(new Runnable() {
                @Override
                public void run() {
                    scrollViews[text.ordinal()].scrollTo(0, PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(getString(R.string.scrollKey) + text.getLable(), 0));
                }
            });
        }
        ArrayAdapter adapter = (ArrayAdapter) dafListView.getAdapter();
        adapter.notifyDataSetChanged();
        frameLayout.setBackgroundColor(viewFactory.getBackgroundColor());
        ViewFactory.setListViewColors(dafListView, getActivity());
    }

    public void setTractate(Tractate tractate) {
        this.tractate = tractate;
        this.tractate.initializeJSON();
    }


    public void setCurrentText(ViewFactory.Text newText) {
        currentText = newText;
        frameLayout.removeAllViews();
        frameLayout.addView(scrollViews[newText.ordinal()]);
    }

    private void setTextSwitcherAnimations(Animation inAnimation, Animation outAnimation) {
        for (TextSwitcher textSwitcher : textSwitchers) {
            textSwitcher.setInAnimation(inAnimation);
            textSwitcher.setOutAnimation(outAnimation);
        }
    }

    @Override
    public void nextPage() {
        if (tractate.isLastAmud())
            return;
        setTextSwitcherAnimations(viewFactory.inAn, viewFactory.outAn);
        tractate.nextAmud();
        newPage();
    }

    @Override
    public void previousPage() {
        if (tractate.isFirstAmud())
            return;
        setTextSwitcherAnimations(viewFactory.prevInAn, viewFactory.prevOutAn);
        tractate.previousAmud();
        newPage();
    }


    public void setNewCellNumber(int cellNumber) {
        final int oldCellNumber = tractate.getCellNumber();
        if (oldCellNumber != cellNumber) {
            Snackbar.make(mainView, "", Snackbar.LENGTH_SHORT)
                    .setAction("Go Back", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            tractate.setCellNumber(oldCellNumber);
                            newPage();
                        }
                    })
                    .show();
        }
        tractate.setCellNumber(cellNumber);
        newPage();
    }

    private void newPage() {
        scrollToZero();
        dafListView.setItemChecked(tractate.getCellNumber(), true);
        for (ViewFactory.Text text : ViewFactory.Text.values())
            textSwitchers[text.ordinal()].setText(tractate.getText(text));
        setTitles();
        setTextSwitcherAnimations(null, null);
        if (isPrimaryBody)
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putInt(getString(R.string.savedCellNumberKey), tractate.getCellNumber()).apply();
    }


    public void setTitles() {
        toolbar.setTitle(tractate.toString());
    }

    private void scrollToZero() {
        if (!isAdded())
            return;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        String key = getString(R.string.scrollKey);
        for (ViewFactory.Text text : ViewFactory.Text.values())
            editor.putInt(key + text.getLable(), 0);
        editor.apply();
        for (final ScrollView scrollView : scrollViews)
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.scrollTo(0, 0);
                }
            });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewFactory = new ViewFactory(getActivity());
        timeStarted = Calendar.getInstance().getTimeInMillis();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!isAdded())
            return;
            saveScrollPositions();
        new TimeLearnedUtil(getActivity()).addTime(timeStarted);


    }

    private void saveScrollPositions() {
        if (scrollViews[0] == null)
            return;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        String key = getString(R.string.scrollKey);
        for (ViewFactory.Text text : ViewFactory.Text.values())
            editor.putInt(key + text.getLable(), scrollViews[text.ordinal()].getScrollY());
        editor.apply();
    }
}
