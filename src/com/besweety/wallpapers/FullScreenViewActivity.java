package com.besweety.wallpapers;

import com.besweety.wallpapers.helper.FullScreenImageAdapter;

import java.util.List;

import com.besweety.wallpapers.listeners.OnSwipeTouchListener;
import com.besweety.wallpapers.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.JsonObjectRequest;
import com.besweety.wallpapers.app.AppController;
import com.besweety.wallpapers.picasa.model.Wallpaper;
import com.besweety.wallpapers.util.Utils;

public class FullScreenViewActivity extends Activity implements OnClickListener {
	private static final String TAG = FullScreenViewActivity.class
			.getSimpleName();
	public static final String TAG_SEL_IMAGE = "selectedImage";
	public static final String TAG_ALL_IMAGES="allImages";
	private Wallpaper selectedPhoto;
	private ImageView fullImageView;
	private LinearLayout llSetWallpaper, llDownloadWallpaper;
	private HorizontalScrollView llImage;
	private Utils utils;
	private ProgressBar pbLoader;
	private List<Wallpaper> photosList;
	

	private FullScreenImageAdapter adapter;
	private ViewPager viewPager;


	
	

	// Picasa JSON response node keys
	private static final String TAG_ENTRY = "entry",
			TAG_MEDIA_GROUP = "media$group",
			TAG_MEDIA_CONTENT = "media$content", TAG_IMG_URL = "url",
			TAG_IMG_WIDTH = "width", TAG_IMG_HEIGHT = "height";

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fullscreen_image);

		fullImageView = (ImageView) findViewById(R.id.imgFullscreen);
		llSetWallpaper = (LinearLayout) findViewById(R.id.llSetWallpaper);
		llDownloadWallpaper = (LinearLayout) findViewById(R.id.llDownloadWallpaper);
		llImage=(HorizontalScrollView) findViewById(R.id.llImage);
		pbLoader = (ProgressBar) findViewById(R.id.pbLoader);

		// hide the action bar in fullscreen mode
		getActionBar().hide();

		utils = new Utils(getApplicationContext());

		// layout click listeners
		llSetWallpaper.setOnClickListener(this);
		llDownloadWallpaper.setOnClickListener(this);

		// setting layout buttons alpha/opacity
		llSetWallpaper.getBackground().setAlpha(70);
		llDownloadWallpaper.getBackground().setAlpha(70);

		Intent i = getIntent();
		selectedPhoto = (Wallpaper) i.getSerializableExtra(TAG_SEL_IMAGE);
		
		Bundle extra = new Bundle();
		extra =  (Bundle) i.getBundleExtra(TAG_ALL_IMAGES);
		photosList= (List<Wallpaper>) extra.getSerializable("objects");
		
		// check for selected photo null
		workWithPhoto(selectedPhoto);
	}

	/**
	 * Fetching image fullresolution json
	 * */
	private void fetchFullResolutionImage() {
		String url = selectedPhoto.getPhotoJson();

		// show loader before making request
		pbLoader.setVisibility(View.VISIBLE);
		llSetWallpaper.setVisibility(View.GONE);
		llDownloadWallpaper.setVisibility(View.GONE);

		// volley's json obj request
		JsonObjectRequest jsonObjReq = new JsonObjectRequest(Method.GET, url,
				null, new Response.Listener<JSONObject>() {

					@Override
					public void onResponse(JSONObject response) {
						Log.d(TAG,
								"Image full resolution json: "
										+ response.toString());
						try {
							// Parsing the json response
							JSONObject entry = response
									.getJSONObject(TAG_ENTRY);

							JSONArray mediacontentArry = entry.getJSONObject(
									TAG_MEDIA_GROUP).getJSONArray(
									TAG_MEDIA_CONTENT);

							JSONObject mediaObj = (JSONObject) mediacontentArry
									.get(0);

							String fullResolutionUrl = mediaObj
									.getString(TAG_IMG_URL);

							// image full resolution widht and height
							final int width = mediaObj.getInt(TAG_IMG_WIDTH);
							final int height = mediaObj.getInt(TAG_IMG_HEIGHT);

							Log.d(TAG, "Full resolution image. url: "
									+ fullResolutionUrl + ", w: " + width
									+ ", h: " + height);

							ImageLoader imageLoader = AppController
									.getInstance().getImageLoader();

							// We download image into ImageView instead of
							// NetworkImageView to have callback methods
							// Currently NetworkImageView doesn't have callback
							// methods

							imageLoader.get(fullResolutionUrl,
									new ImageListener() {

										@Override
										public void onErrorResponse(
												VolleyError arg0) {
											Toast.makeText(
													getApplicationContext(),
													getString(R.string.msg_wall_fetch_error),
													Toast.LENGTH_LONG).show();
										}

										@Override
										public void onResponse(
												ImageContainer response,
												boolean arg1) {
											if (response.getBitmap() != null) {
												// load bitmap into imageview
												fullImageView
														.setImageBitmap(response
																.getBitmap());
												adjustImageAspect(width, height);

												// hide loader and show set &
												// download buttons
												pbLoader.setVisibility(View.GONE);
												llSetWallpaper
														.setVisibility(View.VISIBLE);
												llDownloadWallpaper
														.setVisibility(View.VISIBLE);
											}
										}
									});

						} catch (JSONException e) {
							e.printStackTrace();
							Toast.makeText(getApplicationContext(),
									getString(R.string.msg_unknown_error),
									Toast.LENGTH_LONG).show();
						}

					}
				}, new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e(TAG, "Error: " + error.getMessage());
						// unable to fetch wallpapers
						// either google username is wrong or
						// devices doesn't have internet connection
						Toast.makeText(getApplicationContext(),
								getString(R.string.msg_wall_fetch_error),
								Toast.LENGTH_LONG).show();

					}
				});

		// Remove the url from cache
		AppController.getInstance().getRequestQueue().getCache().remove(url);

		// Disable the cache for this url, so that it always fetches updated
		// json
		jsonObjReq.setShouldCache(false);

		// Adding request to request queue
		AppController.getInstance().addToRequestQueue(jsonObjReq);
		
		
		llImage.setOnTouchListener(new OnSwipeTouchListener(null) {
	         
            public void onSwipeRight() {
            	
            	int prevPhotoIndex=selectedPhoto.getPosition()-1;
            	
            	if(prevPhotoIndex==0)
            	{
            		prevPhotoIndex=photosList.size();
            	}
            		selectedPhoto=photosList.get(prevPhotoIndex);
            		workWithPhoto(selectedPhoto);
            	
            }
            public void onSwipeLeft() {
            	
            	int nextPhotoIndex=selectedPhoto.getPosition()+1;
            	
            	if(photosList.size()==nextPhotoIndex)
            	{
            		nextPhotoIndex=0;
            	}
            	
            	selectedPhoto=photosList.get(nextPhotoIndex);
            	workWithPhoto(selectedPhoto);


            }


        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }
        });
	}

	/**
	 * Adjusting the image aspect ration to scroll horizontally, Image height
	 * will be screen height, width will be calculated respected to height
	 * */
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private void adjustImageAspect(int bWidth, int bHeight) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		if (bWidth == 0 || bHeight == 0)
			return;

		int sWidth = 0;

		if (android.os.Build.VERSION.SDK_INT >= 13) {
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			sWidth = size.x;
		} else {
			Display display = getWindowManager().getDefaultDisplay();
			sWidth = display.getWidth();
		}

		int new_height = (int) Math.floor((double)bHeight * (double)sWidth / (double)bWidth);
		params.width = sWidth;
		params.height = new_height;

		Log.d(TAG, "Fullscreen image new dimensions: w = " + sWidth
				+ ", h = " + new_height);

		fullImageView.setLayoutParams(params);
	}

	/**
	 * View click listener
	 * */
	@Override
	public void onClick(View v) {
		Bitmap bitmap = ((BitmapDrawable) fullImageView.getDrawable())
				.getBitmap();
		switch (v.getId()) {
		// button Download Wallpaper tapped
		case R.id.llDownloadWallpaper:
			utils.saveImageToSDCard(bitmap);
			break;
		// button Set As Wallpaper tapped
		case R.id.llSetWallpaper:
			utils.setAsWallpaper(bitmap);
			break;
		default:
			break;
		}

	}
	
	private void workWithPhoto(Wallpaper selectedPhoto)
	{
		// check for selected photo null
				if (selectedPhoto != null) {

					// fetch photo full resolution image by making another json request
					fetchFullResolutionImage();

				} else {
					Toast.makeText(getApplicationContext(),
							getString(R.string.msg_unknown_error), Toast.LENGTH_SHORT)
							.show();
				}
		
	}
}
