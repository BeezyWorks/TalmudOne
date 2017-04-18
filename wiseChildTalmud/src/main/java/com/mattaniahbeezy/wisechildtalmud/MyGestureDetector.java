package com.mattaniahbeezy.wisechildtalmud;

/**
 * Created by Mattaniah on 6/1/2015.
 */

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private int SWIPE_MIN_DISTANCE;
    private GestureHost gestureHost;
    private Context context;

    public interface GestureHost{
        public void previousPage();
        public void nextPage();
    }

    public MyGestureDetector(Context context, GestureHost gestureHost){
        this.gestureHost=gestureHost;
        this.context = context;
        SWIPE_MIN_DISTANCE = (int) context.getResources().getDimension(R.dimen.swipe_min_distance);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        try {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)

                return false;
            // right to left swipe
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                gestureHost.previousPage();
                return true;
                //				left to right swipe
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                gestureHost.nextPage();
                return true;
            }
        } catch (Exception e) {
            // nothing
        }
        return false;
    }

    public static View.OnTouchListener getOnTouchListener(Context context, GestureHost gestureHost){
        final MyGestureDetector gestureDetector = new MyGestureDetector(context, gestureHost);
        final GestureDetector detector = new GestureDetector(context, gestureDetector);
        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        };
        return onTouchListener;
    }

}