package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mattaniah on 6/9/2015.
 */
public class ViewFactory {
    Context context;
    SharedPreferences sharedPref;
    private Style overrideStyle = null;

    Animation inAn;
    Animation outAn;
    Animation prevInAn;
    Animation prevOutAn;

    public ViewFactory(Context context) {
        if (context == null)
            return;
        this.context = context;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        inAn = AnimationUtils.loadAnimation(context.getApplicationContext(), R.anim.enter_animation);
        outAn = AnimationUtils.loadAnimation(context.getApplicationContext(), R.anim.exit_animation);
        prevInAn = AnimationUtils.loadAnimation(context.getApplicationContext(), R.anim.enter_animation_previous);
        prevOutAn = AnimationUtils.loadAnimation(context.getApplicationContext(), R.anim.exit_animation_previous);
    }

    public ScrollView getScrollView() {
        ScrollView scrollView = new ScrollView(context);
        return scrollView;
    }

    public TextView getTextView(Text text) {
        TextView textView = new TextView(context);
        try {
            textView.setTextSize(Integer.valueOf(sharedPref.getString(context.getString(R.string.textSizeKey), "25")));
        }
        catch (Exception e){
            textView.setTextSize(25);
            sharedPref.edit().putString(context.getString(R.string.textSizeKey), "25").apply();
        }
        textView.setTypeface(getTypeface(text == Text.ENGLISH));
        textView.setTextColor(getTextColor());
        int sidePadding = (int) context.getResources().getDimension(R.dimen.padding_left_and_right);
        int topPadding = (int) context.getResources().getDimension(R.dimen.small_padding_top);
        textView.setPadding(sidePadding, topPadding, sidePadding, topPadding);
        textView.setLineSpacing(1f, getLineSpace().getValue());
        return textView;
    }

    public LineSpaceOptions getLineSpace() {
        String key = LineSpaceOptions.key;
        String savedOption = sharedPref.getString(key, LineSpaceOptions.SINGLE.getLabel());
        for (LineSpaceOptions options : LineSpaceOptions.values())
            if (savedOption.equals(options.getLabel()))
                return options;

        return LineSpaceOptions.SINGLE;

    }

    public enum LineSpaceOptions {
        SINGLE, ONE_TWENTY, ONE_FOURTY_FIVE;
        public LineSpaceOptions value = this;
        public static String key = "lineSpaceOptions";

        public String getLabel() {
            switch (value) {
                case SINGLE:
                    return "Single";
                case ONE_TWENTY:
                    return "120%";
                case ONE_FOURTY_FIVE:
                    return "145%";

            }
            return "Single";
        }

        public float getValue() {
            switch (value) {

                case SINGLE:
                    return 1f;
                case ONE_TWENTY:
                    return 1.2f;
                case ONE_FOURTY_FIVE:
                    return 1.45f;
            }
            return 1f;
        }

    }

    public Typeface getTypeface(boolean english) {
        String key = english ? context.getString(R.string.englishTypefaceKey) : context.getString(R.string.typefaceKey);
        String defaultValue = english ? "Lato.ttf" : "TaameyFrankCLM.ttf";
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return Typeface.createFromAsset(context.getAssets(), sharedPref.getString(key, defaultValue));
        } catch (Exception e) {
            sharedPref.edit().putString(key, defaultValue).apply();
            return getTypeface(english);
        }
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public int getBackgroundColor() {
        Resources res = context.getResources();
        switch (getStyle()) {

            case NIGHT:
                return res.getColor(R.color.background_material_dark);
            case DAY:
                return res.getColor(R.color.background_material_light);
            case SEPIA:
                return res.getColor(R.color.sepia);
            case AMOLED_NIGHT:
                return Color.BLACK;
        }
        return res.getColor(R.color.background_material_light);
    }

