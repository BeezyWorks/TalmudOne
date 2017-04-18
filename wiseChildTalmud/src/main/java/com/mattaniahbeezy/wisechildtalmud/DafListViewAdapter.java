package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.sourceforge.zmanim.hebrewcalendar.HebrewDateFormatter;

/**
 * Created by Mattaniah on 6/11/2015.
 */
public class DafListViewAdapter extends ArrayAdapter {
    Activity context;
    BodyFragment hostFragment;
    HostActivity hostActivity;
    Tractate tractate;
    HebrewDateFormatter hFat = new HebrewDateFormatter();
    BookmarkUtil bookmarkUtil;
    NotesUtil notesUtil;

    public DafListViewAdapter(BodyFragment hostFragment, Tractate tractate) {
        super(hostFragment.getActivity(), R.layout.daf_row, tractate.getCellCount());
        this.tractate = tractate;
        this.context = hostFragment.getActivity();
        this.hostActivity = (HostActivity) context;
        this.hostFragment=hostFragment;
        hFat.setUseGershGershayim(false);
        this.bookmarkUtil = new BookmarkUtil(context, tractate.getIndex());
        this.notesUtil = new NotesUtil(context, tractate.getIndex());
    }

    @Override
    public int getCount() {
        return tractate.getCellCount() + 3;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = View.inflate(context, R.layout.daf_row, null);
        ViewFactory viewFactory = new ViewFactory(context);
        TextView label = (TextView) view.findViewById(R.id.dafLabel);
        final ImageView bookmarkIndicator = (ImageView) view.findViewById(R.id.bookmarkIndicator);
        ImageView noteIndicator = (ImageView) view.findViewById(R.id.noteIndicator);

        createBookmarkIndicator(bookmarkIndicator, position);
        createNotesIndicator(noteIndicator, position);

        label.setText(tractate.getAmudName(position));

        view.setBackground(viewFactory.getListViewIemBackground());
        label.setTextColor(viewFactory.getListViewTextColor());
        return view;
    }

    private void createBookmarkIndicator(final ImageView bookmarkIndicator, final int cellNumber) {
        boolean isCellBookmarked = bookmarkUtil.isCellBookmarked(cellNumber);
        bookmarkIndicator.setImageResource(isCellBookmarked ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_outline);
        final int defaultColor = new ViewFactory(context).isStyleDark() ? Color.WHITE : context.getResources().getColor(android.R.color.darker_gray);
        final int highlightColor=new ViewFactory(context).getPrimaryHighlightColor();
        bookmarkIndicator.setColorFilter(isCellBookmarked ? highlightColor : defaultColor);
        bookmarkIndicator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookmarkUtil.toggleBookmark(cellNumber);
                boolean isCellBookmarked = bookmarkUtil.isCellBookmarked(cellNumber);
                bookmarkIndicator.setImageResource(isCellBookmarked ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_outline);
                bookmarkIndicator.setColorFilter(isCellBookmarked ? highlightColor : defaultColor);
            }
        });
    }

    private void createNotesIndicator(final ImageView notesIndicator, final int cellNumber) {
        boolean isNoteExist = notesUtil.isCellHaveNote(cellNumber);
        notesIndicator.setColorFilter(isNoteExist ? context.getResources().getColor(R.color.primary) : context.getResources().getColor(android.R.color.darker_gray));
        notesIndicator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hostActivity.showNoteDialog(notesUtil, cellNumber);
                hostFragment.closeDrawer();
            }
        });
    }
}
