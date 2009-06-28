/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hlidskialf.android.alarmclock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.telephony.TelephonyManager;

/**
 * Manages alarms and vibe.  Singleton, so it can be initiated in
 * AlarmReceiver and shut down in the AlarmAlert activity
 */
class AlarmKlaxon implements Alarms.AlarmSettings {

    interface KillerCallback {
        public void onKilled();
    }

    /** Play alarm up to 10 minutes before silencing */
    final static int ALARM_TIMEOUT_SECONDS = 10 * 60;

    private static final long[] sVibratePattern = new long[] { 500, 500 };

    private int mAlarmId;
    private String mAlert;
    private Alarms.DaysOfWeek mDaysOfWeek;
    private boolean mVibrate;
    private int mDuration;
    private int mDelay;
    private boolean mVibrateOnly;
    private float mVolume, mCurVolume = -1;
    private int mCrescendo;
    private Context mContext;

    private boolean mPlaying = false;
    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;
    private KillerCallback mKillerCallback;

    // Internal messages
    private static final int KILLER = 1000;
    private static final int PLAY   = 1001;
    private static final int INC_VOL= 1002;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case KILLER:
                    if (Log.LOGV) {
                        Log.v("*********** Alarm killer triggered ***********");
                    }
                    if (mKillerCallback != null) {
                        mKillerCallback.onKilled();
                    }
                    break;
                case PLAY:
                    play((Context) msg.obj, msg.arg1);
                    break;
                case INC_VOL: 
                    if (mMediaPlayer != null) {
                      mCurVolume += (Float)msg.obj;
                      mMediaPlayer.setVolume(mCurVolume, mCurVolume);
                    }
                    break;
            }
        }
    };

    private Runnable mLoopCallback = new Runnable() {
      public void run() {
        if (mPlaying)
          play(mContext, mAlarmId);
      } 
    };
    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
      public void onCompletion(MediaPlayer player) {
        player.stop();
        if (mPlaying)
          mHandler.postDelayed(mLoopCallback, mDelay);
      }
    };
    private class CrescendoThread extends Thread {
      private int num_steps = 10;
      private int mStepDelay;
      private float mStepDelta;
      private Handler mCrescendoHandler;
      private boolean mRunning = false;
      public CrescendoThread() {
        mStepDelay = mCrescendo / num_steps;
        mStepDelta = mVolume / (float)num_steps;
        mCrescendoHandler = new Handler();
      }
      public void start() {
        mRunning = true;
        super.start();
      }
      public void run() {
        if (!mRunning) return;
        if (mCurVolume < mVolume) {
          mCurVolume += mStepDelta;
          mHandler.sendMessage(mHandler.obtainMessage(INC_VOL, (Float)mStepDelta));
          mCrescendoHandler.postDelayed(CrescendoThread.this, mStepDelay);
        }
      }
      public void done() {
        mRunning = false;
        mCrescendoHandler.removeCallbacks(CrescendoThread.this);
      }
    };
    private CrescendoThread mCrescendoThread;

    AlarmKlaxon() {
    }

    public void reportAlarm(
            int idx, boolean enabled, int hour, int minutes,
            Alarms.DaysOfWeek daysOfWeek, boolean vibrate, String message, String alert,
            int snooze, int duration, int delay, boolean vibrate_only, 
            int volume, int crescendo,
            int captcha_snooze, int captcha_dismiss
            ) {
        if (Log.LOGV) Log.v("AlarmKlaxon.reportAlarm: " + idx + " " + hour +
                            " " + minutes + " dow " + daysOfWeek);
        mAlert = alert;
        mDaysOfWeek = daysOfWeek;
        mVibrate = vibrate;
        mDuration = duration*1000;
        mDelay = delay;
        mVibrateOnly = vibrate_only;
        mVolume = (float)volume/100f;
        mCrescendo = crescendo*1000;
    }

    public void postPlay(final Context context, final int alarmId) {
        mHandler.sendMessage(mHandler.obtainMessage(PLAY, alarmId, 0, context));
    }

    // Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;

    private void play(Context context, int alarmId) {
        if (mVibrator == null) {
          mVibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        ContentResolver contentResolver = context.getContentResolver();

        if (mPlaying) stop(context, false);

        mContext = context;
        mAlarmId = alarmId;

        /* this will call reportAlarm() callback */
        Alarms.getAlarm(contentResolver, this, mAlarmId);

        if (Log.LOGV) Log.v("AlarmKlaxon.play() " + mAlarmId + " alert " + mAlert);

      if (!mVibrateOnly) {
        // TODO: Reuse mMediaPlayer instead of creating a new one and/or use
        // RingtoneManager.
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(new OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e("Error occurred while playing audio.");
                mp.stop();
                mp.release();
                mMediaPlayer = null;
                return true;
            }
        });

        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(
                    Context.TELEPHONY_SERVICE);
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low volume to not disrupt the call.
            if (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                Log.v("Using the in-call alarm");
                mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                setDataSourceFromResource(context.getResources(),
                        mMediaPlayer, R.raw.in_call_alarm);
            } else {
                mMediaPlayer.setDataSource(context, Uri.parse(mAlert));
            }
            startAlarm(mMediaPlayer);
        } catch (Exception ex) {
            Log.v("Using the fallback ringtone");
            // The alert may be on the sd card which could be busy right now.
            // Use the fallback ringtone.
            try {
                // Must reset the media player to clear the error state.
                mMediaPlayer.reset();
                setDataSourceFromResource(context.getResources(), mMediaPlayer,
                        R.raw.fallbackring);
                startAlarm(mMediaPlayer);
            } catch (Exception ex2) {
                // At this point we just don't play anything.
                Log.e("Failed to play fallback ringtone", ex2);
            }
        }
      }

        /* Start the vibrator after everything is ok with the media player */
        if (mVibrateOnly || mVibrate) {
            mVibrator.vibrate(sVibratePattern, 0);
        } else {
            mVibrator.cancel();
        }

        enableKiller();
        mPlaying = true;
    }

    // Do the common stuff when starting the alarm.
    private void startAlarm(MediaPlayer player)
            throws java.io.IOException, IllegalArgumentException,
                   IllegalStateException {
        player.setAudioStreamType(AudioManager.STREAM_ALARM);

        if (mDelay > 0) {
          player.setLooping(false);
          player.setOnCompletionListener(mCompletionListener);
        }
        else
          player.setLooping(true);

        if (mCrescendo > 0) {
          if (mCurVolume == -1) {
            mCurVolume = 0.05f;
            player.setVolume(mCurVolume, mCurVolume);
            mCrescendoThread = new CrescendoThread();
            mCrescendoThread.start();
          }
        }
        else 
          player.setVolume(mVolume, mVolume);

        player.prepare();
        player.start();
    }

    private void setDataSourceFromResource(Resources resources,
            MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        }
    }

    /**
     * Stops alarm audio and disables alarm if it not snoozed and not
     * repeating
     */
    public void stop(Context context, boolean snoozed) {
        if (Log.LOGV) Log.v("AlarmKlaxon.stop() " + mAlarmId);
        if (mPlaying) {
            mPlaying = false;

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            if (mVibrator == null) {
              mVibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
            }

            // Stop vibrator
            mVibrator.cancel();

            /* disable alarm only if it is not set to repeat */
            if (!snoozed && ((mDaysOfWeek == null || !mDaysOfWeek.isRepeatSet()))) {
                Alarms.enableAlarm(context, mAlarmId, false);
            }
        }
        disableKiller();
    }

    /**
     * This callback called when alarm killer times out unattended
     * alarm
     */
    public void setKillerCallback(KillerCallback killerCallback) {
        mKillerCallback = killerCallback;
    }

    /**
     * Kills alarm audio after ALARM_TIMEOUT_SECONDS, so the alarm
     * won't run all day.
     *
     * This just cancels the audio, but leaves the notification
     * popped, so the user will know that the alarm tripped.
     */
    private void enableKiller() {
      if (mDuration > 0)
        mHandler.sendMessageDelayed(mHandler.obtainMessage(KILLER), mDuration);
    }

    private void disableKiller() {
        mHandler.removeMessages(KILLER);
    }


}