    public int getTextColor() {
        Resources res = context.getResources();
        return isStyleDark() ? res.getColor(R.color.abc_primary_text_material_dark) : res.getColor(R.color.abc_primary_text_material_light);
    }

    public boolean isStyleDark() {
        Style style = getStyle();
        return style == Style.NIGHT || style == Style.AMOLED_NIGHT;
    }

    public void setOverrideStyle(Style style) {
        this.overrideStyle = style;
    }

    public Style getStyle() {
        if (overrideStyle != null)
            return overrideStyle;
        String key = Style.key;
        String savedTheme = sharedPref.getString(key, Style.DAY.getStyleName());
        for (Style style : Style.values())
            if (savedTheme.equals(style.getStyleName()))
                return style;

        return Style.values()[0];
    }

    public int getPrimaryHighlightColor(){
        return isStyleDark()?context.getResources().getColor(R.color.primaryBright):context.getResources().getColor(R.color.primary);
    }

    public enum Style {
        DAY, NIGHT, SEPIA, AMOLED_NIGHT;
        public static String key = "StyleKey";
        public Style value = this;

        public String getStyleName() {
            switch (value) {
                case NIGHT:
                    return "Night";
                case DAY:
                    return "Day";
                case SEPIA:
                    return "Sepia";
                case AMOLED_NIGHT:
                    return "AMOLED Night";

            }
            return "I am error";
        }
    }

    public enum Text {
        ENGLISH, TOSAFOS, RASHI, GEMARA;
        public Text value = this;

        public String getLable() {
            switch (value) {
                case GEMARA:
                    return "\u05D2\u05DE\u05E8\u05D0";
                case RASHI:
                    return "\u05E8\u05E9\'\'\u05D9";
                case TOSAFOS:
                    return "\u05EA\u05D5\u05E1\'";
                case ENGLISH:
                    return "English";
            }
            return "I am error";
        }

        public String getTextKey() {
            switch (value) {
                case GEMARA:
                    return "gemara";
                case RASHI:
                    return "rashi";
                case TOSAFOS:
                    return "tosafos";
                case ENGLISH:
                    return "english";
            }
            return "I am error";
        }
    }

    public static void setListViewColors(ListView listView, Activity context) {
        int selectorResource = new ViewFactory(context).isStyleDark() ? R.drawable.listview_selector_dark : R.drawable.listview_selector_light;
        listView.setSelector(selectorResource);
        listView.setBackgroundColor(new ViewFactory(context).isStyleDark() ? context.getResources().getColor(R.color.background_floating_material_dark) : context.getResources().getColor(R.color.background_floating_material_light));
    }

    public ColorStateList getListViewTextColor() {
        return isStyleDark() ? context.getResources().getColorStateList(R.color.listviewtext_dark) : context.getResources().getColorStateList(R.color.listviewtext_light);
    }

    public Drawable getListViewIemBackground() {
        return isStyleDark() ? context.getResources().getDrawable(R.drawable.listview_selector_dark) : context.getResources().getDrawable(R.drawable.listview_selector_light);
    }

    public static void setDrawerWidths(View view, Activity context) {
        DisplayMetrics metrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float width = metrics.widthPixels;
        int actionBarHeight = (int) context.getResources().getDimension(R.dimen.abc_action_bar_default_height_material);
        float drawerWidth = width - actionBarHeight;
        float maxDrawerWidth = context.getResources().getDimension(R.dimen.max_drawer_width);
        if (drawerWidth > maxDrawerWidth)
            drawerWidth = maxDrawerWidth;

        ViewGroup.LayoutParams rightParams = view.getLayoutParams();
        rightParams.width = (int) drawerWidth;
        view.setLayoutParams(rightParams);
    }

