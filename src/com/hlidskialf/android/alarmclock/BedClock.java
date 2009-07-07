package com.hlidskialf.android.alarmclock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class BedClock extends Activity  implements View.OnClickListener, ViewSwitcher.ViewFactory
{
  private static final int MENUITEM_FLIP=1;
  private static final int MENUITEM_CLOSE=2;
  private static final int MENUITEM_ANIM=3;

  private static final int ANIMATION_FADE=0;
  private static final int ANIMATION_PUSHUP=1;
  private static final int ANIMATION_PUSHLEFT=2;
  private static final int ANIMATION_PUSHDOWN=3;
  private static final int ANIMATION_PUSHRIGHT=4;
  private static final int ANIMATION_EXPLODE=5;
  private static final int ANIMATION_SHATTER=6;
  private static final int ANIMATION_HYPER=7;

  private TextSwitcher mSwitchHour,mSwitchMin,mSwitchSec;
  private String mHour,mMin,mSec;
  private ArrayList<int[]> mAnimations;
  private int mAnim;

  private Calendar mCal;
  private Handler mHandler;
  private Runnable mCallback;
  private LayoutInflater mInflater;



  private int mOrient;
  private boolean mDoWakeLock;
  private PowerManager mPower;
  private BatteryManager mBattery;
  private BroadcastReceiver mBatteryReceiver;
  private static PowerManager.WakeLock mWakeLock;
  private SharedPreferences mPrefs;


  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    getWindow().requestFeature(Window.FEATURE_NO_TITLE);

    mInflater = getLayoutInflater();

    setContentView(R.layout.bedclock);

    mSwitchHour = (TextSwitcher)findViewById(R.id.bedclock_time_hour);
    mSwitchHour.setFactory(this);
    mSwitchHour.setOnClickListener(this);
    mSwitchMin = (TextSwitcher)findViewById(R.id.bedclock_time_min);
    mSwitchMin.setFactory(this);
    mSwitchMin.setOnClickListener(this);
    mSwitchSec = (TextSwitcher)findViewById(R.id.bedclock_time_sec);
    mSwitchSec.setFactory(this);
    mSwitchSec.setOnClickListener(this);

    mAnimations = new ArrayList<int[]>();
    mAnimations.add(ANIMATION_FADE,     new int[] {R.anim.slow_fade_in, R.anim.slow_fade_out});
    mAnimations.add(ANIMATION_PUSHUP,   new int[] {R.anim.push_up_in, R.anim.push_up_out});
    mAnimations.add(ANIMATION_PUSHLEFT, new int[] {R.anim.push_left_in, R.anim.push_left_out});
    mAnimations.add(ANIMATION_PUSHDOWN, new int[] {R.anim.push_down_in, R.anim.push_down_out});
    mAnimations.add(ANIMATION_PUSHRIGHT,new int[] {R.anim.push_right_in, R.anim.push_right_out});
    mAnimations.add(ANIMATION_EXPLODE,  new int[] {R.anim.slow_fade_in, R.anim.wave_scale});
    mAnimations.add(ANIMATION_SHATTER,  new int[] {R.anim.shake, R.anim.push_down_out});
    mAnimations.add(ANIMATION_HYPER,    new int[] {R.anim.hyperspace_in, R.anim.hyperspace_out});
    
    mPrefs = getSharedPreferences(AlarmClock.PREFERENCES, 0);

    setAnimation( mPrefs.getInt("bedclock_animation", ANIMATION_FADE) );

    mOrient = mPrefs.getInt("bedclock_orientation", ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    setRequestedOrientation(mOrient);

    mDoWakeLock = mPrefs.getBoolean("bedclock_wake_lock", false);
    if (mDoWakeLock) {
        mPower = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPower.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, BedClock.class.getName());
    }

    mCal = Calendar.getInstance();
    mHandler = new Handler();
    mBatteryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
           int plugged = intent.getIntExtra("plugged", 0);
           if (mWakeLock == null) return;
           if ((plugged == BatteryManager.BATTERY_PLUGGED_AC) || (plugged ==  BatteryManager.BATTERY_PLUGGED_USB) )
           {
               if (!mWakeLock.isHeld()) {
                  mWakeLock.acquire();
                  android.util.Log.v("Alarming","BedClock: wakelock/acquire-plugged");
               }
           } else {
               if (mWakeLock.isHeld()) {
                  mWakeLock.release();
                  android.util.Log.v("Alarming","BedClock: wakelock/release-unplug");
               }
           }
        }
    }; 
  }
  @Override 
  protected void onDestroy() {
    if (mWakeLock != null && mWakeLock.isHeld()) {
      mWakeLock.release();
      android.util.Log.v("Alarming","BedClock: wakelock/release-destroy");
    }
    super.onDestroy();
  }
  @Override
  public void onResume() {
    super.onResume();

    if (mDoWakeLock && mBatteryReceiver != null) {
      registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    mCallback = new Runnable() { 
      public void run() {
        BedClock.this.updateClock();
        mHandler.postDelayed(mCallback, 1000);
      }
    };
    mHandler.postDelayed(mCallback, 0);
  }
  @Override
  public void onPause() {
    cleanup();
    super.onPause();
  }
  public void setAnimation(int anim)
  {
    mAnim = anim;

    mPrefs.edit().putInt("bedclock_animation",mAnim).commit();
    int[] anim_ids = mAnimations.get(anim);

    Animation in_anim = AnimationUtils.loadAnimation(this, anim_ids[0]);
    Animation out_anim = AnimationUtils.loadAnimation(this, anim_ids[1]);

    mSwitchHour.setInAnimation( in_anim );
    mSwitchHour.setOutAnimation( out_anim );
    mSwitchMin.setInAnimation( in_anim );
    mSwitchMin.setOutAnimation( out_anim );
    mSwitchSec.setInAnimation( in_anim );
    mSwitchSec.setOutAnimation( out_anim );
  }
  public void updateClock()
  {
    boolean is24 = Alarms.get24HourMode(this);
    mCal.setTime(new Date());


    String s;
    s = DateFormat.format(is24 ? "k:" : "h:",mCal).toString();
    if (!s.equals(mHour)) {
        mSwitchHour.setText(mHour = s);
    }
    s = DateFormat.format(mOrient == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ? "mm:" : "mm", mCal).toString();
    if (!s.equals(mMin)) {
        mSwitchMin.setText(mMin = s);
    }
    s = DateFormat.format("ss",mCal).toString();
    if (!s.equals(mSec)) {
        mSwitchSec.setText(mSec = s);
    }
  }

  public View makeView() {
    return mInflater.inflate(R.layout.bedclock_text, null);
  }

  public void onClick(View v) { finish(); }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuItem mi;
    mi = menu.add(0,MENUITEM_ANIM,0, R.string.next_animation);
    mi.setIcon(android.R.drawable.ic_menu_rotate);
    mi = menu.add(0,MENUITEM_FLIP,0, R.string.flip_orientation);
    mi.setIcon(android.R.drawable.ic_menu_always_landscape_portrait);
    mi = menu.add(0,MENUITEM_CLOSE,0, R.string.hide_clock);
    mi.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    switch (id)  {
    case MENUITEM_FLIP:
      int orient = getRequestedOrientation();
      orient = (orient == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) ? 
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
      if (orient != mOrient) {
        setRequestedOrientation(mOrient = orient);
        mPrefs.edit().putInt("bedclock_orientation", mOrient).commit();
      }
      return true;
    case MENUITEM_CLOSE:
      cleanup();
      finish();
      return true;
    case MENUITEM_ANIM:
      setAnimation(mAnim+1 >= mAnimations.size() ? 0 : mAnim+1);
      return true;
    }
    return false;
  }


  public boolean onKeyDown(int keyCode, KeyEvent event) { 
    if (keyCode == KeyEvent.KEYCODE_MENU) {
      return super.onKeyDown(keyCode,event);
    }
    
    cleanup();
    finish(); 
    return true; 
  }

  public void cleanup() {
    try {
    if (mWakeLock != null && mWakeLock.isHeld()) {
      mWakeLock.release();
      android.util.Log.v("Alarming","BedClock: wakelock/release-cleanup");
    }
    if (mDoWakeLock && mBatteryReceiver != null) {
        unregisterReceiver(mBatteryReceiver);
    }
    if (mHandler != null && mCallback != null) {
      mHandler.removeCallbacks(mCallback);
      mCallback = null;
    }
    } catch (Exception e) {
      //just cleanup gracefully
      android.util.Log.v("Alarming", "BedClock: cleanup exception!");
    }
  }

}
