package com.kevintcoughlin.smodr.views.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cascadiacollections.jamoka.adapter.BinderRecyclerAdapter;
import com.cascadiacollections.jamoka.fragment.BinderRecyclerFragment;
import com.kevintcoughlin.smodr.database.AppDatabase;
import com.kevintcoughlin.smodr.models.Channel;
import com.kevintcoughlin.smodr.models.Feed;
import com.kevintcoughlin.smodr.models.Item;
import com.kevintcoughlin.smodr.services.FeedService;
import com.kevintcoughlin.smodr.viewholders.EpisodeView;
import com.kevintcoughlin.smodr.viewholders.EpisodeViewHolder;
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory;

import org.jetbrains.annotations.Contract;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public final class EpisodesFragment extends BinderRecyclerFragment<Item, EpisodeViewHolder> implements Callback<Feed> {
    private static final String EPISODE_FEED_URL = "com.com.kevintcoughlin.smodr.views.fragments.EpisodesFragment.feedUrl";
    private static final String BASE_URL = "https://www.smodcast.com/";
    private FeedService mFeedService;

    @NonNull
    public static Fragment create(@NonNull Channel channel) {
        final Fragment fragment = new EpisodesFragment();
        final Bundle bundle = new Bundle();
        bundle.putString(EPISODE_FEED_URL, channel.getLink());
        fragment.setArguments(bundle);
        return fragment;
    }
    
    public void markCompleted(Item item) {
        mAdapter.markCompleted(item);
    }

    @Override
    public boolean onLongClick(Item item) {
        return true;
    }

    private static final class ItemAdapter extends BinderRecyclerAdapter<Item, EpisodeViewHolder> {
        ItemAdapter() {
            super(new EpisodeView());
        }

        void markCompleted(Item item) {
            this.updateItem(item);
        }

        void updateItem(Item item) {
            final int index = items.indexOf(item);
            final Item newItem = Item.create(item, true);

            items.set(index, newItem);
            notifyItemChanged(index);
        }

        @Override
        public void onBindViewHolder(@NonNull EpisodeViewHolder viewHolder, int i) {
            super.onBindViewHolder(viewHolder, i);
        }
    }

    @NonNull
    public static final String TAG = EpisodesFragment.class.getSimpleName();

    @NonNull
    private final ItemAdapter mAdapter = new ItemAdapter();

    @NonNull
    private final LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getContext());

    @Contract(pure = true)
    @NonNull
    @Override
    protected BinderRecyclerAdapter<Item, EpisodeViewHolder> getAdapter() {
        return mAdapter;
    }

    @Contract(pure = true)
    @NonNull
    @Override
    protected LinearLayoutManager getLayoutManager() {
        return mLinearLayoutManager;
    }

    @Override
    public void onRefresh() {
        super.onRefresh();

        fetchEpisodes();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(
                getRecyclerView().getContext(),
                getLayoutManager().getOrientation()
        );
        getRecyclerView().addItemDecoration(mDividerItemDecoration);

        final Item[] items = AppDatabase.getData(getContext());
        mAdapter.setItems(items);
        fetchEpisodes();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResponse(@NonNull final Call<Feed> call, @NonNull final Response<Feed> response) {
        final Feed feed = response.body();

        if (feed != null && feed.getChannel() != null) {
            final List<Item> items = feed.getChannel().getItem();
            AppDatabase.insertData(getContext(), items);
            final Item[] dbItems = AppDatabase.getData(getContext());
            mAdapter.setItems(dbItems);
            stopRefreshing();
        }
    }

    @Override
    public void onFailure(@NonNull final Call<Feed> call, @NonNull final Throwable t) {
        Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void fetchEpisodes() {
        if (mFeedService == null) {
            initializeFeedService();
        }

        final Bundle arguments = getArguments();

        if (arguments != null) {
            final String feedUrl = arguments.getString(EPISODE_FEED_URL);

            if (feedUrl != null) {
                mFeedService.feed(feedUrl).enqueue(this);
            }
        }
    }

    private void initializeFeedService() {
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(TikXmlConverterFactory.create())
                .build();

        mFeedService = retrofit.create(FeedService.class);
    }
}
