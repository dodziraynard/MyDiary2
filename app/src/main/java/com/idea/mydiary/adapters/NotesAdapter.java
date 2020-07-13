package com.idea.mydiary.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.idea.mydiary.NewNoteActivity;
import com.idea.mydiary.R;
import com.idea.mydiary.models.Note;

import java.util.List;

import static com.idea.mydiary.MainActivity.SELECTED_NOTE_ID;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> implements View.OnCreateContextMenuListener {

    private static final int MENU_ORDER_0 = 0;
    private static final int MENU_ORDER_1 = 1;
    private static final int MENU_ORDER_2 = 2;
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final List<Note> mNotes;
    private long adapterPosition = -1;

    public NotesAdapter(Context context, List<Note> notes) {
        mContext = context;
        mNotes = notes;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @NonNull
    @Override
    public NotesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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

        holder.mNoteCard.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setAdapterPosition(holder.getAdapterPosition());
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mNotes.size();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, v.getId(), MENU_ORDER_0, mContext.getString(R.string.string_view));
        menu.add(0, v.getId(), MENU_ORDER_1, mContext.getString(R.string.string_share));
        menu.add(0, v.getId(), MENU_ORDER_2, mContext.getString(R.string.string_delete));
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.mNoteCard.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    public void setAdapterPosition(long adapterPosition) {
        this.adapterPosition = adapterPosition;
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

            mNoteCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                Intent intent = new Intent(mContext, NewNoteActivity.class);
                intent.putExtra(SELECTED_NOTE_ID, getAdapterPosition());
                mContext.startActivity(intent);
            }
        });
        }
    }
}
