package com.hlidskialf.android.alarmclock;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Random;

import com.hlidskialf.android.widget.NumberPicker;

public class MathCaptcha extends Dialog implements CaptchaInterface
{
  private int mAnswer;
  private NumberPicker mAnswerHundreds, mAnswerTens, mAnswerOnes;
  CaptchaInterface.OnCorrectListener mCorrectListener;

  public void setOnCorrectListener(CaptchaInterface.OnCorrectListener listener) {
    mCorrectListener = listener;
  }

  public MathCaptcha(Context context) { super(context); }

  public void onCreate(Bundle icicle) {
    View layout = getLayoutInflater().inflate(R.layout.math_captcha, null);

    Random rand = new Random();
    int first = rand.nextInt(99);
    int second = rand.nextInt(99);

    TextView tv;
    if (first < second) {
      int temp = first;
      first = second;
      second = temp;
    }

    tv = (TextView)layout.findViewById(R.id.question_sign);
    if (rand.nextBoolean()) {
      tv.setText("+");
      mAnswer = first + second;
    }
    else {
      tv.setText("-");
      mAnswer = first - second;
    }

    tv = (TextView)layout.findViewById(R.id.question_1_tens);
    tv.setText( String.valueOf( (first / 10) % 10 ) );
    tv = (TextView)layout.findViewById(R.id.question_1_ones);
    tv.setText( String.valueOf( first % 10 ) );

    tv = (TextView)layout.findViewById(R.id.question_2_tens);
    tv.setText( String.valueOf( (second / 10) % 10 ) );
    tv = (TextView)layout.findViewById(R.id.question_2_ones);
    tv.setText( String.valueOf( second % 10 ) );

    Button b;

    b = (Button)layout.findViewById(android.R.id.button1);
    b.setOnClickListener(new Button.OnClickListener() {
      public void onClick(View v) {
        if (check_answer()) {
          if (mCorrectListener != null) 
            mCorrectListener.onCorrect();
          dismiss();
        } else {
          mAnswerHundreds.setCurrent(0);
          mAnswerTens.setCurrent(0);
          mAnswerOnes.setCurrent(0);
        }
      }
    });
    b = (Button)layout.findViewById(android.R.id.button2);
    b.setOnClickListener(new Button.OnClickListener() {
      public void onClick(View v) {
        dismiss();
      }
    });

    mAnswerHundreds = (NumberPicker)layout.findViewById(R.id.answer_hundreds);
    mAnswerTens = (NumberPicker)layout.findViewById(R.id.answer_tens);
    mAnswerOnes = (NumberPicker)layout.findViewById(R.id.answer_ones);
    
    setContentView(layout);
  }

  private int get_answer()
  {
    return mAnswerHundreds.getCurrent()*100 
      + mAnswerTens.getCurrent()*10 
      + mAnswerOnes.getCurrent();
  }

  private boolean check_answer()
  {
    return get_answer() == mAnswer;
  }
}
