package com.tcf_corp.android.map;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.tcf_corp.android.util.LogUtil;

public class CustomMapView extends MapView {
	private static final String TAG = CustomMapView.class.getSimpleName();
	private static final boolean DEBUG = false;

	private final long EVENT_DELAY = 500L;

	private final Handler mHandler;
	private final Runnable mZoomChangeNotifier;
	private final Runnable mCenterChangeNotifier;
	private final Runnable mAnimationFinishChecker;

	private final ArrayList<ZoomListener> mZoomListeners;
	private int mLastZoomLevel;

	private final ArrayList<PanListener> mPanListeners;
	private int mLastCenterLati;
	private int mLastCenterLongi;
	private int mAnimationToLati;
	private int mAnimationToLongi;

	private boolean isTouched;
	private boolean isDuringAnimation;

	public CustomMapView(Context context, AttributeSet attribute) {
		super(context, attribute);

		// Handler objectを取得
		mHandler = new Handler();
		// Zoom level変更用、Center position変更用の通知Runnableをそれぞれ作成
		// Zoom level変更を通知するRunnableを作成
		mZoomChangeNotifier = new Runnable() {
			@Override
			public void run() {
				// 前回postしたZoom level変更を通知するRunnableが残っていればcancelする
				mHandler.removeCallbacks(this);
				notifyZoomChange();
			}
		};
		// ZoomListenerを保持するArrayListを作成
		mZoomListeners = new ArrayList<ZoomListener>();
		mLastZoomLevel = getZoomLevel();

		// Center position変更を通知するRunnableを作成
		mCenterChangeNotifier = new Runnable() {
			@Override
			public void run() {
				// 前回postしたcenter position変更を通知するRunnableが残っていればcancelする
				mHandler.removeCallbacks(this);
				notifyCenterChange();
			}
		};
		// PanListenerを保持するArrayListを作成
		mPanListeners = new ArrayList<PanListener>();
		GeoPoint center = getMapCenter();
		mLastCenterLati = center.getLatitudeE6();
		mLastCenterLongi = center.getLongitudeE6();

		// 移動Animation終了check用のRunnableを作成
		mAnimationFinishChecker = new Runnable() {
			@Override
			public void run() {
				mHandler.removeCallbacks(this);
				// 移動Animationが終了していればZoom level変更、Center
				// position変更確認後、listenerに通知
				if (isAnimationFinished()) {
					isDuringAnimation = false;
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mHandler.removeCallbacks(mZoomChangeNotifier);
							mHandler.removeCallbacks(mCenterChangeNotifier);
							// UI上移動Animationは終了しているが、移動距離が長い場合MapVeiw#computeScroll()が実行されている場合があるので
							// computeScroll() ->
							// checkAndNotifyChanges()による通知cancelを避けるために
							// mZoomChangeNotifier,
							// mCetnerChangeNotifierをpostせずにListenerに通知
							if (mLastZoomLevel != getZoomLevel()) {
								notifyZoomChange();
							}
							if (isCenterChanged()) {
								notifyCenterChange();
							}
						}
					});
					// 移動Animationが終了していなければ再度、自分をpostする
				} else {
					mHandler.postDelayed(this, EVENT_DELAY);
				}
			}
		};

		isTouched = false;
		isDuringAnimation = false;
	}

	// MapView#onTouchEvent()をoverrideしtouch中か移動Animation中かのflagを設定
	@Override
	public boolean onTouchEvent(MotionEvent ev) {

		int action = ev.getAction();

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			if (DEBUG)
				LogUtil.v(TAG, "ACTION_DOWN");
			isTouched = true;
			isDuringAnimation = false;
			break;
		case MotionEvent.ACTION_UP:
			if (DEBUG) {
				LogUtil.v(TAG, "ACTION_UP");
			}
			isTouched = false;
			break;
		}

		return super.onTouchEvent(ev);
	}

	// MapView#computeScroll()をoverrideしZoom level変更/Center position変更の確認
	@Override
	public void computeScroll() {
		super.computeScroll();
		if (DEBUG) {
			LogUtil.v(TAG, "computeScroll");
		}
		// 移動Animation中のZoom level変更, Center position変更は通知しない
		if (!isDuringAnimation) {
			checkAndNotifyChanges(EVENT_DELAY);
		}
	}

	private void checkAndNotifyChanges(long delay) {
		// 直近に取得したZoom levelと異なるZoom levelか確認
		if (getZoomLevel() != mLastZoomLevel) {
			if (DEBUG) {
				LogUtil.v(TAG, "Zoom level changed");
			}
			// Zoom level変更を通知するRunnableをpost
			mHandler.postDelayed(mZoomChangeNotifier, delay);
		} else {
			if (DEBUG) {
				LogUtil.v(TAG, "Zoom level NOT changed");
			}
		}
		// 直近に取得したcenter positionと現在のcenter positionが異なるか確認
		// touch中か確認
		if (isCenterChanged() && !isTouched) {
			// Center position変更を通知するRunnableをpost
			mHandler.postDelayed(mCenterChangeNotifier, delay);
		}
	}

	private boolean isCenterChanged() {
		GeoPoint gp = getMapCenter();
		if (mLastCenterLati == gp.getLatitudeE6()
				&& mLastCenterLongi == gp.getLongitudeE6()) {
			if (DEBUG) {
				LogUtil.v(TAG, "Center NOT changed");
			}
			return false;
		}
		if (DEBUG) {
			LogUtil.v(TAG, "Center changed");
		}
		return true;
	}

	public void animateTo(GeoPoint gp) {
		isDuringAnimation = true;
		// どこが移動Animation終了後のCenter positionになるべきか保持しておく
		mAnimationToLati = gp.getLatitudeE6();
		mAnimationToLongi = gp.getLongitudeE6();
		getController().animateTo(gp);
		// 移動Animationが終了したかどうかの確認の為のRunnableをpostする
		mHandler.postDelayed(mAnimationFinishChecker, EVENT_DELAY);
	}

	private boolean isAnimationFinished() {
		GeoPoint center = getMapCenter();
		int centerLati = center.getLatitudeE6();
		int centerLongi = center.getLongitudeE6();

		if (DEBUG) {
			LogUtil.v(TAG, "mAnimationToLati:" + mAnimationToLati);
			LogUtil.v(TAG, "mAnimationToLongi:" + mAnimationToLongi);
			LogUtil.v(TAG, "centerLati:" + centerLati);
			LogUtil.v(TAG, "centerLongi:" + centerLongi);
		}

		if (centerLati == mAnimationToLati && centerLongi == mAnimationToLongi) {
			return true;
		}

		return false;
	}

	public void setZoom(int zoomLevel) {
		getController().setZoom(zoomLevel);
		// 移動Animationが終了したかどうかの確認の為のRunnableをpostする
		mHandler.postDelayed(mAnimationFinishChecker, EVENT_DELAY);
	}

	public void setZoomListener(ZoomListener listener) {
		mZoomListeners.add(listener);
	}

	public void notifyZoomChange() {
		if (DEBUG) {
			LogUtil.d(TAG, "New zoom level: " + getZoomLevel());
		}
		int newZoomLevel = getZoomLevel();
		for (ZoomListener listener : mZoomListeners) {
			// Zoom level変更をlistenerに通知
			listener.onZoomChange(CustomMapView.this, newZoomLevel,
					mLastZoomLevel);
		}
		// 現Zoom levelを直近のZoom levelとして保持
		mLastZoomLevel = newZoomLevel;
	}

	public void setPanListener(PanListener listener) {
		mPanListeners.add(listener);
	}

	public void notifyCenterChange() {
		if (DEBUG) {
			LogUtil.d(TAG, "notify center change");
		}

		for (PanListener listener : mPanListeners) {
			// Center position変更をlistenerに通知
			listener.onCenterChange(CustomMapView.this, getMapCenter(),
					new GeoPoint(mLastCenterLati, mLastCenterLongi));
		}
		// 現Center positionを直近のCenter positionとして保持
		GeoPoint center = getMapCenter();
		mLastCenterLati = center.getLatitudeE6();
		mLastCenterLongi = center.getLongitudeE6();
	}

	public interface ZoomListener {
		public void onZoomChange(CustomMapView mapView, int newZoom, int oldZoom);
	}

	public interface PanListener {
		public void onCenterChange(CustomMapView mapView, GeoPoint newGeoPoint,
				GeoPoint oldGeoPoint);
	}

	public interface OnCenterChangeListener {
		public void onCenterChanged(int latitudeE6, int longitudeE6);
	}
}
