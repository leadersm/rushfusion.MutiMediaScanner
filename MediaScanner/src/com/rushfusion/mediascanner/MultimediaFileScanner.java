package com.rushfusion.mediascanner;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;

/**
 * @author rushfusion (1).�ж���ƶ��豸,���豸�����ռ��С˳��һ��һ��ɨ��;
 *         (2).ͬһ���豸��,��������������ԭ��,�Ӹ�Ŀ¼��ʼɨ���豸�ϵ������ļ���,ֱ���ﵽ����ָ�����ļ������Ϊֹ;
 *         (3).��ɨ�赽ĳһ�ļ�����������һ����ý���ļ�ʱ,����ֹͣ���ļ������ļ�ɨ��,���ø��ļ���·�����������ûص�����ָ������void
 *         onFind(String path); Ȼ�󰴲���(1),(2),(3)����ɨ����һ��Ӧ��ɨ���ļ���; (4).ȫ��ɨ����ɺ�:
 *         �������ָ������ļ�����Ȼ�û��ɨ�����,�ò���false���ûص�������һ������onScanCompleted(boolean
 *         completed);
 *         ���ɨ���������ļ���,δ����ָ������ļ������,�ò���true���ûص����󷽷�onScanCompleted(boolean
 *         completed); (5). ���ɨ���г����쳣:δ�����ƶ��豸,�ƶ��豸�γ���,�ö����쳣����Ϊ�������ûص�����void
 *         onError(Exception e,int code);
 */
public class MultimediaFileScanner {
	public static final int TYPE_IMAGE = 1;// image file
	public static final int TYPE_AUDIO = 2;// audio file
	public static final int TYPE_VIDEO = 3;// video file

	public static final int ERROR_TYPE = 101;// type error
	public static final int ERROR_MAX_SCAN_DEEP = 102;// max scan deep error
	public static final int ERROR_DEVICE_NOT_FOUND = 103;// δ�����ƶ��豸
	public static final int ERROR_DEVICE_REMOVED = 104;// �ƶ��豸�γ�

	private static final String TAG = "MultimediaFileScanner";

	private static MultimediaFileScanner MultimediaFileScanner;

	private byte[] lock = new byte[0];
	private static boolean isScanning = false;
	private LinkedList<File> mScanQueue = new LinkedList<File>();

	private static HashMap<String, Object> mImageExts = new HashMap<String, Object>();
	private static HashMap<String, Object> mAudioExts = new HashMap<String, Object>();
	private static HashMap<String, Object> mVideoExts = new HashMap<String, Object>();

	private static Context mContext;
	private static ScanCallback mCallback;

	private int maxScanDeep = 0;
	private boolean isDeepEnough = true;

	private MultimediaFileScanner() {

	}

	public static MultimediaFileScanner getInstance(Context context) {
		if (MultimediaFileScanner == null) {
			mContext = context;
			MultimediaFileScanner = new MultimediaFileScanner();

			initMediaType();
		}
		return MultimediaFileScanner;
	}

	/**
	 * �ص��ӿ�
	 * 
	 * @author lsm
	 */
	public interface ScanCallback {
		public void onFind(HashMap<String, Object> device, String path);

		public void onScanCompleted(boolean completed);

		public void onError(Exception e, int code);
	}

	/**
	 * ��ʼ��ý���ļ����� ������������ͣ� JPEG,JPG,PNG,GIF,BMP, MP3,WAV,WMA,ACC,APE,OGG,
	 * MP4,AVi,RM,RMVB,WMV,ASF,ASX,3GP,M4V,DAT��FLV,VOB,MOV
	 * ���û����Ҫ��ý���ļ�������ʹ��addMediaType(int mediaType,String extensionName)��ӡ�
	 */
	private static void initMediaType() {
		mImageExts.put("JPEG", null);
		mImageExts.put("JPG", null);
		mImageExts.put("PNG", null);
		mImageExts.put("GIF", null);
		mImageExts.put("BMP", null);
		// -----------------
		mAudioExts.put("MP3", null);
		mAudioExts.put("WAV", null);
		mAudioExts.put("WMA", null);
		mAudioExts.put("ACC", null);
		mAudioExts.put("APE", null);
		mAudioExts.put("OGG", null);
		mAudioExts.put("M4A", null);
		mAudioExts.put("AMR", null);
		mAudioExts.put("AWB", null);
		mAudioExts.put("OGA", null);
		// -----------------
		mVideoExts.put("MP4", null);
		mVideoExts.put("AVI", null);
		mVideoExts.put("RM", null);
		mVideoExts.put("RMVB", null);
		mVideoExts.put("WMV", null);
		mVideoExts.put("ASX", null);
		mVideoExts.put("3GP", null);
		mVideoExts.put("M4V", null);
		mVideoExts.put("DAT", null);
		mVideoExts.put("FLV", null);
		mVideoExts.put("VOB", null);
		mVideoExts.put("MOV", null);
		mVideoExts.put("MKV", null);
		mVideoExts.put("MPEG", null);
		mVideoExts.put("MPG", null);
		mVideoExts.put("ASF", null);
		mVideoExts.put("TS", null);
		mVideoExts.put("TP", null);
		mVideoExts.put("MTS", null);
		mVideoExts.put("F4V", null);
	}

