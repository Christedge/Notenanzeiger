package de.christeck.notenanzeiger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.os.Bundle;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.content.Intent;
import android.content.Context;
import android.content.ContentResolver;
import android.widget.Toast;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.util.Log;
import android.webkit.MimeTypeMap;


public class FileManager
{
	private Context applicationContext;
	private static final String CACHE_FILENAME = "Default.pdf";
	private static final String LAST_PAGE_INDEX_KEY = "lastPageIndex";


	public FileManager(Context context) {
		applicationContext = context;
	}


	public void uriToCache(Uri uri){
        ContentResolver contentResolver = applicationContext.getContentResolver();

		try {
			InputStream inputStream = contentResolver.openInputStream(uri);
			copyInputStreamToCache(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
			showToast("Error: " + e.getMessage());
		}
	}


	private void populateCacheIfEmpty(){
		File file = getCacheFile();
		if (!file.exists()) {
			try {
				InputStream inputStream = applicationContext.getAssets().open(CACHE_FILENAME);
				copyInputStreamToCache(inputStream);
			} catch (IOException e) {
				e.printStackTrace();
				showToast("Error: " + e.getMessage());
			}
		}
	}


	private void copyInputStreamToCache(InputStream inputStream){
		File cacheFile = getCacheFile();
		File cacheDir = applicationContext.getFilesDir();
		long remainingSpace = cacheDir.getUsableSpace();
		long criticalSpace = 1024 * 1024 * 100;
		boolean noSpaceLeftOnDeviceAhead = false;
	
		try
        {
			FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
			final byte[] buffer = new byte[1024];
			int size = 0;
			loop:
			while ((size = inputStream.read(buffer)) != -1) {
				if(remainingSpace < criticalSpace){
					noSpaceLeftOnDeviceAhead = true;
					break loop;
				}
				else{
					fileOutputStream.write(buffer, 0, size);
					remainingSpace = cacheDir.getUsableSpace();
				}
			}
			inputStream.close();
			fileOutputStream.close();
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(applicationContext).edit();
			editor.putInt(LAST_PAGE_INDEX_KEY, 0);
			editor.apply();
			
			if(noSpaceLeftOnDeviceAhead){
				cacheFile.delete();
				showToast("Little space available. Delete some files and try again.");
			}
			else if(cacheFile.length() > criticalSpace){
					showToast("Huge file imported. Consider loading a smaller file after reading to free space on device.");
			}

        } catch (IOException e) {
			e.printStackTrace();
			showToast("Error: " + e.getMessage());
		}		
	}
	

	public void clearCache(){
		File file = getCacheFile();
		if (file.exists()){
			file.delete();
		}
		populateCacheIfEmpty();
	}
	
	
	private File getCacheFile(){
		File file = new File(applicationContext.getFilesDir(), CACHE_FILENAME);
		return file;
	}
	
	
	public ParcelFileDescriptor getParcelFileDescriptor(){
		populateCacheIfEmpty();
		File file = getCacheFile();
		ParcelFileDescriptor parcelFileDescriptor = null;
		if (file.exists()) {
			try {
				parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
       		} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return parcelFileDescriptor;
	}

	
	private void showToast(String message){
		Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show();
	}

}
