package com.idea.mydiary.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.idea.mydiary.R;
import com.idea.mydiary.views.NoteViewActivity;
import com.idea.mydiary.models.Note;

import java.util.List;

import static com.idea.mydiary.views.MainActivity.SELECTED_NOTE_ID;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder>
        implements View.OnCreateContextMenuListener {

    public static final int MENU_EDIT = 0;
    public static final int MENU_EXPORT_PDF = 1;
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private List<Note> mNotes;
    private long adapterPosition = -1;
    private Note mRecentlyDeletedItem;
    private int mRecentlyDeletedItemPosition;
    private final Activity mActivity;
    private OnDeleteListener listener;
    private boolean mCanDelete = true;

    public NotesAdapter(Context context, Activity activity) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
        mActivity = activity;
    }

    public interface OnDeleteListener {
        void deleteNote(Note note);
    }

    public void setOnNoteDeleteListener(OnDeleteListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mLayoutInflater.inflate(R.layout.note_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final NotesAdapter.ViewHolder holder, int position) {
        Note note = mNotes.get(position);
        holder.mDay.setText(note.getDay());
        holder.mMonth.setText(note.getMonth());
        holder.mYear.setText(note.getYear());
        holder.mTitle.setText(note.getTitle());
        holder.mText.setText(note.getText());

        holder.mNoteCard.setOnCreateContextMenuListener(this);

        holder.mNoteCard.setOnLongClickListener(v -> {
            setAdapterPosition(holder.getLayoutPosition());
            return false;
        });
    }

    @Override
    public int getItemCount() {
        if (mNotes == null) return 0;
        return mNotes.size();
    }

    public Context getContext() {
        return mContext;
    }

    public void deleteItem(int position) {
        mRecentlyDeletedItem = mNotes.get(position);
        mRecentlyDeletedItemPosition = position;
        mNotes.remove(position);
        notifyItemRemoved(position);
        showUndoSnackbar();
    }

    private void showUndoSnackbar() {
        Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content), R.string.string_note_deleted,
                Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.snack_bar_undo, v -> undoDelete());
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                if (mCanDelete) {
                    listener.deleteNote(mRecentlyDeletedItem);
                }
            }
        });
        snackbar.show();
    }

    private void undoDelete() {
        mCanDelete = false;
        mNotes.add(mRecentlyDeletedItemPosition,
                mRecentlyDeletedItem);
        notifyItemInserted(mRecentlyDeletedItemPosition);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, v.getId(), MENU_EDIT, mContext.getString(R.string.string_edit));
        menu.add(0, v.getId(), MENU_EXPORT_PDF, mContext.getString(R.string.export_pdf));
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.mNoteCard.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    public void setAdapterPosition(long adapterPosition) {
        this.adapterPosition = adapterPosition;
    }

    public void setNotes(List<Note> notes, long removedPosition) {
        this.mNotes = notes;
        if (removedPosition > -1) {
            notifyItemRemoved((int) removedPosition);
        } else {
            notifyDataSetChanged();
        }
    }

    public long getAdapterPosition() {
        return adapterPosition;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mDay;
        private final TextView mMonth;
        private final TextView mYear;
        private final TextView mTitle;
        private final TextView mText;
        private final CardView mNoteCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mDay = itemView.findViewById(R.id.textViewDay);
            mMonth = itemView.findViewById(R.id.textViewMonth);
            mYear = itemView.findViewById(R.id.textViewYear);
            mTitle = itemView.findViewById(R.id.textViewNoteTitle);
            mText = itemView.findViewById(R.id.textViewNoteText);
            mNoteCard = itemView.findViewById(R.id.note_card);

            mNoteCard.setOnClickListener((View v) -> {
                Intent intent = new Intent(mContext, NoteViewActivity.class);
                intent.putExtra(SELECTED_NOTE_ID, mNotes.get(getLayoutPosition()).getId());
                mContext.startActivity(intent);
            });
        }
    }

    public static class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

        private NotesAdapter mAdapter;
        private Drawable icon;
        private final ColorDrawable background;


        public SwipeToDeleteCallback(NotesAdapter adapter) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            mAdapter = adapter;

            icon = ContextCompat.getDrawable(mAdapter.getContext(),
                    R.drawable.ic_delete_white);
            background = new ColorDrawable(ContextCompat.getColor(mAdapter.getContext(), R.color.error));
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getLayoutPosition();
            mAdapter.deleteItem(position);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            View itemView = viewHolder.itemView;
            int backgroundCornerOffset = 20;
            if (dX > 0) { // Swiping to the right
                background.setBounds(itemView.getLeft(), itemView.getTop(),
                        itemView.getLeft() + ((int) dX) + backgroundCornerOffset,
                        itemView.getBottom());

            } else if (dX < 0) { // Swiping to the left
                background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                        itemView.getTop(), itemView.getRight(), itemView.getBottom());
            } else { // view is unSwiped
                background.setBounds(0, 0, 0, 0);
            }
            background.draw(c);

            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + icon.getIntrinsicHeight();

            if (dX > 0) { // Swiping to the right
                int iconLeft = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
                int iconRight = itemView.getLeft() + iconMargin;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                background.setBounds(itemView.getLeft(), itemView.getTop(),
                        itemView.getLeft() + ((int) dX) + backgroundCornerOffset,
                        itemView.getBottom());
            } else if (dX < 0) { // Swiping to the left
                int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                int iconRight = itemView.getRight() - iconMargin;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                        itemView.getTop(), itemView.getRight(), itemView.getBottom());
            } else { // view is unSwiped
                background.setBounds(0, 0, 0, 0);
            }

            background.draw(c);
            icon.draw(c);
        }
    }
}
