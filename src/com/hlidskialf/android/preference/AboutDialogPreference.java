/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.hlidskialf.android.preference;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.ScrollView;

public class AboutDialogPreference extends DialogPreference 
{
  private Context mContext;
  private String mSummary;
  private PackageInfo mPackageInfo;
  
  public AboutDialogPreference(Context context, AttributeSet attrs) 
  {
    super(context,attrs);

    mContext = context;

    int string_id;
    if ((string_id = attrs.getAttributeResourceValue(androidns,"dialogMessage",0)) != 0) 
      mSummary = context.getString(string_id);
    else
      mSummary = attrs.getAttributeValue(androidns,"dialogMessage");

    try {
      ComponentName comp = new ComponentName(mContext, AboutDialogPreference.class);
      mPackageInfo = mContext.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
    } catch (android.content.pm.PackageManager.NameNotFoundException e) {
    }
  }

  @Override 
  protected View onCreateDialogView() {
    LinearLayout.LayoutParams params;
    LinearLayout layout = new LinearLayout(mContext);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(6,6,6,6);
    layout.setGravity(Gravity.CENTER);

    TextView mTitleText = new TextView(mContext);
    if (mPackageInfo != null && mPackageInfo.applicationInfo != null && mPackageInfo.applicationInfo.name != null) 
      mTitleText.setText(mPackageInfo.applicationInfo.name);
    mTitleText.setTextSize(14);
    layout.addView(mTitleText);

    TextView mVersionText = new TextView(mContext);
    mVersionText.setText("v"+mPackageInfo.versionName);
    layout.addView(mVersionText);

    TextView mAboutText = new TextView(mContext);
    mAboutText.setText(mSummary);
    mAboutText.setGravity(Gravity.CENTER);
    mAboutText.setTextSize(14);
    ScrollView mScrollView = new ScrollView(mContext);
    mScrollView.addView(mAboutText);
    layout.addView(mScrollView);
    
    return layout;
  }


  private static final String androidns="http://schemas.android.com/apk/res/android";
}
