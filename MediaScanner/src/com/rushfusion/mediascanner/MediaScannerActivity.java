package com.rushfusion.mediascanner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MediaScannerActivity extends Activity {
	/** Called when the activity is first created. */
	private static final String TAG = "MediaScanner";
	private int MEDIA_TYPE = MultimediaFileScanner.TYPE_IMAGE;
	private int MAX_DEEP = 10;
	ViewGroup loading;
	boolean isLoading = false;
	Button stop, start;
	ListView lv ;
	List<File> dirs = new ArrayList<File>();
	List<File> files = new ArrayList<File>();
	LayoutInflater inflater;
	FileAdapter fa;
	
	private static final int UPDATE_DATA = 0;
	
	Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
			case UPDATE_DATA:
				fa.notifyDataSetChanged();
				break;
			default:
				break;
			}
			
		}
		
	};
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		inflater = LayoutInflater.from(this);
		loading = (ViewGroup) findViewById(R.id.loading);
		stop = (Button) findViewById(R.id.stop);
		start = (Button) findViewById(R.id.start);
		lv = (ListView) findViewById(R.id.listView1);
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				MultimediaFileScanner.getInstance(getApplicationContext())
				.scanTheDirectory(null, dirs.get(position).getPath(), MEDIA_TYPE, fileback);
			}
		});
		fa = new FileAdapter();
		lv.setAdapter(fa);
		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(isLoading)return;
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected void onPreExecute() {
						// TODO Auto-generated method stub
						super.onPreExecute();
						isLoading = true;
						loading.setVisibility(View.VISIBLE);
					}

					@Override
					protected void onPostExecute(Void result) {
						// TODO Auto-generated method stub
						super.onPostExecute(result);
						isLoading = false;
						loading.setVisibility(View.GONE);
					}

					@Override
					protected Void doInBackground(Void... params) {
						// TODO Auto-generated method stub
						MultimediaFileScanner.getInstance(
								getApplicationContext()).startScanning(
								MEDIA_TYPE, MAX_DEEP, dirback);
						return null;
					}
				}.execute();
			}
		});
		stop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				MultimediaFileScanner.getInstance(getApplicationContext())
						.stopScanning();
			}
		});
	}

	MultimediaFileScanner.ScanCallback dirback = new MultimediaFileScanner.ScanCallback() {

		@Override
		public void onScanCompleted(boolean completed) {
			// TODO Auto-generated method stub
			Log.v(TAG, "onScanCompleted-->" + completed);
			handler.sendEmptyMessage(UPDATE_DATA);
		}

		@Override
		public void onError(Exception e, int code) {
			// TODO Auto-generated method stub
			Log.v(TAG, "onError-->" + e.getMessage());
		}

		@Override
		public void onFind(HashMap<String, Object> device, String path) {
			// TODO Auto-generated method stub
			Log.v(TAG, "onFind dir-->" + path);
			dirs.add(new File(path));
		}
	};

	MultimediaFileScanner.ScanCallback fileback = new MultimediaFileScanner.ScanCallback() {

		@Override
		public void onScanCompleted(boolean completed) {
			// TODO Auto-generated method stub
			Log.i(TAG, "onScan current dir Completed-->" + completed);
		}

		@Override
		public void onError(Exception e, int code) {
			// TODO Auto-generated method stub
			Log.i(TAG, "onError-->" + e.getMessage());
		}

		@Override
		public void onFind(HashMap<String, Object> device, String path) {
			// TODO Auto-generated method stub
			Log.i(TAG, "onFind MediaFile-->" + path);
			files.add(new File(path));
		}
	};
	
	
	class FileAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return dirs.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			View view = inflater.inflate(R.layout.item, null);
			TextView name = (TextView) view.findViewById(R.id.fileName);
			TextView path = (TextView) view.findViewById(R.id.filePath);
			name.setText(dirs.get(position).getName());
			path.setText(dirs.get(position).getPath());
			return view;
		}
		
	}
	
	
	
}