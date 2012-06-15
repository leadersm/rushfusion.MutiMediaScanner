package com.rushfusion.mediascanner;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;

/**
 * @author rushfusion 
 * (1).有多个移动设备,按设备容量空间大小顺序一个一个扫描;
 * (2).同一个设备中,按多叉树广度优先原则,从根目录开始扫描设备上的所有文件夹,直到达到参数指定的文件夹深度为止;
 * (3).当扫描到某一文件夹下有至少一个多媒体文件时,立即停止本文件夹内文件扫描,并用该文件夹路径做参数调用回调对象指定方法void
 * 		onFind(String path); 然后按步骤(1),(2),(3)策略扫描下一个应该扫描文件夹; 
 * (4).全部扫描完成后:
 * 		如果超过指定最大文件夹深度还没有扫描完成,用参数false调用回调对象另一个方法onScanCompleted(boolean completed);
 * 		如果扫描完所有文件夹,未超过指定最大文件夹深度,用参数true调用回调对象方法onScanCompleted(boolean completed); 
 * (5). 如果扫描中出现异常:未发现移动设备,移动设备拔出等,用对象异常对象为参数调用回调对象void onError(Exception e,int code);
 */
public class Scanner {

	private static final int TYPE_IMAGE = 1;// image file
	private static final int TYPE_AUDIO = 2;// audio file
	private static final int TYPE_VIDEO = 3;// video file

	public static final int ERROR_TYPE = 101;// type error
	public static final int ERROR_MAX_SCAN_DEEP = 102;// max scan deep error
	public static final int ERROR_DEVICE_NOT_FOUND = 103;// 未发现移动设备
	public static final int ERROR_DEVICE_REMOVED = 104;// 移动设备拔出

	private static final String TAG = "Scanner";

	private static Scanner scanner;

	private static List<File> sdcards;
	private static LinkedList<String> Images = null;
	private static LinkedList<String> Audios = null;
	private static LinkedList<String> Videos = null;

	private static Context ctx;
	private static ScanCallback callback;
	
	
	private int currentDeep = 0;
	private int maxScanDeep = 0;
	private static boolean isScanning = false;
	private boolean isDeepEnough = true;
	
	private Scanner() {

	}

	public static Scanner getInstance(Context context) {
		if (scanner == null) {
			ctx = context;
			scanner = new Scanner();
			sdcards = new ArrayList<File>();
			initMediaType();
		}
		return scanner;
	}

	/**
	 * 回调接口
	 * @author lsm
	 */
	public interface ScanCallback {
		public void onFind(String path);

		public void onScanCompleted(boolean completed);

		public void onError(Exception e, int code);
	}
	
	/**
	 * 初始化媒体文件类型
	 * 已填加以下类型：
	 * JPEG,JPG,PNG,GIF,BMP,
	 * MP3,WAV,WMA,ACC,APE,OGG,
	 * MP4,AVi,RM,RMVB,WMV,ASF,ASX,3GP,M4V,DAT，FLV,VOB,MOV
	 * 如果没有您要的媒体文件类型请使用addMediaType(int mediaType,String extensionName)添加。
	 */
	private static void initMediaType() {
		Images = new LinkedList<String>();
		Audios = new LinkedList<String>();
		Videos = new LinkedList<String>();
		Images.add(".JPEG");
		Images.add(".JPG");
		Images.add(".PNG");
		Images.add(".GIF");
		Images.add(".BMP");
		// -----------------
		Audios.add(".MP3");
		Audios.add(".WAV");
		Audios.add(".WMA");
		Audios.add(".ACC");
		Audios.add(".APE");
		Audios.add(".OGG");
		// -----------------
		Videos.add(".MP4");
		Videos.add(".AVI");
		Videos.add(".RM");
		Videos.add(".RMVB");
		Videos.add(".WMV");
		Videos.add(".ASF");
		Videos.add(".ASX");
		Videos.add(".3GP");
		Videos.add(".M4V");
		Videos.add(".DAT");
		Videos.add(".FLV");
		Videos.add(".VOB");
		Videos.add(".MOV");
	}
	
