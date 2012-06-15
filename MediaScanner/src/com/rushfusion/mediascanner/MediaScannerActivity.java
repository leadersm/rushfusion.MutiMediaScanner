package com.rushfusion.mediascanner;

import java.util.HashMap;

import com.rushfusion.mediascanner.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MediaScannerActivity extends Activity {
	/** Called when the activity is first created. */
	private int TEST_MEDIA_TYPE = 1;
	private int TEST_MAX_Scan_Deep = 16;

	Button stop;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		stop= (Button) findViewById(R.id.stop);
		stop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				System.out.println("================stop!!!!================");
				MultimediaFileScanner.getInstance(getApplicationContext()).stopScanning();
			}
		});
		new Thread(scanRunnable).start();
		
	}
	MultimediaFileScanner.ScanCallback callback = new MultimediaFileScanner.ScanCallback() {

		@Override
		public void onScanCompleted(boolean completed) {
			// TODO Auto-generated method stub
			System.out.println("onScanCompleted-->"+completed);
		}

		@Override
		public void onError(Exception e, int code) {
			// TODO Auto-generated method stub
			System.out.println("onError-->"+e.getMessage());
		}

		@Override
		public void onFind(HashMap<String, Object> device, String path) {
			// TODO Auto-generated method stub
			System.out.println("onFind-->"+path);
			
		}
	};
	
	Runnable scanRunnable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			MultimediaFileScanner.getInstance(getApplicationContext()).startScanning(TEST_MEDIA_TYPE, TEST_MAX_Scan_Deep,callback);
		}
	};

}