package de.christeck.notenanzeiger;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.provider.Settings;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.view.View.OnKeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.Window;
import android.view.Display;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.LinearLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.Toast;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.Context;
import android.webkit.MimeTypeMap;
import java.lang.Math;
import java.io.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.util.Log;



public class MainActivity extends Activity implements ThumbnailRecyclerViewAdapter.ItemClickListener
{
	private final int READ_REQUEST_CODE = 1;
	private final int DEFAULT_PAGE_INDEX = 0;
	private final int ANIMATION_DURATION = 250;
	private boolean performAnimations = false;
	private final String LAST_PAGE_INDEX_KEY = "lastPageIndex";
	private final String FULLSCREEN_KEY = "fullScreen";
	private final String USAGEHINT_SHOWN_KEY = "usageHintShown";
	private ImageView pdfView;
	private GridLayout navigationButtons;
	private RecyclerView recyclerView;
	private ThumbnailRecyclerViewAdapter adapter;
	private Button buttonImport;
	private Button buttonBrowse;
	private Button buttonClear;
	private Button buttonFullscreen;
	private PdfPageRenderer renderer;
	private FileManager fileManager;
	private Point lastDownCoordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		fileManager = new FileManager(getApplication().getApplicationContext());
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        navigationButtons = findViewById(R.id.navigation_buttons);
        recyclerView = findViewById(R.id.thumbnail_recycler_scroller);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
		pdfView = (ImageView) this.findViewById(R.id.pdfView);
		createButtons();
				
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		lastDownCoordinates = new Point(0, 0);
		renderer = new PdfPageRenderer(size);

        adapter = new ThumbnailRecyclerViewAdapter(getApplication().getApplicationContext(), renderer);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

		createGestures();

		// Disable animations on screens with low refresh rates, e.g. eInk devices.
		performAnimations = getWindowManager().getDefaultDisplay().getRefreshRate() > 35.0;

