package com.mattaniahbeezy.wisechildtalmud;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mattaniah on 6/9/2015.
 */
public class DisplayOptions {
    private final SharedPreferences sharedPref;
    Context context;
    private LinearLayout mainLL;
    private Resources res;

    public DisplayOptions(Context context) {
        this.context = context;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        mainLL = new LinearLayout(context);
        mainLL.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) context.getResources().getDimension(R.dimen.padding_left_and_right);
        mainLL.setPadding(padding, padding/2, padding, padding/2);
        res = context.getResources();

//            List<String> fontList = new ArrayList<>();
//            for (String string : context.getResources().getStringArray(R.array.fontValues))
//                fontList.add(string);
//            mainLL.addView(Settings.getSpinnerOption(context, context.getString(R.string.typefaceKey), fontList, "Typeface"));
//
//
//        //add theme spinner
//        List<String> themeList = new ArrayList<String>();
//        for (MegaActivity.Theme theme : MegaActivity.Theme.values())
//            themeList.add(MegaActivity.Theme.getThemeName(theme));
//        mainLL.addView(Settings.getSpinnerOption(context, bedtime ? context.getString(R.string.bedtimeThemeKey) : context.getString(R.string.themeKey), themeList, "Theme"));
//
//        //add line spacing options
//        List<String> lineSpacingOptionsList = new ArrayList<>();
//        for (Settings.LineSpacingOption option: Settings.LineSpacingOption.values())
//            lineSpacingOptionsList.add(Settings.LineSpacingOption.getTitle(option));
//        mainLL.addView(Settings.getSpinnerOption(context, Settings.LineSpacingOption.key, lineSpacingOptionsList, "Line Spacing"));
//
//        //add font size
//        mainLL.addView(Settings.getEditTextOption(context, context.getString(R.string.sizeKey), "25", InputType.TYPE_CLASS_NUMBER, "Text Size"));
    }


    public View getDisplayOptionsView() {
        return mainLL;
    }
}
