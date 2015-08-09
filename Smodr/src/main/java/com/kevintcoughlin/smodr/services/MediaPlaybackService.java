package com.kevintcoughlin.smodr.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.kevintcoughlin.smodr.R;
import com.kevintcoughlin.smodr.views.activities.MainActivity;

import java.io.IOException;

public final class MediaPlaybackService extends Service implements MediaPlayer.OnErrorListener,
		MediaPlayer.OnPreparedListener {
	public static final int NOTIFICATION_ID = 37;
	public static final String INTENT_EPISODE_URL = "intent_episode_url";
	public static final String INTENT_EPISODE_TITLE = "intent_episode_title";
	public static final String ACTION_PLAY = "com.kevintcoughlin.smodr.app.PLAY";
	public static final String ACTION_PAUSE = "com.kevintcoughlin.smodr.app.PAUSE";
	public static final String ACTION_RESUME = "com.kevintcoughlin.smodr.app.RESUME";
	public static final String ACTION_STOP = "com.kevintcoughlin.smodr.app.STOP";
	private String mTitle = "";
	private int mPosition = 0;
	private boolean mPrepared = false;
	MediaPlayer mMediaPlayer = null;

	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		if (intent != null && intent.getAction() == null) {
			stopPlayback();
		} else if (intent != null) {
			if (intent.getAction().equals(ACTION_PLAY)) {
				final String url = intent.getStringExtra(INTENT_EPISODE_URL);
				mTitle = intent.getStringExtra(INTENT_EPISODE_TITLE);

				if (url != null) {
					try {
						if (mMediaPlayer == null) {
							mMediaPlayer = new MediaPlayer();
							mMediaPlayer.setOnPreparedListener(this);
						} else {
							stopPlayback();
						}
						mMediaPlayer.setDataSource(url);
						mMediaPlayer.prepareAsync();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					stopPlayback();
				}
			} else if (intent.getAction().equals(ACTION_PAUSE)) {
				pausePlayback();
				createNotification();
			} else if (intent.getAction().equals(ACTION_RESUME)) {
				if (mPrepared) {
					mMediaPlayer.start();
				} else {
					stopPlayback();
				}
				createNotification();
			} else if (intent.getAction().equals(ACTION_STOP)) {
				stopPlayback();
			}
		}

		return Service.START_REDELIVER_INTENT;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		stopPlayback();
		mPrepared = false;

		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopPlayback();
		mPrepared = false;
	}

	private void pausePlayback() {
		mMediaPlayer.pause();
	}

	private void stopPlayback() {
		mMediaPlayer.reset();
		mPrepared = false;

		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.cancel(NOTIFICATION_ID);
		stopForeground(true);
	}

	private void createNotification() {
		Intent mIntent = new Intent(this, MediaPlaybackService.class);
		mIntent.setAction(ACTION_STOP);

		PendingIntent mPendingIntent = PendingIntent.getService(
				this,
				0,
				mIntent,
				PendingIntent.FLAG_UPDATE_CURRENT
		);

		final String SERVICE_NAME = "Smodr";
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.icon)
						.setOngoing(true)
						.setPriority(Notification.PRIORITY_HIGH)
						.setContentTitle(SERVICE_NAME)
						.setContentText(mTitle)
						.addAction(
								R.drawable.ic_action_pause,
								getString(R.string.notification_action_pause),
								mPendingIntent
						);

		Intent resultIntent = new Intent(this, MainActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(resultIntent);

		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		mBuilder.setContentIntent(resultPendingIntent);

		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Notification notification = mBuilder.build();

		mNotificationManager.notify(NOTIFICATION_ID, notification);
		startForeground(NOTIFICATION_ID, notification);
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		mediaPlayer.start();

		// At the end of the episode, seek to the beginning.
		if (mPosition >= mediaPlayer.getDuration())
			mPosition = 0;

		mediaPlayer.seekTo(mPosition);
		createNotification();

		mPrepared = true;
	}
}