	/**
	 * 
	 * @param mediaType
	 *            : TYPE_IMAGE 1;// image file TYPE_AUDIO 2;// audio file
	 *            TYPE_VIDEO 3;// video file
	 * @param extensionName
	 *            : ��:"JPG"
	 * @throws Exception
	 *             ��չ����ʽ�쳣
	 */
	public void addMediaType(int mediaType, String extensionName)
			throws Exception {

		extensionName = extensionName.toUpperCase();
		switch (mediaType) {
		case TYPE_IMAGE:
			mImageExts.put(extensionName, null);
			break;
		case TYPE_AUDIO:
			mAudioExts.put(extensionName, null);
			break;
		case TYPE_VIDEO:
			mVideoExts.put(extensionName, null);
			break;
		default:
			break;
		}
	}

	/**
	 * 
	 * @param mediaType
	 *            ý������:1:ͼƬ,2:��Ƶ,3:��Ƶ;
	 * @param maxScanDeep
	 *            ����ļ������
	 * @param callback
	 *            һ���ص�����
	 */
	public void startScanning(final int mediaType, final int maxScanDeep,
			final ScanCallback callback) {
		if (callback == null || maxScanDeep <= 0 || mediaType < TYPE_IMAGE
				|| mediaType > TYPE_VIDEO)
			return;
//		Thread thread = new Thread(new Runnable() {
//			public void run() {
//				
//			}
//		});
//
//		thread.setPriority(Thread.MIN_PRIORITY);
//		thread.start();
		
		synchronized (lock) {
			isScanning = true;
		}
		ArrayList<HashMap<String, Object>> storages = getRemovableStorageDevices(callback);
		Collections.sort(storages,
				new Comparator<HashMap<String, Object>>() {
					@Override
					public int compare(HashMap<String, Object> map0,
							HashMap<String, Object> map1) {
						File dir0 = (File) map0.get("file");
						File dir1 = (File) map1.get("file");
						StatFs sf0 = new StatFs(dir0.getPath());
						StatFs sf1 = new StatFs(dir1.getPath());
						long usedSize0 = sf0.getBlockCount()
								- sf0.getAvailableBlocks();
						long usedSize1 = sf1.getBlockCount()
								- sf1.getAvailableBlocks();
						return (int) (usedSize0 - usedSize1);
					}
				});

		synchronized (lock) {
			if (!isScanning)
				return;
		}

		MultimediaFileScanner.this.maxScanDeep = maxScanDeep;

		mCallback = callback;
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addDataScheme("file");
		mContext.registerReceiver(mUnmountReceiver, intentFilter);

		if (mediaType == TYPE_IMAGE) {
			scanTheStorages(storages, mImageExts);
		} else if (mediaType == TYPE_AUDIO) {
			scanTheStorages(storages, mAudioExts);
		} else if (mediaType == TYPE_VIDEO) {
			scanTheStorages(storages, mVideoExts);
		} else {// δ֪type
			callback.onError(new Exception("δ֪���������쳣 mediaType-->"
					+ mediaType), ERROR_TYPE);
		}
		mContext.unregisterReceiver(mUnmountReceiver);
		synchronized (lock) {
			isScanning = false;
		}
	}

