package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mattaniah on 6/22/2015.
 */
public class BookmarksFragment extends Fragment {
    HostActivity hostActivity;
    CoordinatorLayout mainView;
    ViewFactory viewFactory;
    RecyclerView bookmarksContainer;
    BookmarksViewAdapter adapter;
    List<BookmarkUtil> bookmarkUtils = new ArrayList<>();
    List<Tractate> bookmarks = new ArrayList<>();


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.hostActivity = (HostActivity) activity;
        viewFactory = new ViewFactory(activity);
        viewFactory.setOverrideStyle(ViewFactory.Style.DAY);
    }


    public View getHeaderView(String headerTitle) {
        TextView textView = new TextView(getActivity());
        textView.setTextColor(getActivity().getResources().getColor(R.color.primary));
        textView.setTextSize(18);
        textView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        textView.setText(headerTitle);
        int padding = (int) getResources().getDimension(R.dimen.padding_left_and_right);
        textView.setPadding(padding, 0, padding, 0);
        return textView;
    }

    private void adjustDecor() {
        hostActivity.getToolbar().setTitle("Bookmarks");
        hostActivity.getToolbar().findViewById(R.id.rightDrawerToggle).setVisibility(View.GONE);
        hostActivity.getToolbar().findViewById(R.id.toolbarSearchView).setVisibility(View.GONE);
        hostActivity.getTablayout().removeAllTabs();
        hostActivity.getTablayout().setVisibility(View.GONE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainView = (CoordinatorLayout) inflater.inflate(R.layout.bookmarks_fragment_layout, null);
        adjustDecor();
        createBookmarksContainer();
        mainView.findViewById(R.id.deleteAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAllBookmarks();
            }
        });
        return mainView;
    }

    private void createBookmarksContainer() {
        bookmarksContainer = (RecyclerView) mainView.findViewById(R.id.bookmarksContainer);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        bookmarksContainer.setLayoutManager(linearLayoutManager);
        createLayouts();
    }

    private void createLayouts() {
        bookmarkUtils = BookmarkUtil.getAllBookmarks(getActivity());
        adapter = new BookmarksViewAdapter();
        if (bookmarksContainer != null)
            bookmarksContainer.setAdapter(adapter);
    }

    private void deleteAllBookmarks() {
        new AlertDialog.Builder(getActivity())
                .setMessage("Delete all bookmarks?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final List<String> deletedList = new ArrayList<>();
                        for (BookmarkUtil util : bookmarkUtils) {
                            deletedList.add(util.getJSONAsString());
                            util.deleteAllBookmarks();
                        }
                        adapter.mDataSet.removeAll(adapter.mDataSet);
                        adapter.notifyDataSetChanged();
                        Snackbar.make(mainView, "All Bookmarks Deleted", Snackbar.LENGTH_LONG)
                                .setAction("Undo", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        for (int i = 0; i < bookmarkUtils.size(); i++) {
                                            BookmarkUtil util = bookmarkUtils.get(i);
                                            util.restoreBookmarks(deletedList.get(i));
                                        }
                                        adapter = new BookmarksViewAdapter();
                                        bookmarksContainer.setAdapter(adapter);
                                    }
                                })
                                .show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


    @Override
    public void onPause() {
        hostActivity.updateBookmarks();
        super.onPause();
    }

    public class BookmarksViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        List<Object> mDataSet = new ArrayList<>();
        private static final int TITLE_VIEW = 0;
        private static final int BOOKMARK_VIEW = 1;

        public BookmarksViewAdapter() {
            for (BookmarkUtil util : bookmarkUtils) {
                mDataSet.add(Tractate.masechtosBavli[util.tractateIndex]);
                mDataSet.addAll(util.getAllBookmarks());
            }
        }

        private abstract class BookmarksHolders extends RecyclerView.ViewHolder {
            int position;

            public BookmarksHolders(View itemView) {
                super(itemView);
            }

            public abstract void setPosition(int position);
        }


        public class BookmarkViewHolder extends BookmarksHolders implements View.OnClickListener {
            View view;
            TextView label;
            ImageView deleteButton;
            Tractate tractate;


            public BookmarkViewHolder(View v) {
                super(v);
                this.view = v;
                label = (TextView) view.findViewById(R.id.dafLabel);
                deleteButton = (ImageView) view.findViewById(R.id.deleteButton);
                view.setBackground(viewFactory.getListViewIemBackground());
                label.setTextColor(viewFactory.getListViewTextColor());

            }

            @Override
            public void setPosition(int position) {
                this.position = position;
                this.tractate = (Tractate) mDataSet.get(position);
                label.setText(tractate.getAmudName(tractate.getCellNumber()));
                view.setOnClickListener(this);
                deleteButton.setOnClickListener(this);
            }


            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.bookmark_row:
                        if (tractate != null)
                            hostActivity.setBodyFragment(tractate);
                        break;
                    case R.id.deleteButton:
                        delete();
                        break;
                }
            }

            private void delete() {
                removeItem(position);
                BookmarkUtil.deleteBookmark(getActivity(), tractate);
                Snackbar.make(mainView, "Bookmark Deleted", Snackbar.LENGTH_LONG)
                        .setAction("undo", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                BookmarkUtil.addBookmark(getActivity(), tractate);
                                addItem(tractate, position);
                            }
                        })
                        .show();
            }
        }

        public class TitleViewHolder extends BookmarksHolders {
            private TextView textView;
            View divider;

            public TitleViewHolder(View v) {
                super(v);
                v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                divider = v.findViewById(R.id.divider);
                textView = (TextView) v.findViewById(R.id.tractateTitle);
            }

            public void setPosition(int position) {
                divider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
                textView.setText((String) mDataSet.get(position));
            }

        }

        @Override
        public int getItemViewType(int position) {
            if (mDataSet.get(position) instanceof String)
                return TITLE_VIEW;
            return BOOKMARK_VIEW;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case TITLE_VIEW:
                    return new TitleViewHolder(View.inflate(getActivity(), R.layout.bookmark_header, null));
                case BOOKMARK_VIEW:
                    return new BookmarkViewHolder(View.inflate(getActivity(), R.layout.bookmark_row, null));
            }
            return new BookmarkViewHolder(View.inflate(getActivity(), R.layout.daf_row, null));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            ((BookmarksHolders) holder).setPosition(position);
        }

        public void removeItem(int position) {
            mDataSet.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(0, mDataSet.size());
        }

        public void addItem(Tractate tractate, int position) {
            mDataSet.add(position, tractate);
            notifyItemInserted(position);
            notifyItemRangeChanged(0, mDataSet.size());
        }

        @Override
        public int getItemCount() {
            return mDataSet.size();
        }
    }

}
