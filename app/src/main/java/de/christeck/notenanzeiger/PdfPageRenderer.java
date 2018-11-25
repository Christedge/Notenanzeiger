package de.christeck.notenanzeiger;

import java.io.IOException;
import java.math.BigDecimal;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.pdf.PdfRenderer;
import android.graphics.Rect;


public class PdfPageRenderer
{
	private PdfRenderer pdfRenderer;
	private PdfRenderer.Page currentPage;
	private Point viewportSize;
	private static final int CONTENT_MARGIN = 4;
	private static final int MAX_CROP_FRACTION = 8;
	private static final int BACKGROUND_COLOR = 0xfffffff0; // snow white


	public PdfPageRenderer(Point preliminarySize) {
		// This size is preliminary, since it is initialized by getWindowManager().getDefaultDisplay().
		viewportSize = preliminarySize;
		pdfRenderer = null;
		currentPage = null;
	}


	public void setViewportSize(Point pdfViewSize) {
		// As soon the app is running, the size of the pdf view can be set.
		viewportSize = pdfViewSize;
	}


    public void setPdf(ParcelFileDescriptor pdfFileDescriptor){
		if(pdfRenderer != null){
			pdfRenderer.close();
			pdfRenderer = null;
		}

        try {
            pdfRenderer = new PdfRenderer(pdfFileDescriptor);
        } catch (IOException e) {
            e.printStackTrace();
		}
    }


	public void closeRenderer() {
		if(pdfRenderer != null){
			pdfRenderer.close();
			pdfRenderer = null;
		}
		currentPage = null;
	}


	public void setRecentPageIndex(int index){
		if(pdfRenderer != null){
			int pageCount = pdfRenderer.getPageCount();
			if(index > 0 && index < pageCount){
				// This is just to tell the renderer the last page rendered in case it gets resumed.
				currentPage = pdfRenderer.openPage(index);
				currentPage.close();
			}
		}
	}
	

	public int getPageIndex(){
		int pageIndex = -1;
		if(currentPage != null){
			pageIndex = currentPage.getIndex();
		}
		return pageIndex;
	}

	
	public int pageCount(){
		int pageCount = 0;
		if(pdfRenderer != null){
			pageCount = pdfRenderer.getPageCount();
		}
		return pageCount;
	}


	public Bitmap renderPage(int pageIndex, boolean thumbnail){
		boolean indexIsValid = validateIndex(pageIndex);
		Bitmap pageBitmap = null;
		if(indexIsValid){
			currentPage = pdfRenderer.openPage(pageIndex);
			Point size = calculateBitmapSize(thumbnail);
			pageBitmap = generatePageBitmap(size);
			currentPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
			currentPage.close();
			if(!thumbnail){
				pageBitmap = removeMargin(pageBitmap);
			}
		}
		return pageBitmap;
	}



	private boolean validateIndex(int index){
		boolean indexIsValid = false;

		if(index +1 > 0 && index < pageCount()){
			indexIsValid = true;
		}
		
		return indexIsValid;
	}


	private Bitmap generatePageBitmap(Point size){
		Bitmap bitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(BACKGROUND_COLOR);
		canvas.drawBitmap(bitmap, 0, 0, null);
		return bitmap;
	}
	
	
	private Point calculateBitmapSize(boolean thumbnail){
		int viewportWidth = viewportSize.x;
		int viewportHeight = viewportSize.y;
		int pdfWidth = currentPage.getWidth();
		int pdfHeight = currentPage.getHeight();
		float ratioPdf = (float) pdfWidth / (float) pdfHeight;
		float ratioMax = (float) viewportWidth / (float) viewportHeight;

		int finalWidth = viewportWidth;
		int finalHeight = viewportHeight;
		if (ratioMax > ratioPdf) {
			finalWidth = (int) ((float)viewportHeight * ratioPdf);
		} else {
			finalHeight = (int) ((float)viewportWidth / ratioPdf);
		}
		
		if(thumbnail == true){
			finalWidth = (int) ((double)finalWidth / 1.8);
			finalHeight = (int) ((double)finalHeight / 1.8);			
		}
		
		Point size = new Point(finalWidth, finalHeight);
		return size;
	}