	/**
	 * 
	 * @param mediaType:    
	 * 			TYPE_IMAGE  1;// image file  
	 * 			TYPE_AUDIO	2;// audio file
	 * 			TYPE_VIDEO	3;// video file
	 * @param extensionName:
	 * 			例:".JPG",必须带 "."				  
	 * @throws Exception 扩展名格式异常
	 */
	public void addMediaType(int mediaType,String extensionName) throws Exception{
		if(!extensionName.contains(".")){
			throw new Exception("扩展名必须包含 '.'");
		}
		extensionName = extensionName.substring(extensionName.lastIndexOf('.')).toUpperCase();
		switch (mediaType) {
		case TYPE_IMAGE:
			Images.add(extensionName);
			break;
		case TYPE_AUDIO:
			Audios.add(extensionName);
			break;
		case TYPE_VIDEO:
			Videos.add(extensionName);
			break;
		default:
			break;
		}
	}
	
	
	/**
	 * 
	 * @param mediaType 媒体类型:1:图片,2:音频,3:视频;
	 * @param maxScanDeep 最大文件夹深度
	 * @param callback 一个回调对象
	 */
	public void scan(int mediaType, int maxScanDeep, ScanCallback callback) {
		isScanning = true;
		readSDCard(callback);
		currentDeep = 0;
		this.maxScanDeep = maxScanDeep;
		Scanner.callback = callback;
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addDataScheme("file");
		ctx.registerReceiver(mUnmountReceiver, intentFilter);
		
		File[] files = reorderArray(sdcards);
		if (mediaType == TYPE_IMAGE) {
			scanByMediaType(files, callback, Images);
		} else if (mediaType == TYPE_AUDIO) {
			scanByMediaType(files, callback, Audios);
		} else if (mediaType == TYPE_VIDEO) {
			scanByMediaType(files, callback, Videos);
		} else {// 未知type
			callback.onError(new Exception("未知搜索类型异常 mediaType-->" + mediaType),ERROR_TYPE);
		}
		ctx.unregisterReceiver(mUnmountReceiver);
		isScanning = false;
	}

	/**
	 * 
	 * @param dirs
	 * @param currentDeep
	 * @param callback
	 * @param mediaTypes
	 * @return
	 */
	private void scanByMediaType(File[] dirs, ScanCallback callback,LinkedList<String> mediaTypes) {
		for (File f : dirs) {
			if(isScanning){
				if(f.isFile()){
					if(checkFile(f, mediaTypes, callback))
						break;
				}else{
					scanDirectory(f,mediaTypes,callback);
				}
			}else
				return;
		}
		if(isScanning)
		callback.onScanCompleted(isDeepEnough);
	}
	
	private boolean checkFile(File f,LinkedList<String> mediaTypes, ScanCallback cb){
		String name = f.getName();
		int i = name.lastIndexOf('.');
		if(i != -1){
			name = name.substring(i).toUpperCase();
			if (mediaTypes.contains(name)) {
				cb.onFind(f.getParent());
//				Log.d(TAG, "find out-->"+f.getAbsolutePath());
				return true;
			}
		}
		return false;
	}
	
