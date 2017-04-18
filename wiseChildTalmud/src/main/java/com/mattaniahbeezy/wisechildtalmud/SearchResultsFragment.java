package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Mattaniah on 6/24/2015.
 */
public class SearchResultsFragment extends Fragment {
    HostActivity hostActivity;
    SearchUtil.Query query;
    Activity context;
    List<SearchUtil.Result> resultList;
    ViewFactory viewFactory;
    View mainView;
    RecyclerView resultsContainer;

    public static SearchResultsFragment getInstance(List<SearchUtil.Result> resultList, SearchUtil.Query query) {
        SearchResultsFragment fragment = new SearchResultsFragment();
        fragment.resultList = resultList;
        fragment.query = query;
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.hostActivity = (HostActivity) activity;
        this.context = activity;
        viewFactory = new ViewFactory(activity);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainView = (CoordinatorLayout) inflater.inflate(R.layout.bookmarks_fragment_layout, null);
        hostActivity.getToolbar().setTitle(query.searchTerm + " " + resultList.size() + " results");
        hostActivity.getToolbar().findViewById(R.id.rightDrawerToggle).setVisibility(View.GONE);
        hostActivity.getToolbar().findViewById(R.id.toolbarSearchView).setVisibility(View.GONE);
        hostActivity.getTablayout().removeAllTabs();
        hostActivity.getTablayout().setVisibility(View.GONE);
        resultsContainer = (RecyclerView) mainView.findViewById(R.id.bookmarksContainer);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        resultsContainer.setLayoutManager(linearLayoutManager);
        resultsContainer.setAdapter(new ResultsViewAdapter());
        resultsContainer.setHasFixedSize(true);
        mainView.findViewById(R.id.deleteAll).setVisibility(View.GONE);
        return mainView;
    }

    private class ResultsViewAdapter extends RecyclerView.Adapter<ResultsViewAdapter.ViewHolder> {

        public ResultsViewAdapter() {
        }


        public class ViewHolder extends RecyclerView.ViewHolder {
            View view;
            TextView blurb;
            TextView header;
            View divider;

            public ViewHolder(View v) {
                super(v);
                this.view = v;
                header = (TextView) view.findViewById(R.id.searchResultTitle);
                blurb = (TextView) view.findViewById(R.id.searchResultBlurb);
                divider = view.findViewById(R.id.divider);
                view.setBackground(viewFactory.getListViewIemBackground());
                blurb.setTextColor(viewFactory.getListViewTextColor());
                blurb.setGravity(query.text == ViewFactory.Text.ENGLISH ? Gravity.LEFT : Gravity.RIGHT);
            }
        }

        @Override
        public ResultsViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(View.inflate(getActivity(), R.layout.searchresultitem, null));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            final Tractate tractate = (Tractate) resultList.get(position).tractate;
            holder.divider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            holder.header.setText(tractate.getName() + " " + tractate.getAmudName(tractate.getCellNumber()));
            holder.blurb.setText(resultList.get(position).resultString);
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hostActivity.setBodyFragment(tractate, false);
                }
            });
        }

        @Override
        public int getItemCount() {
            return resultList.size();
        }
    }
}
