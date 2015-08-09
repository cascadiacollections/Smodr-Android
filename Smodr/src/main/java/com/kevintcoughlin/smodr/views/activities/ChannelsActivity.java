package com.kevintcoughlin.smodr.views.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.kevintcoughlin.smodr.R;
import com.kevintcoughlin.smodr.SmodrApplication;
import com.kevintcoughlin.smodr.models.Channel;
import com.kevintcoughlin.smodr.views.fragments.ChannelsFragment;
import com.kevintcoughlin.smodr.views.fragments.EpisodesFragment;

public final class ChannelsActivity extends AppCompatActivity implements ChannelsFragment.Callbacks {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channels_view_layout);

	    final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
	    setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            final FragmentManager fm = getSupportFragmentManager();
            final ChannelsFragment fragment = new ChannelsFragment();
            fm.beginTransaction()
                    .add(R.id.channels_container, fragment, ChannelsFragment.TAG)
                    .commit();
        }
    }

    @Override
    public void onChannelSelected(@NonNull final Channel channel) {
        trackChannelSelected(channel);
	    final EpisodesFragment fragment = new EpisodesFragment();
	    final Bundle args = new Bundle();
	    args.putString(EpisodesFragment.ARG_CHANNEL_NAME, channel.getShortName());
	    fragment.setArguments(args);
	    getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.channels_container, fragment)
            .addToBackStack(ChannelsFragment.TAG)
            .commit();
    }

    @Override
    public void setTitle(CharSequence title) {
	    if (getActionBar() != null) {
            getActionBar().setTitle(title);
        }
    }

    @Override
    public boolean onNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return true;
    }

    private void trackChannelSelected(@NonNull final Channel channel) {
        final Tracker t = ((SmodrApplication) getApplication()).getTracker(
                SmodrApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("CHANNEL")
                .setAction("SELECTED")
                .setLabel(channel.getShortName())
                .build());
    }
}