	private void scanDirectory(File f,LinkedList<String> mediaTypes, ScanCallback cb){
		if(!isScanning)
			return;
		if(f.listFiles()==null)
			return;
		currentDeep++;
		for(File file : f.listFiles()){
			if(isScanning){
				if (currentDeep <= maxScanDeep) {//小于扫描深度
					if(file.isFile()){
						if(checkFile(file, mediaTypes, cb))
							break;
					}else{
						scanDirectory(file, mediaTypes, cb);
					}
				} else {//超过扫描深度
					isDeepEnough = false;
				}
			}else
				return;
		}
		currentDeep-- ;
	}
	
	
	public void stopScanning(){
		if(isScanning){
			//1、停止扫描
			isScanning = false;
			//2、返回callback false
			callback.onScanCompleted(false);
			if(mUnmountReceiver.isOrderedBroadcast()){
				ctx.unregisterReceiver(mUnmountReceiver);
			}
		}
	}
	/*
	 * 移除移动存储设备广播接收-->停止搜索?
	 */
	private static BroadcastReceiver mUnmountReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			callback.onError(new Exception("设备移除，搜索被迫停止!"),Scanner.ERROR_DEVICE_REMOVED);
			scanner.stopScanning();
			Log.w(TAG, "----->设备移除，搜索被迫停止!<-----");
		}
	};

	
	
	/**
	 * 读取sd卡信息 有多个移动设备,按设备容量空间大小顺序一个一个扫描;
	 * 
	 * @param callback
	 */
	private void readSDCard(ScanCallback callback) {
		Log.d(TAG,"ExternalStorageState-->"+ Environment.getExternalStorageState()+
				"   ExternalStorageDirectory-->"+ Environment.getExternalStorageDirectory());
		File root = new File("/mnt");
		if(root.exists() && root.isDirectory()){
			checkExternStorage(root);
		}else if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())&&Environment.getExternalStorageDirectory().exists()){
			root = Environment.getExternalStorageDirectory();
			checkExternStorage(root);
		}else{
			callback.onError(new Exception("设备未找到！！state-->"+Environment.getExternalStorageState()),ERROR_DEVICE_NOT_FOUND);
			Log.w(TAG, "未找到外部存储设备！！");
		}
	}

	private void checkExternStorage(File root) {
		if(root.getAbsolutePath().equals("/sdcard")){//针对iTv盒子
			StatFs sf = new StatFs(root.getPath());
			long totalSize = sf.getBlockSize() * sf.getBlockCount();
			long availSize = sf.getBlockSize() * sf.getAvailableBlocks();
			Log.d(TAG, "发现设备--->"+ root.getPath()+ "<---,总大小:" + formatSize(totalSize) + ",剩余空间:"+ formatSize(availSize));
			sdcards.add(root);
		}else{
			File[] files = root.listFiles();
			for (File f : files) {
				if (f.isDirectory()) {//针对enjoy盒子
					String path = f.getAbsolutePath();
					if (
						path.equals("/mnt/flash")|| 
						path.equals("/mnt/sata")|| 
						path.equals("/mnt/sdcard")||
						path.equals("/mnt/usb")||
						path.startsWith("/mnt/sd")) 
					{
						StatFs sf = new StatFs(f.getPath());
						long totalSize = sf.getBlockSize() * sf.getBlockCount();
						long availSize = sf.getBlockSize() * sf.getAvailableBlocks();
						Log.d(TAG, "发现设备--->"+ path+ "<---,总大小:" + formatSize(totalSize) + ",剩余空间:"+ formatSize(availSize));
						sdcards.add(f);
					}
				}
			}
		}
		
	}
	private String formatSize(long size) {     
		return Formatter.formatFileSize(ctx, size);
    }
	

	private File[] reorderArray(List<File> sds) {
		File[] files = new File[sds.size()];
		for (int i = 0; i < sds.size(); i++) {
			files[i] = sds.get(i);
		}
		for (int i = 0; i < files.length; i++) {
			for (int j = i + 1; j < files.length; j++) {
				StatFs sfi = new StatFs(files[i].getPath());
				StatFs sfj = new StatFs(files[j].getPath());
				long total_i = sfi.getBlockSize() * sfi.getBlockCount() / 1024;
				long total_j = sfj.getBlockSize() * sfj.getBlockCount() / 1024;

				if (total_i > total_j) {
					File temp = files[i];
					files[i] = files[j];
					files[j] = temp;
				}
			}
		}
		return files;
	}
	
	
	/**
	 * 
	 * @param mediaType the media type
	 * @param path  the directory's path
	 * @param callBack
	 */
	public void scanTheDirectory(int mediaType,String path,ScanCallback cb){
		isScanning = true;
		File directory = new File(path);
		if(directory.listFiles()==null)
			return;
		for (File f : directory.listFiles()) {
			if(isScanning){
				if (f.isDirectory()) {
					scanTheDirectory(mediaType, f.getAbsolutePath(), cb);
				}else{
					if (mediaType == TYPE_IMAGE) {
						scanTheFileType(f,Images,cb);
					} else if (mediaType == TYPE_AUDIO) {
						scanTheFileType(f,Audios,cb);
					} else if (mediaType == TYPE_VIDEO) {
						scanTheFileType(f,Videos,cb);
					} else {
						cb.onError(new Exception("未知搜索类型异常 mediaType-->" + mediaType),ERROR_TYPE);
					}
				}
			}else
				return;
		}
		if(isScanning)//---
		cb.onScanCompleted(true);
	}
	
	private void scanTheFileType(File file,LinkedList<String> mediaTypes,ScanCallback cb){
		String name = file.getName();
		int i = name.lastIndexOf('.');
		if (i != -1) {
			name = name.substring(i).toUpperCase();
			if (mediaTypes.contains(name)) {
				cb.onFind(file.getAbsolutePath());
//				Log.d(TAG, "find file in the dir-->" + file.getAbsolutePath());
			}
		}
	}
	
}