		onNewIntent(getIntent());
    }
	
	
	private void createButtons(){
		buttonImport = (Button) findViewById(R.id.button_import);
		buttonImport.setText(getString(R.string.button_import));
		buttonImport.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(navigationButtons.getVisibility() == View.VISIBLE){
					showNavigationButtons(false);
				}
				performPdfSelection();
			}
		});
		
		buttonBrowse = (Button) findViewById(R.id.button_browse);
		buttonBrowse.setText(getString(R.string.button_browse));
		buttonBrowse.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(navigationButtons.getVisibility() == View.VISIBLE){
					showNavigationButtons(false);
				}
				showThumbnails(true);
			}
		});
		
		buttonClear = (Button) findViewById(R.id.button_clear);
		buttonClear.setText(getString(R.string.button_clear));
		buttonClear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(navigationButtons.getVisibility() == View.VISIBLE){
					showNavigationButtons(false);
				}
				clearCache();
			}
		});
		
		buttonFullscreen = (Button) findViewById(R.id.button_fullscreen);
		buttonFullscreen.setText(getString(R.string.button_fullscreen));
		buttonFullscreen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(navigationButtons.getVisibility() == View.VISIBLE){
					showNavigationButtons(false);
				}
				toggleFullscreen();
			}
		});
	}
	
	private void createGestures(){
		pdfView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				boolean consumed = false;
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					lastDownCoordinates.x = (int) event.getX();
					lastDownCoordinates.y = (int) event.getY();
					consumed = true;
	       	 	}
				else if(event.getAction() == MotionEvent.ACTION_UP){
					if(recyclerView.getVisibility() == View.VISIBLE || navigationButtons.getVisibility() == View.VISIBLE){
						showThumbnails(false);
						showNavigationButtons(false);
					}
					else{
						int diffX = (int) (lastDownCoordinates.x - event.getX());
						int diffY = (int) (lastDownCoordinates.y - event.getY());
						int swipeDistance = (int) Math.sqrt(diffX * diffX + diffY * diffY);
						int swipeAngle = (int) Math.toDegrees(Math.atan2(Math.abs(diffY), Math.abs(diffX)));
						int minSwipeDistanceVertical = (int) (pdfView.getHeight() * 0.6);

						if(swipeAngle > 45 && swipeAngle < 135 && Math.abs(swipeDistance) > minSwipeDistanceVertical){
							swipeAngle = -2;
							showNavigationButtons(true);
						}
						else if(swipeDistance < minSwipeDistanceVertical){
							int direction = determineClickedArea(lastDownCoordinates);
							int pageIndex = renderer.getPageIndex();
							if(pageIndex > -1){
								displayPageBitmap(pageIndex + direction);
							}
						}	
					}
					consumed = true;
	       	 	}
	       	 return consumed;
	    	}
		});
	}
	
	// Callback required by the RecyclerView
    @Override
    public void onItemClick(View view, int position) {
		displayPageBitmap(position);
		showThumbnails(false);
    }

	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    switch (keyCode) {
	        case KeyEvent.KEYCODE_PAGE_DOWN:
				displayPageBitmap(renderer.getPageIndex() + 1);
	            return true;
	        case KeyEvent.KEYCODE_PAGE_UP:
				displayPageBitmap(renderer.getPageIndex() - 1);
	            return true;
	        case KeyEvent.KEYCODE_DPAD_DOWN:
				displayPageBitmap(renderer.getPageIndex() + 1);
	            return true;
	        case KeyEvent.KEYCODE_DPAD_RIGHT:
				displayPageBitmap(renderer.getPageIndex() + 1);
	            return true;
	        case KeyEvent.KEYCODE_DPAD_LEFT:
				displayPageBitmap(renderer.getPageIndex() - 1);
	            return true;
	        case KeyEvent.KEYCODE_DPAD_UP:
				displayPageBitmap(renderer.getPageIndex() - 1);
	            return true;
	        case KeyEvent.KEYCODE_SPACE:
				displayPageBitmap(renderer.getPageIndex() + 1);
	            return true;
	        case KeyEvent.KEYCODE_DEL:
				displayPageBitmap(renderer.getPageIndex() - 1);
	            return true;
	        case KeyEvent.KEYCODE_ENTER:
				performPdfSelection();
	            return true;
	        case KeyEvent.KEYCODE_NUMPAD_ENTER:
				performPdfSelection();
	            return true;
	        case KeyEvent.KEYCODE_ESCAPE:
				showThumbnails(false);
	            return true;
	        case KeyEvent.KEYCODE_EXPLORER:
				performPdfSelection();
	            return true;
	        case KeyEvent.KEYCODE_FORWARD:
				displayPageBitmap(renderer.getPageIndex() + 1);
	            return true;
			// This one is emulated by Android's back button which results in
			// the back button turning pages instead of closing the application.
			/*
			case KeyEvent.KEYCODE_BACK:
				displayPageBitmap(renderer.getPageIndex() - 1);
	            return true;
			*/
	        case KeyEvent.KEYCODE_MEDIA_NEXT:
				displayPageBitmap(renderer.getPageIndex() + 1);
	            return true;
	        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				displayPageBitmap(renderer.getPageIndex() - 1);
	            return true;
	        case KeyEvent.KEYCODE_MOVE_END:
				displayPageBitmap(renderer.pageCount() - 1);
	            return true;
	        case KeyEvent.KEYCODE_MOVE_HOME:
				displayPageBitmap(0);
	            return true;
	        case KeyEvent.KEYCODE_MENU:
				showNavigationButtons(true);
				// showThumbnails(true);
	            return true;
	        default:
	            return super.onKeyUp(keyCode, event);
	    }
	}


	private void clearCache(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.clear_cache_title));
		builder.setMessage(getString(R.string.clear_cache_message));
		builder.setCancelable(false);
		builder.setPositiveButton(getString(R.string.clear_cache_button_remove), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				fileManager.clearCache();
				loadPdf();
				displayPageBitmap(getLastPageDisplayed());
			}
		});
		builder.setNegativeButton(getString(R.string.clear_cache_button_keep), new DialogInterface.OnClickListener() {
			 @Override
		     public void onClick(DialogInterface dialog, int which) {

		     }
		});
 
		builder.show();
	}


	private void performPdfSelection() {
		// This one's called the "Android Storage Access Framework"
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("application/pdf");
		startActivityForResult(intent, READ_REQUEST_CODE);
	}
	

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultingIntent) {
		if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			onNewIntent(resultingIntent);
		}
	}


	@Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
		if(intent == null){
			return;
		}
		
		Uri uri = intent.getData();
		validateUri(uri);
    }

	
	private void validateUri(Uri uri){
		if(uri == null){
			return;
		}	

		String scheme = uri.getScheme();
		if(scheme.equalsIgnoreCase("file")){
			String title = getString(R.string.file_protocol_title);
			String message = getString(R.string.file_protocol_message);
			showInfoDialog(title, message);
			return;
		}
		else if(!scheme.equalsIgnoreCase("content")){
			showToast(getString(R.string.unsupported_scheme) + scheme);
			return;			
		}


        ContentResolver contentResolver = getContentResolver();
		String type = contentResolver.getType(uri);
		MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
		String mimeString = mimeTypeMap.getMimeTypeFromExtension("pdf");
		if(!mimeString.equals(type)){
			showToast(getString(R.string.unsupported_type) + type);
			return;
		}
		
		fileManager.uriToCache(uri);
		loadPdf();
		displayPageBitmap(getLastPageDisplayed());
	}
	
	
	private void loadPdf(){
		ParcelFileDescriptor parcelFileDescriptor = fileManager.getParcelFileDescriptor();
		if(parcelFileDescriptor != null){
			renderer.setPdf(parcelFileDescriptor);
			adapter.notifyDataSetChanged();
		}
	}


    private void displayPageBitmap(int index) {
		int previousIndex = renderer.getPageIndex();

		if(previousIndex < 0){
			previousIndex = 0;
		}
		int direction = index - previousIndex;
		boolean renderThumbnail = false;
		Bitmap pageBitmap = renderer.renderPage(index, renderThumbnail);
		if(pageBitmap != null){
			pdfView.setImageBitmap(pageBitmap);
			saveLastPageDisplayed(renderer.getPageIndex());
			pdfView.startAnimation(createPageAnimation(direction));				
		}
	}


    private AnimationSet createPageAnimation(int direction) {
		float originX = 0.5f;
		float angle = -5.0f;
		if(direction > 0){
			originX = 1.0f;
			angle = angle * (-1);
		}
		else if(direction < 0){
			originX = 0.0f;
		}
		
		float stretchY = 1.05f;
		ScaleAnimation scaleAnimation = new ScaleAnimation (0.0f, 1.0f, stretchY, 1.0f, Animation.RELATIVE_TO_SELF, originX, Animation.RELATIVE_TO_SELF, 1.0f);
		scaleAnimation.setDuration(ANIMATION_DURATION);
		scaleAnimation.setInterpolator(new DecelerateInterpolator(2.0f));
		
		RotateAnimation rotateAnimation = new RotateAnimation(angle, 0.0f, Animation.RELATIVE_TO_SELF, originX, Animation.RELATIVE_TO_SELF, 1.0f);
		rotateAnimation.setDuration(ANIMATION_DURATION);

		TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, -1 * (pdfView.getHeight()), 0);
		translateAnimation.setFillAfter(true);
		translateAnimation.setDuration(ANIMATION_DURATION);

		AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.9f);
		alphaAnimation.setDuration(ANIMATION_DURATION);
		
		AnimationSet animationSet = new AnimationSet(true);
		
		if(performAnimations){
			animationSet.addAnimation(scaleAnimation);
			animationSet.addAnimation(alphaAnimation);

			// Page was not turned, thus the animation will make it appear from top down.
			if(originX == 0.5f){
				animationSet.addAnimation(translateAnimation);
			}
			// Page was turned, thus the animation will make it appear via a rotation.
			else{
				animationSet.addAnimation(rotateAnimation);
			}			
		}
	
		return animationSet;
	}


	private void showThumbnails(boolean show){
		if(show){
			recyclerView.setVisibility(View.VISIBLE);
			recyclerView.getLayoutManager().scrollToPosition(renderer.getPageIndex());
			recyclerView.startAnimation(createDialogAnimation());
			}
		else{
			recyclerView.setVisibility(View.GONE);
		}
	}
	
	private void showNavigationButtons(boolean show){
		if(show){
			navigationButtons.setVisibility(View.VISIBLE);
			navigationButtons.startAnimation(createDialogAnimation());
			}
		else{
			navigationButtons.setVisibility(View.GONE);
		}
	}
	
	
	private void toggleFullscreen(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
		boolean showFullscreen = prefs.getBoolean(FULLSCREEN_KEY, false);
		showFullscreen = (!showFullscreen);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(FULLSCREEN_KEY, showFullscreen);
		editor.apply();
		
		onWindowFocusChanged(true);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
		boolean showFullscreen = prefs.getBoolean(FULLSCREEN_KEY, false);

		View contentView = getWindow().getDecorView();
		if(showFullscreen){
			contentView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_FULLSCREEN |
				View.SYSTEM_UI_FLAG_IMMERSIVE);
		}
		else{
			contentView.setSystemUiVisibility(0);
		}

		Point size = new Point(pdfView.getWidth(), pdfView.getHeight());
		renderer.setViewportSize(size);
		displayPageBitmap(getLastPageDisplayed());
		showUsageHint();
	}


    private AnimationSet createDialogAnimation() {
		ScaleAnimation scaleAnimation = new ScaleAnimation (0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		scaleAnimation.setDuration(ANIMATION_DURATION);
		scaleAnimation.setInterpolator(new DecelerateInterpolator(4.0f));
		
		AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
		alphaAnimation.setDuration(ANIMATION_DURATION);
		
		AnimationSet animationSet = new AnimationSet(true);
		
		if(performAnimations){
			animationSet.addAnimation(scaleAnimation);
			animationSet.addAnimation(alphaAnimation);
		}
		
		return animationSet;
	}



	private void saveLastPageDisplayed(int pageIndex){
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		editor.putInt(LAST_PAGE_INDEX_KEY, pageIndex);
		editor.apply();
	}


	private int getLastPageDisplayed(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
		return prefs.getInt(LAST_PAGE_INDEX_KEY, DEFAULT_PAGE_INDEX);
	}

	
	private int determineClickedArea(Point C){
		int areaCode = 0;

		Point A = new Point(pdfView.getWidth(), 0);
		Point B = new Point(0, pdfView.getHeight());
		// Dot Product respectively Hesse normal form
		int result =  (B.x - A.x) * (C.y - A.y) - (B.y - A.y) * (C.x - A.x);
		if(result < 0){
			areaCode = 1; 
		}
		else{
			areaCode = -1;
		}

		return areaCode;
	}


	private void showInfoDialog(String title, String message){
		AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);
		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.alert_button_neutral),
		    new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) {
		            dialog.dismiss();
		        }
		    });
		alertDialog.show();
	}


	private void showUsageHint(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
		boolean usageHintShown = prefs.getBoolean(USAGEHINT_SHOWN_KEY, false);
		
		if(!usageHintShown){
			showInfoDialog(getString(R.string.usage_title), getString(R.string.usage_hint));
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(USAGEHINT_SHOWN_KEY, true);
			editor.apply();
		}
	}


	private void showToast(String message){
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}
	
	
	@Override
	public void onBackPressed() {
		finish();
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		loadPdf();
		renderer.setRecentPageIndex(getLastPageDisplayed());
		showThumbnails(false);
		showNavigationButtons(false);
	}


	@Override
	public void onPause() {
		super.onPause();
		renderer.closeRenderer();
	}

}