	private Bitmap removeMargin(Bitmap sourceBitmap){
		Rect pageBounds = new Rect(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight());
		Rect renderedBounds = new Rect(pageBounds);

		int MAX_CROP_FRACTION = 8;
		int maxCropDepth = 0;


		// Left
		maxCropDepth = sourceBitmap.getWidth() / MAX_CROP_FRACTION;
		// Copying the array takes time, but comparing the pixels on it is much faster than operating on the bitmap directly
		int[] marginPixels = new int[pageBounds.bottom];
		
		loop:
		for (int x = 0; x < maxCropDepth; x++) {
			sourceBitmap.getPixels(marginPixels, 0, 1, x, 0, 1, pageBounds.bottom);
			for (int y = 0; y < pageBounds.bottom; y++) {
				if (marginPixels[y] != BACKGROUND_COLOR || (x == maxCropDepth -1 && y == pageBounds.bottom -1)) {
					renderedBounds.left = x;
					break loop;
				}
			}
		}
		// Right
		loop:
		for (int x = maxCropDepth -1; x >= 0; x--) {
			sourceBitmap.getPixels(marginPixels, 0, 1, pageBounds.right - maxCropDepth + x, 0, 1, pageBounds.bottom);			
			for (int y = 0; y < pageBounds.bottom; y++) {
				if (marginPixels[y] != BACKGROUND_COLOR || (x == 0 && y == pageBounds.bottom -1)) {
					renderedBounds.right = pageBounds.right - maxCropDepth + x;
					break loop;
				}
			}
		}
		// Top
		maxCropDepth = sourceBitmap.getHeight() / MAX_CROP_FRACTION;
		marginPixels = new int[pageBounds.right];
		
		loop:
		for (int y = 0; y < maxCropDepth; y++) {
			sourceBitmap.getPixels(marginPixels, 0, pageBounds.right, 0, y, pageBounds.right, 1);			
			for (int x = 0; x < pageBounds.right; x++) {
				if (marginPixels[x] != BACKGROUND_COLOR || (x == pageBounds.right -1 && y == maxCropDepth -1)) {
					renderedBounds.top = y;
					break loop;
				}
			}
		}
		// Bottom
		loop:
		for (int y = maxCropDepth -1; y >= 0; y--) {
			sourceBitmap.getPixels(marginPixels, 0, pageBounds.right, 0, pageBounds.bottom - maxCropDepth + y, pageBounds.right, 1);			
			for (int x = 0; x < pageBounds.right; x++) {
				if (marginPixels[x] != BACKGROUND_COLOR || (x == pageBounds.right -1 && y == 0)) {
					renderedBounds.bottom = pageBounds.bottom - maxCropDepth + y;
					break loop;
				}
			}
		}


		renderedBounds = toPageRatio(pageBounds, renderedBounds);

		if(pageBounds.contains(renderedBounds)){
			sourceBitmap = Bitmap.createBitmap(sourceBitmap, renderedBounds.left, renderedBounds.top, renderedBounds.right - renderedBounds.left, renderedBounds.bottom - renderedBounds.top);			
		}
		else{
			Log.w("Notenanzeiger", "PdfPageRenderer coding bug: Rendered boundary exceeds page boundary");
		}

		return sourceBitmap;
	}
	
	
	private Rect toPageRatio(Rect pageBounds, Rect renderedBounds){
		int renderedWidth = renderedBounds.right - renderedBounds.left;
		int renderedHeight = renderedBounds.bottom - renderedBounds.top;
		BigDecimal pageRatio = new BigDecimal((double) pageBounds.bottom / (double) pageBounds.right);
		BigDecimal renderedRatio = new BigDecimal((double) (renderedHeight) / (double) (renderedWidth));
		int comparisonResult = (pageRatio.compareTo(renderedRatio));

		int offsetX = 0;
		int offsetY = 0;

		if (comparisonResult == -1){
			renderedBounds = addMarginsHeight(pageBounds, renderedBounds);
			renderedHeight = renderedBounds.bottom - renderedBounds.top;
			int finalWidth = (int) ((double) (renderedHeight) / pageRatio.doubleValue());
			offsetX = (finalWidth - renderedWidth) / 2;
			offsetX = offsetX * (-1);
		} else if(comparisonResult == 1){
			renderedBounds = addMarginsWidth(pageBounds, renderedBounds);
			renderedWidth = renderedBounds.right - renderedBounds.left;
			int finalHeight = (int) ((double) (renderedWidth) * pageRatio.doubleValue());
			offsetY = (finalHeight - renderedHeight) / 2;
			offsetY = offsetY * (-1);
		}
		
		renderedBounds.inset(offsetX, offsetY);

		return renderedBounds;
	}


	private Rect addMarginsWidth(Rect pageBounds, Rect renderedBounds){
		
		loop:
		for(int i = CONTENT_MARGIN; i > 0; i --){
			if(renderedBounds.left - i >= pageBounds.left){
				renderedBounds.left = renderedBounds.left - i;
				break loop;
			}
		}
		loop:
		for(int i = CONTENT_MARGIN; i > 0; i --){
			if(renderedBounds.right + i <= pageBounds.right){
				renderedBounds.right = renderedBounds.right + i;
				break loop;
			}
		}
		
		return renderedBounds;
	}


	private Rect addMarginsHeight(Rect pageBounds, Rect renderedBounds){

		loop:
		for(int i = CONTENT_MARGIN; i > 0; i --){
			if(renderedBounds.top - i >= pageBounds.top){
				renderedBounds.top = renderedBounds.top - i;
				break loop;
			}
		}
		loop:
		for(int i = CONTENT_MARGIN; i > 0; i --){
			if(renderedBounds.bottom + i <= pageBounds.bottom){
				renderedBounds.bottom = renderedBounds.bottom + i;
				break loop;
			}
		}
		
		return renderedBounds;
	}

}

