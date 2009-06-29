/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.provider.Settings;
import android.net.Uri;

import com.hlidskialf.android.preference.SeekBarPreference;

/**
 * Settings for the Alarm Clock.
 */
public class SettingsActivity extends PreferenceActivity {

    private static final int ALARM_STREAM_TYPE_BIT =
            1 << AudioManager.STREAM_ALARM;
    
    private static final String KEY_ALARM_IN_SILENT_MODE = "alarm_in_silent_mode";
    private static final String KEY_DEFAULT_ALARM = "default_alarm";
    private CheckBoxPreference mAlarmInSilentModePref;
    private AlarmPreference mAlarmPref;
    private SeekBarPreference mSnoozePref, mDurationPref, mVolumePref, mCrescendoPref, mDelayPref;
    private ListPreference mCaptchaSnoozePref, mCaptchaDismissPref;
    private SharedPreferences mPrefs;


    private Preference.OnPreferenceChangeListener mSeekBarChange = 
      new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference p, Object newValue) {
          SeekBarPreference sbp= (SeekBarPreference)p;
          p.setSummary(sbp.getText());
          return true;
        }
      };
    private Preference.OnPreferenceChangeListener mListChange = 
      new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference p, Object newValue) {
          ListPreference lp = (ListPreference)p;
          lp.setValueIndex(lp.findIndexOfValue((String)newValue));
          lp.setSummary( lp.getEntry() );
          return true;
        }
      };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.settings);

        mPrefs = getSharedPreferences(AlarmClock.PREFERENCES, 0);
        
        mAlarmInSilentModePref = (CheckBoxPreference) findPreference(KEY_ALARM_IN_SILENT_MODE);

        mAlarmPref = (AlarmPreference)findPreference("default_alarm");
        Ringtone tone = RingtoneManager.getRingtone(this, Uri.parse(mPrefs.getString("default_alarm","")));
        if (tone != null) {
          mAlarmPref.setSummary(tone.getTitle(this));
        }
        mAlarmPref.setRingtoneChangedListener(new AlarmPreference.IRingtoneChangedListener() {
            public void onRingtoneChanged(Uri ringtoneUri) { 
              Ringtone ringtone = RingtoneManager.getRingtone(SettingsActivity.this, ringtoneUri);
              if (ringtone != null) {
                  mPrefs.edit().putString("default_alarm", ringtoneUri.toString()).commit();
                  mAlarmPref.setSummary(ringtone.getTitle(SettingsActivity.this));
              }
            }
        });

        mSnoozePref = (SeekBarPreference)findPreference("default_snooze");
        mSnoozePref.setValue(mPrefs.getInt("default_snooze",9));
        mSnoozePref.setSummary(mSnoozePref.getText());
        mSnoozePref.setOnPreferenceChangeListener(mSeekBarChange);

        mDurationPref = (SeekBarPreference)findPreference("default_duration");
        mDurationPref.setValue(mPrefs.getInt("default_duration",0));
        mDurationPref.setSummary(mDurationPref.getText());
        mDurationPref.setOnPreferenceChangeListener(mSeekBarChange);

        mVolumePref = (SeekBarPreference)findPreference("default_volume");
        mVolumePref.setValue(mPrefs.getInt("default_volume",100));
        mVolumePref.setSummary(mVolumePref.getText());
        mVolumePref.setOnPreferenceChangeListener(mSeekBarChange);

        mCrescendoPref = (SeekBarPreference)findPreference("default_crescendo");
        mCrescendoPref.setValue(mPrefs.getInt("default_crescendo",0));
        mCrescendoPref.setSummary(mCrescendoPref.getText());
        mCrescendoPref.setOnPreferenceChangeListener(mSeekBarChange);

        mDelayPref = (SeekBarPreference)findPreference("default_delay");
        mDelayPref.setValue(mPrefs.getInt("default_delay",0));
        mDelayPref.setSummary(mDelayPref.getText());
        mDelayPref.setOnPreferenceChangeListener(mSeekBarChange);

        mCaptchaSnoozePref = (ListPreference)findPreference("default_captcha_snooze");
        mCaptchaSnoozePref.setValue(mPrefs.getString("default_captcha_snooze","0"));
        mCaptchaSnoozePref.setSummary( mCaptchaSnoozePref.getEntry() );
        mCaptchaSnoozePref.setOnPreferenceChangeListener(mListChange);

        mCaptchaDismissPref = (ListPreference)findPreference("default_captcha_dismiss");
        mCaptchaDismissPref.setValue(mPrefs.getString("default_captcha_dismiss","0"));
        mCaptchaDismissPref.setSummary( mCaptchaDismissPref.getEntry() );
        mCaptchaDismissPref.setOnPreferenceChangeListener(mListChange);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        
        if (preference == mAlarmInSilentModePref) {
            
            int ringerModeStreamTypes = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
            
            if (mAlarmInSilentModePref.isChecked()) {
                ringerModeStreamTypes &= ~ALARM_STREAM_TYPE_BIT;
            } else {
                ringerModeStreamTypes |= ALARM_STREAM_TYPE_BIT;
            }
            
            Settings.System.putInt(getContentResolver(),
                    Settings.System.MODE_RINGER_STREAMS_AFFECTED,
                    ringerModeStreamTypes);
            
            return true;
        }
        
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void refresh() {
        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
        mAlarmInSilentModePref.setChecked(
                (silentModeStreams & ALARM_STREAM_TYPE_BIT) == 0);
    }
    
}
