package com.github.andarb.simplyreddit;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.github.andarb.simplyreddit.adapters.PostAdapter;
import com.github.andarb.simplyreddit.data.Post;
import com.github.andarb.simplyreddit.database.AppDatabase;
import com.github.andarb.simplyreddit.models.PostsViewModel;
import com.github.andarb.simplyreddit.models.PostsViewModelFactory;
import com.github.andarb.simplyreddit.utils.PostPullService;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A new instance of this fragment is used for each page of the ViewPager.
 * A list of most popular, hot or new posts from all subreddits is downloaded and displayed here.
 */
public class ViewPagerFragment extends Fragment {

    private static final String ARG_PAGE = "com.github.andarb.simplyreddit.arg.PAGE";

    private Context mContext;
    private String mPage;
    private PostAdapter mAdapter;
    private AppDatabase mDb;
    private Unbinder mButterknifeUnbinder;

    @BindView(R.id.posts_recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.post_list_pb)
    ProgressBar mProgressBar;

    /* Required empty public constructor */
    public ViewPagerFragment() {

    }

    /* Create a new instance of this fragment with a viewpager page number argument*/
    public static ViewPagerFragment newInstance(String category) {
        ViewPagerFragment fragment = new ViewPagerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE, category);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_list, container, false);
        mButterknifeUnbinder = ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getArguments() != null) {
            // Set viewpager page that needs to be loaded
            mPage = getArguments().getString(ARG_PAGE);
        }
        mContext = getActivity();
        mDb = AppDatabase.getDatabase(mContext.getApplicationContext());

        // Setup recyclerview adapter
        mAdapter = new PostAdapter(mContext);
        mRecyclerView.setLayoutManager(
                new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);

        // Setup viewmodel for adapter data
        PostsViewModelFactory factory = new PostsViewModelFactory(mDb, mPage);
        PostsViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(PostsViewModel.class);
        viewModel.getPosts().observe(this, new Observer<List<Post>>() {
            @Override
            public void onChanged(@Nullable List<Post> posts) {
                mAdapter.setPosts(posts);
                mAdapter.notifyDataSetChanged();
            }
        });

        // On configuration change retrieve posts from ViewModel. Otherwise, make a network call
        if (savedInstanceState == null) {
            refreshPage();
        }
    }

    /* Pull new data from the internet */
    public void refreshPage() {
        mProgressBar.setVisibility(View.VISIBLE);
        Intent intent = new Intent(mContext, PostPullService.class);
        intent.putExtra(PostPullService.EXTRA_CATEGORY, mPage);
        mContext.startService(intent);
    }

    /* Invoked from MainActivity after it receives a broadcast confirming data retrieval */
    public void hideProgressBar() {
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mButterknifeUnbinder.unbind();
    }
}