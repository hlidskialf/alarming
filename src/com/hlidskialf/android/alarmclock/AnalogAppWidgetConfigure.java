
package com.hlidskialf.android.alarmclock;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.RemoteViews;

public class AnalogAppWidgetConfigure extends Activity 
                         implements AdapterView.OnItemSelectedListener, 
                                    AdapterView.OnItemClickListener 
{
    public final static int[] CLOCKS = {
        R.layout.clock_basic_bw,
        R.layout.clock_googly,
        R.layout.clock_droid2,
        R.layout.clock_droids,
        R.layout.analog_appwidget,
        R.layout.clock_orologio,
        R.layout.clock_roman,
        R.layout.clock_moma,
        R.layout.clock_faceless,
        R.layout.clock_faceless_white,
        R.layout.clock_whatever,
        R.layout.clock_whatever_white,
        R.layout.clock_seethru,
        R.layout.clock_alarm,
        R.layout.clock_pocket,
        R.layout.clock_return,
    };
    private LayoutInflater mFactory;
    private Gallery mGallery;
    private View mClock;
    private ViewGroup mClockLayout;
    private int mPosition;
    private int mAppWidgetId;
    private SharedPreferences mPrefs;

    class ClockAdapter extends BaseAdapter {
        public ClockAdapter() { }
        public int getCount() { return CLOCKS.length; }
        public Object getItem(int position) { return position; }
        public long getItemId(int position) { return position; }
        public View getView(final int position, View convertView, ViewGroup parent) {
            return mFactory.inflate(CLOCKS[position], null);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
      super.onCreate(icicle);
      setContentView(R.layout.clockpicker);
      mFactory = LayoutInflater.from(this);

      View layout = findViewById(android.R.id.content);
      layout.setBackgroundResource(R.drawable.bwgradient);

      Intent intent = getIntent();
      mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

      mGallery = (Gallery) findViewById(R.id.gallery);
      mGallery.setAdapter(new ClockAdapter());
      mGallery.setOnItemSelectedListener(this);
      mGallery.setOnItemClickListener(this);

      mClockLayout = (ViewGroup) findViewById(R.id.clock_layout);
      mClockLayout.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          updateAndFinish();
        }
      });

      mPrefs = getSharedPreferences(AlarmClock.PREFERENCES, 0);
      int face = mPrefs.getInt(AlarmClock.PREF_WIDGET_CLOCK_FACE, 0);
      if (face < 0 || face > CLOCKS.length) 
        face = 0;
      mGallery.setSelection(face, false);

      setResult(RESULT_CANCELED);
    }

    public void onItemSelected(AdapterView parent, View v, int position, long id) {
      if (mClock != null)
        mClockLayout.removeView(mClock);
      mClock = mFactory.inflate(CLOCKS[position], null);
      mClockLayout.addView(mClock, 0);
      mPosition = position;
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
      mPosition = position;
      updateAndFinish();
    }
    public void onNothingSelected(AdapterView parent) { }

    private void updateAndFinish()
    {
      mPrefs.edit().putInt(AlarmClock.PREF_WIDGET_CLOCK_FACE, mPosition).commit();

      AppWidgetManager awm = AppWidgetManager.getInstance(this);
      RemoteViews views = new RemoteViews(getPackageName(), CLOCKS[mPosition]);

      Intent config = new Intent(this, AnalogAppWidgetConfigure.class);
      config.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
      PendingIntent configureIntent = PendingIntent.getActivity(this, 0, config, 0);
      views.setOnClickPendingIntent(R.id.clock, configureIntent);

      if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { // called by tap
        int[] appWidgetIds = awm.getAppWidgetIds(new ComponentName(
          this, AnalogAppWidgetProvider.class));
        awm.updateAppWidget(appWidgetIds, views);
        
      } else { // called on configure
        awm.updateAppWidget(mAppWidgetId, views);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, result);
      }
      
      finish();
    }
}
