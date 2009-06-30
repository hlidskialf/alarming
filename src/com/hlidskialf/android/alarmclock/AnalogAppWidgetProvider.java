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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Config;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Arrays;

/**
 * Simple widget to show analog clock.
 */
public class AnalogAppWidgetProvider extends BroadcastReceiver {
    static final String TAG = "AnalogAppWidgetProvider";

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            SharedPreferences prefs = context.getSharedPreferences(AlarmClock.PREFERENCES, 0);
            int face = prefs.getInt(AlarmClock.PREF_WIDGET_CLOCK_FACE, 0);
            if (face < 0 || face > AnalogAppWidgetConfigure.CLOCKS.length) face = 0;
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    AnalogAppWidgetConfigure.CLOCKS[face]);


            Intent config = new Intent(context, AnalogAppWidgetConfigure.class);
            config.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            PendingIntent configureIntent = PendingIntent.getActivity(context, 0, config, 0);
            views.setOnClickPendingIntent(R.id.clock, configureIntent);
            
            int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            
            AppWidgetManager gm = AppWidgetManager.getInstance(context);
            gm.updateAppWidget(appWidgetIds, views);
        }
    }
}

