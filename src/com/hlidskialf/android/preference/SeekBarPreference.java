/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.hlidskialf.android.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.preference.DialogPreference;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;


public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
  private static final String androidns="http://schemas.android.com/apk/res/android";

  private SeekBar mSeekBar;
  private TextView mSplashText,mValueText;
  private Context mContext;

  private String mDialogMessage, mSuffix, mZeroText;
  private int mDefault, mMax, mValue = 0;
  private String[] mEntries;

  public SeekBarPreference(Context context, AttributeSet attrs) { 
    super(context,attrs); 
    mContext = context;

    int string_id;

    if ((string_id = attrs.getAttributeResourceValue(androidns,"dialogMessage",0)) != 0) 
      mDialogMessage = context.getString(string_id);
    else
      mDialogMessage = attrs.getAttributeValue(androidns,"dialogMessage");

    if ((string_id = attrs.getAttributeResourceValue(androidns,"text",0)) != 0)
      mSuffix = context.getString(string_id);
    else
      mSuffix = attrs.getAttributeValue(androidns,"text");

    if ((string_id = attrs.getAttributeResourceValue(androidns,"hint",0)) != 0)
      mZeroText = context.getString(string_id);
    else
      mZeroText = attrs.getAttributeValue(androidns,"hint");

    mDefault = attrs.getAttributeIntValue(androidns,"defaultValue", 0);

    int array_id = attrs.getAttributeResourceValue(androidns,"entries",0);
    if (array_id != 0) {
      mEntries = context.getResources().getStringArray(array_id);
      mMax = mEntries.length-1;
    }
    else {
      mMax = attrs.getAttributeIntValue(androidns,"max", 100);
    }

    if (shouldPersist())
      setValue( getPersistedInt(mDefault) );
    else
      setValue( mDefault );
    
  }
  @Override 
  protected View onCreateDialogView() {
    LinearLayout.LayoutParams params;
    LinearLayout layout = new LinearLayout(mContext);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(6,6,6,6);

    mSplashText = new TextView(mContext);
    if (mDialogMessage != null)
      mSplashText.setText(mDialogMessage);
    layout.addView(mSplashText);

    mValueText = new TextView(mContext);
    mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
    mValueText.setTextSize(32);
    params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.FILL_PARENT, 
        LinearLayout.LayoutParams.WRAP_CONTENT);
    layout.addView(mValueText, params);

    mSeekBar = new SeekBar(mContext);
    mSeekBar.setOnSeekBarChangeListener(this);
    layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    mSeekBar.setMax(mMax);
    return layout;
  }
  @Override 
  protected void onBindDialogView(View v) {
    super.onBindDialogView(v);
    setValue(mValue);
  }
  @Override
  protected void onSetInitialValue(boolean restore, Object defaultValue)  
  {
    super.onSetInitialValue(restore, defaultValue);
    int value;
    if (restore) 
      value = shouldPersist() ? getPersistedInt(mDefault) : 0;
    else 
      value = (Integer)defaultValue;
    setValue(value);
  }

  public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
  {
    mValue = value;

    mValueText.setText(getText());

    if (shouldPersist())
      persistInt(getValue());

    callChangeListener(getValue());
  }
  public void onStartTrackingTouch(SeekBar seek) {}
  public void onStopTrackingTouch(SeekBar seek) {}

  public void setMax(int max) { mMax = max; }
  public int getMax() { return mMax; }

  public String getText()
  {
    int value = getValue();
    if (value == 0 && mZeroText != null)  {
      return mZeroText;
    }
    String t = String.valueOf( value );
    return mSuffix == null ? t : t+" "+mSuffix;
  }

  public void setValue(int value)
  {
    if (mEntries != null) {
      mValue = 0;
      int i;
      for (i=0; i < mEntries.length; i++) {
        if (mEntries[i].equals( String.valueOf(value) )) {
          mValue = i;
          break;
        }
      }
    }
    else {
      mValue = value;
    }
    if (mSeekBar != null)
      mSeekBar.setProgress(mValue);
    if (mValueText != null)
      mValueText.setText(getText());
  }
  public int getValue()
  {
    if (mEntries != null) {
      return Integer.valueOf( mEntries[mValue] );
    }
    else {
      return mValue;
    }
  }
}