	private boolean checkFile(HashMap<String, Object> device, File file,
			HashMap<String, Object> mediaTypes) {
		String name = file.getName();
		int i = name.lastIndexOf('.');
		if (i != -1) {
			name = name.substring(i + 1).toUpperCase();
			if (mediaTypes.containsKey(name)) {
				synchronized (lock) {
					if (!isScanning)
						return false;
					mCallback.onFind(device, file.getParent());
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param storages
	 * @param currentDeep
	 * @param dirback
	 * @param mediaTypes
	 * @return
	 */
	private void scanTheStorages(ArrayList<HashMap<String, Object>> storages,
			HashMap<String, Object> mediaTypes) {
		if (storages == null || storages.size() == 0)
			return;

		synchronized (lock) {
			if (!isScanning)
				return;
		}
		Log.i(TAG, "scanTheStoragesByOrder: storages.length=" + storages.size());
		// scanning the storage by order
		for (HashMap<String, Object> device : storages) {
			File dir = (File) device.get("file");
			Log.i(TAG, "scanTheStoragesByOrder: device=" + dir.getPath());
			synchronized (lock) {
				if (!isScanning)
					return;
			}

			// Storage is a Directory
			if (dir.isDirectory()) {
				scanTheStorage(device, dir, mediaTypes);
			}
		}

		synchronized (lock) {
			if (isScanning)
				mCallback.onScanCompleted(isDeepEnough);
		}
	}

	private void scanTheStorage(HashMap<String, Object> device, File dir,
			HashMap<String, Object> mediaTypes) {
		synchronized (lock) {
			if (!isScanning)
				return;
		}

		if (dir.listFiles() == null)
			return;

		mScanQueue.clear();

		mScanQueue.addLast(dir);

		int currentDeep = 0;
		int currentLevelDirCount = 1;
		int nextLevelDirCount = 0;
		int countScanned = 0;
		boolean founddMediaFile = false;

		while (mScanQueue.size() > 0) {
			synchronized (lock) {
				if (!isScanning)
					return;
			}
			File newDir = mScanQueue.removeFirst();
			if (newDir != null) {
				countScanned++;
				File dirs[] = newDir.listFiles(new FileFilter() {

					@Override
					public boolean accept(File file) {
						// TODO Auto-generated method stub
						return file.isDirectory();
					}

				});
				if (dirs != null)
					nextLevelDirCount += dirs.length;
				// Log.i(TAG,"scanTheStorage(): currentDeep="+currentDeep+" currentLevelDirCount="+currentLevelDirCount
				// +" nextLevelDirCount+"+nextLevelDirCount+" countScanned="+countScanned);
				if (countScanned == currentLevelDirCount) {// ɨ����һ��
					currentDeep++;
					// Log.i(TAG,"scanTheStorage(): scan over level: "+currentDeep);
					if (currentDeep >= maxScanDeep) {
						isDeepEnough = false;// add
						return;
					}
					countScanned = 0;
					currentLevelDirCount = nextLevelDirCount;
					nextLevelDirCount = 0;
				}

				founddMediaFile = false;
				for (File file : newDir.listFiles()) {
					synchronized (lock) {
						if (!isScanning)
							return;
					}
					if (!founddMediaFile && file.isFile()
							&& checkFile(device, file, mediaTypes)) {
						founddMediaFile = true; // Find the first media file in
												// current Directory
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (currentDeep < maxScanDeep && file.isDirectory()
							&& file.listFiles() != null) {
						mScanQueue.addLast(file);
					}
				}
			}
		}
	}

	public void stopScanning() {
		synchronized (lock) {
			if (isScanning) {
				// 1��ֹͣɨ��
				isScanning = false;
				// 2������callback false
				if (mCallback != null)
					mCallback.onScanCompleted(false);
				if (mUnmountReceiver.isOrderedBroadcast()) {
					mContext.unregisterReceiver(mUnmountReceiver);
				}
			}
		}
	}

	/*
	 * �Ƴ��ƶ��洢�豸�㲥����-->ֹͣ����?
	 */
	private static BroadcastReceiver mUnmountReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mCallback.onError(new Exception("�豸�Ƴ�����������ֹͣ!"),
					ERROR_DEVICE_REMOVED);
			MultimediaFileScanner.stopScanning();
			Log.w(TAG, "----->�豸�Ƴ�����������ֹͣ!<-----");
		}
	};

	/**
	 * ��ȡsd����Ϣ �ж���ƶ��豸,���豸�����ռ��С˳��һ��һ��ɨ��;
	 * 
	 * @param callback
	 */
	public ArrayList<HashMap<String, Object>> getRemovableStorageDevices(
			ScanCallback callback) {
		Log.d(TAG,
				"ExternalStorageState-->"
						+ Environment.getExternalStorageState()
						+ "   ExternalStorageDirectory-->"
						+ Environment.getExternalStorageDirectory());
		File root = new File("/mnt");
		if (root.exists() && root.isDirectory()) {
			return getExternStorages(root);
		} else if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				&& Environment.getExternalStorageDirectory().exists()) {
			root = Environment.getExternalStorageDirectory();
			return getExternStorages(root);
		} else {
			if (callback != null)
				callback.onError(
						new Exception("�豸δ�ҵ�����state-->"
								+ Environment.getExternalStorageState()),
						ERROR_DEVICE_NOT_FOUND);
			Log.w(TAG, "δ�ҵ��ⲿ�洢�豸����");
		}

		return null;
	}

	private ArrayList<HashMap<String, Object>> getExternStorages(File root) {
		if (root == null)
			return null;
		File[] files = root.listFiles();
		if (files == null || files.length == 0)
			return null;
		char sequence = 'A';
		ArrayList<HashMap<String, Object>> storages = new ArrayList<HashMap<String, Object>>();
		for (File dir : files) {
			if (dir.isDirectory()) {
				File[] dirs = dir.listFiles();
				if (dirs != null && dirs.length > 0) {
					for (File storage : dirs) {
						if (storage.isDirectory()) {
							StatFs sf = new StatFs(storage.getPath());

							long totalSize = (sf.getBlockSize() / 1024 * sf
									.getBlockCount()) / 1024;
							long availSize = (sf.getBlockSize() / 1024 * sf
									.getAvailableBlocks()) / 1024;
							Log.i(TAG, "�����豸--->" + storage.getName()
									+ "<---,�ܴ�С:" + formatSize(totalSize)
									+ ",ʣ��ռ�:" + formatSize(availSize));

							HashMap<String, Object> map = new HashMap<String, Object>();
							map.put("title", String.valueOf(sequence) + ":");
							map.put("file", storage);
							map.put("path", storage.getAbsolutePath());
							map.put("type", "device");
							map.put("totalSize", totalSize);// MB
							map.put("availSize", availSize);// MB
							storages.add(map);
							sequence++;
						}
					}
				}
			}
		}

		return storages;
	}

	private String formatSize(long size) {
		return Formatter.formatFileSize(mContext, size);
	}

	/**
	 * 
	 * @param mediaType
	 *            the media type
	 * @param path
	 *            the directory's path
	 * @param callBack
	 */
	public void scanTheDirectory(final HashMap<String, Object> device,
			final String path, final int mediaType, final ScanCallback callback) {
		if (callback == null || path == null || path.equals("")
				|| mediaType < TYPE_IMAGE || mediaType > TYPE_VIDEO)
			return;
		new Thread(new Runnable() {
			public void run() {
				synchronized (lock) {
					isScanning = true;
				}

				File directory = new File(path);
				if (directory.listFiles() == null)
					return;

				if (mediaType == TYPE_IMAGE) {
					for (File f : directory.listFiles()) {
						synchronized (lock) {
							if (!isScanning)
								return;
						}

						if (f.isFile())
							checkFileType(device, f, mImageExts, callback);
					}
				} else if (mediaType == TYPE_AUDIO) {
					for (File f : directory.listFiles()) {
						synchronized (lock) {
							if (!isScanning)
								return;
						}

						if (f.isFile())
							checkFileType(device, f, mAudioExts, callback);
					}
				} else if (mediaType == TYPE_VIDEO) {
					for (File f : directory.listFiles()) {
						synchronized (lock) {
							if (!isScanning)
								return;
						}
						if (f.isFile())
							checkFileType(device, f, mVideoExts, callback);
					}
				} else {
					callback.onError(new Exception("δ֪���������쳣 mediaType-->"
							+ mediaType), ERROR_TYPE);
				}

				synchronized (lock) {
					if (isScanning)// ---
						callback.onScanCompleted(true);
				}
			}
		}).start();
	}

	private void checkFileType(HashMap<String, Object> device, File file,
			HashMap<String, Object> mediaTypes, ScanCallback cb) {
		String name = file.getName();
		int i = name.lastIndexOf('.');
		if (i != -1) {
			name = name.substring(i + 1).toUpperCase();
			if (mediaTypes.containsKey(name)) {
				synchronized (lock) {
					if (!isScanning)
						return;
					cb.onFind(device, file.getAbsolutePath());
				}
				// Log.d(TAG, "find file in the dir-->" +
				// file.getAbsolutePath());
			}
		}
	}

}
