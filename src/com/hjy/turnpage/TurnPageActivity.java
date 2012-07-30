package com.hjy.turnpage;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

public class TurnPageActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TurnPageView pageView = new TurnPageView(this);
        setContentView(pageView);
        
        Bitmap currPageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
		Bitmap nextPageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg2);
		pageView.setBitmaps(currPageBitmap, nextPageBitmap);
    }
}