    public AlertDialog searchDialog() {
        View dialogView = View.inflate(context, R.layout.searchdialoglayout, null);
        final Spinner tractateSpinner = (Spinner) dialogView.findViewById(R.id.tractateSpinner);
        final RadioGroup textRadios = (RadioGroup) dialogView.findViewById(R.id.textRadios);
        final RadioButton shasRadioButton = (RadioButton) dialogView.findViewById(R.id.shasButton);
        final EditText searchField = (EditText) dialogView.findViewById(R.id.searchTerm);
        shasRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tractateSpinner.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            }
        });
        shasRadioButton.setChecked(true);
        for (Text text : Text.values()) {
            RadioButton radioButton = new RadioButton(context);
            radioButton.setTag(text);
            radioButton.setText(text.getLable());
            textRadios.addView(radioButton);
            radioButton.setChecked(text == Text.GEMARA);
        }
        tractateSpinner.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, Tractate.masechtosBavli));

        return new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SearchUtil searchUtil = new SearchUtil(context);
                        textRadios.getCheckedRadioButtonId();
                        int[] scope = shasRadioButton.isChecked() ? new int[Tractate.masechtosBavli.length - 1] : new int[1];
                        if (shasRadioButton.isChecked())
                            for (int i = 0; i < scope.length; i++)
                                scope[i] = i;
                        else
                            scope[0] = tractateSpinner.getSelectedItemPosition();
                        SearchUtil.Query query = new SearchUtil.Query(searchField.getText().toString(), ((Text) textRadios.findViewById(textRadios.getCheckedRadioButtonId()).getTag()), scope);
                        searchUtil.execute(query);
                    }
                })
                .create();
    }

    public View getSpinnerOption(final String key, final List<String> values, String Title) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        View retView = View.inflate(context, R.layout.spinner_option, null);
        Spinner spinner = (Spinner) retView.findViewById(R.id.optionSpinner);
        TextView textView = (TextView) retView.findViewById(R.id.optionTitle);
        textView.setText(Title);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_1,
                values);
        spinner.setAdapter(adapter);
        int selection = 0;
        for (int i = 0; i < values.size(); i++) {
            if (sharedPref.getString(key, values.get(0)).equals(values.get(i)))
                selection = i;
        }

        spinner.setSelection(selection);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sharedPref.edit().putString(key, values.get(position)).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        return retView;
    }

    public View getEditTextOption(final String key, String defaultValue, int inputType, String title) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View retView = inflater.inflate(R.layout.edittext_option, null);
        TextView titleTv = (TextView) retView.findViewById(R.id.editTextTitle);
        EditText editText = (EditText) retView.findViewById(R.id.editText);

        titleTv.setText(title);
        editText.setInputType(inputType);
        editText.setText(sharedPref.getString(key, defaultValue));

        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    sharedPref.edit().putString(key, s.toString().trim()).apply();
                }
            }
        });

        return retView;
    }

    public View getRadioView(String key, String[] values) {
        return getRadioView(key, values, values);
    }

    public View getRadioView(final String key, String[] titles, String[] values) {
        RadioGroup radioGroup = new RadioGroup(context);
        String currentValue = PreferenceManager.getDefaultSharedPreferences(context).getString(key, values[0]);
        for (int i = 0; i < values.length; i++) {
            RadioButton button = new RadioButton(context);
            button.setText(titles[i]);
            radioGroup.addView(button);
            final String value = values[i];
            button.setChecked(currentValue.equals(value));
            button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                        editor.putString(key, value).apply();
                    }

                }
            });
            setViewProperties(button);
        }
        radioGroup.setGravity(Gravity.CENTER_VERTICAL);
        return radioGroup;
    }

    private void setViewProperties(TextView view) {
        view.setTextSize(18);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setMinHeight((int) context.getResources().getDimension(R.dimen.row_height));
    }

    public View getRowWithIcon(int iconResource, String rowTitle, boolean showDivider) {
        View retView = View.inflate(context, R.layout.row_with_image, null);
        TextView title = (TextView) retView.findViewById(R.id.bottomItemTitle);
        ImageView image = (ImageView) retView.findViewById(R.id.bottomItemImage);
        if (showDivider)
            retView.findViewById(R.id.divider).setVisibility(View.VISIBLE);
        title.setText(rowTitle);

        image.setImageDrawable(getDrawableWithTintList(iconResource));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            image.setImageTintList(getListViewTextColor());
        else
            image.setColorFilter(isStyleDark() ? Color.WHITE : context.getResources().getColor(android.R.color.darker_gray));
        title.setTextColor(getListViewTextColor());
        return retView;
    }

    private Drawable getDrawableWithTintList(int drawableId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable drawable = context.getDrawable(drawableId);
            drawable.setTintList(getListViewTextColor());
            return drawable;
        }
        Drawable drawable = context.getResources().getDrawable(drawableId);
        DrawableCompat.wrap(drawable);
        DrawableCompat.setTintList(drawable, getListViewTextColor());
        return drawable;
    }

    public View getDisplayOptions() {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) context.getResources().getDimension(R.dimen.padding_left_and_right);
        linearLayout.setPadding(padding, padding / 2, padding, padding / 2);

        //Add Style Spinner
        List<String> styleValues = new ArrayList<>();
        for (Style style : Style.values())
            styleValues.add(style.getStyleName());
        linearLayout.addView(getSpinnerOption(Style.key, styleValues, "Theme"));

        List<String> typefaceValues = new ArrayList<>();
        for (String typeface : context.getResources().getStringArray(R.array.typefaceNames))
            typefaceValues.add(typeface);
        linearLayout.addView(getSpinnerOption(context.getString(R.string.typefaceKey), typefaceValues, "Typeface"));

        List<String> englishTypefaceValues = new ArrayList<>();
        for (String typeface : context.getResources().getStringArray(R.array.englishTypefaceNames))
            englishTypefaceValues.add(typeface);
        linearLayout.addView(getSpinnerOption(context.getString(R.string.englishTypefaceKey), englishTypefaceValues, "English Typeface"));

        List<String> lineSpaceValues = new ArrayList<>();
        for (LineSpaceOptions options : LineSpaceOptions.values())
            lineSpaceValues.add(options.getLabel());
        linearLayout.addView(getSpinnerOption(LineSpaceOptions.key, lineSpaceValues, "Line Spacing"));

        linearLayout.addView(getEditTextOption(context.getString(R.string.textSizeKey), "25", InputType.TYPE_CLASS_NUMBER, "Text Size"));

        return linearLayout;
    }

    public static class URLToImageView extends AsyncTask {
        ImageView imageView;
        String url;
        Bitmap bitmap;
        int diameter;

        public URLToImageView(ImageView imageView, String url, int diameter) {
            this.imageView = imageView;
            this.url = url;
            this.diameter=diameter;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            try {
                bitmap = getRoundedShape(BitmapFactory.decodeStream((InputStream) new URL(url).getContent()), diameter);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    public static Bitmap getRoundedShape(Bitmap scaleBitmapImage, int diameter) {
        int targetWidth = diameter;
        int targetHeight = diameter;
        Bitmap targetBitmap = Bitmap.createBitmap(targetWidth,
                targetHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(targetBitmap);
        Path path = new Path();
        path.addCircle(((float) targetWidth - 1) / 2,
                ((float) targetHeight - 1) / 2,
                (Math.min(((float) targetWidth),
                        ((float) targetHeight)) / 2),
                Path.Direction.CCW);

        canvas.clipPath(path);
        Bitmap sourceBitmap = scaleBitmapImage;
        canvas.drawBitmap(sourceBitmap,
                new Rect(0, 0, sourceBitmap.getWidth(),
                        sourceBitmap.getHeight()),
                new Rect(0, 0, targetWidth,
                        targetHeight), null);
        return targetBitmap;
    }


    public Bitmap decodeSampledBitmapFromResource(int resId,
                                                  int reqWidth, int reqHeight) {
        Resources res = context.getResources();

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
