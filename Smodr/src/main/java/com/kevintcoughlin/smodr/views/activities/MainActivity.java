package com.kevintcoughlin.smodr.views.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.kevintcoughlin.smodr.BuildConfig;
import com.kevintcoughlin.smodr.R;
import com.kevintcoughlin.smodr.models.Channel;
import com.kevintcoughlin.smodr.models.Item;
import com.kevintcoughlin.smodr.services.MediaService;
import com.kevintcoughlin.smodr.views.TextViewKt;
import com.kevintcoughlin.smodr.views.fragments.EpisodesFragment;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public final class MainActivity extends AppCompatActivity implements EpisodesFragment.OnItemSelected<Item> {
    @BindView(R.id.player)
    LinearLayout mPlayer;
    @BindView(R.id.play)
    ImageView mPlay;
    @BindView(R.id.replay)
    ImageView mReplay;
    @BindView(R.id.forward)
    ImageView mForward;
    @BindView(R.id.ad)
    RelativeLayout mAdView;
    AdView adView;
    @BindView(R.id.seekbar)
    SeekBar mSeekBar;
    @BindView(R.id.current_time)
    TextView mCurrentTime;
    @BindView(R.id.remaining_time)
    TextView mRemainingTime;

    private MediaService mService;
    private boolean mBound = false;
    private final static int ONE_SECOND_IN_MS = 1000;
    private final static String APP_CENTER_ID = "4933507b-9621-4fe6-87c6-150a352d7f47";
    private final static String AD_ID = "ca-app-pub-6967310132431626/8150044399";
    private final static Channel mChannel = new Channel(
            "Tell 'Em Steve-Dave",
            "https://feeds.feedburner.com/TellEmSteveDave"
    );
    private Runnable mUpdateProgress;
    private Handler mHandler = new Handler();

    @Override
    protected void onStart() {
        super.onStart();

        final Intent intent = new Intent(this, MediaService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(connection);
        mBound = false;
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            final MediaService.MediaServiceBinder binder = (MediaService.MediaServiceBinder) service;
            mService = binder.getService();
            mService.setPlaybackListener(new MediaService.IPlaybackListener() {
                @Override
                public void onStartPlayback() {
                    mPlay.setImageDrawable(getDrawable(R.drawable.baseline_pause_black_18dp));
                    mSeekBar.setMax(mService.getDuration());
                    mPlayer.setVisibility(View.VISIBLE);

                    // @todo: dispose of timer
                    // @todo: worth backgrounding?
                    mUpdateProgress = () -> {
                        updateSeekProgress();
                        mHandler.postDelayed(mUpdateProgress, ONE_SECOND_IN_MS);
                    };
                    runOnUiThread(mUpdateProgress);
                }

                @Override
                public void onStopPlayback() {
                    mPlay.setImageDrawable(getDrawable(R.drawable.round_play_arrow_black_18dp));
                }

                @Override
                public void onCompletion() {
                    mSeekBar.setMax(0);
                    mSeekBar.setProgress(0);
                    mPlay.setImageDrawable(getDrawable(R.drawable.round_play_arrow_black_18dp));
                }
            });
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCenter.start(getApplication(), APP_CENTER_ID, Analytics.class, Crashes.class);
        setContentView(R.layout.activity_main_layout);
        ButterKnife.bind(this);

        if (!BuildConfig.DEBUG) {
            initializeAds();
        }

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mService != null) {
                    mService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (savedInstanceState == null) {
            final FragmentManager fm = getSupportFragmentManager();
            final Fragment fragment = EpisodesFragment.create(mChannel);

            fm.beginTransaction()
                    .add(R.id.coordinator_layout, fragment, EpisodesFragment.TAG)
                    .commit();
        }
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof EpisodesFragment) {
            final EpisodesFragment binderRecyclerFragment = (EpisodesFragment) fragment;
            binderRecyclerFragment.setOnItemSelectedListener(this);
        }
    }

    @OnClick(R.id.play)
    public final void onTogglePlaybackClick(@NonNull View view) {
        if (mBound) {
            if (this.mService.isPlaying()) {
                this.mService.pausePlayback();
            } else {
                this.mService.resumePlayback();
            }
        }
    }

    @OnClick(R.id.forward)
    public final void onForwardClick(@NonNull View view) {
        if (mBound) {
            this.mService.forward();
            this.updateSeekProgress();
        }
    }

    @OnClick(R.id.replay)
    public final void onRewindClick(@NonNull View view) {
        if (mBound) {
            this.mService.rewind();
            this.updateSeekProgress();
        }
    }

    private void updateSeekProgress() {
        if (!mBound) {
            return;
        }

        mSeekBar.setProgress(this.mService.getCurrentTime());
        TextViewKt.setElapsedTime(mCurrentTime, this.mService.getCurrentTime());
        TextViewKt.setElapsedTime(mRemainingTime, this.mService.getRemainingTime());
    }

    private void initializeAds() {
        mAdView = findViewById(R.id.ad);
        adView = new AdView(this);
        adView.setAdUnitId(AD_ID);
        mAdView.addView(adView);

        final RequestConfiguration configuration = new RequestConfiguration.Builder()
                .setTestDeviceIds(Collections.singletonList(AdRequest.DEVICE_ID_EMULATOR))
                .build();

        MobileAds.setRequestConfiguration(configuration);
        MobileAds.initialize(this);

        final AdRequest adRequest = new AdRequest.Builder().build();
        final AdSize adSize = getAdSize();

        adView.setAdSize(adSize);
        adView.loadAd(adRequest);
    }

    @NonNull
    private AdSize getAdSize() {
        final Display display = getWindowManager().getDefaultDisplay();
        final DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        final float widthPixels = outMetrics.widthPixels;
        final float density = outMetrics.density;

        final int adWidth = (int) (widthPixels / density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }


    @Override
    public final void onItemSelected(@NonNull Item item) {
        if (!mBound) {
            return;
        }

        mService.startPlayback(item.getUri());
    }
}
