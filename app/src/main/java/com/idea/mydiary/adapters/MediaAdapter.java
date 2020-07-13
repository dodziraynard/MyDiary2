package com.idea.mydiary.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.MediaType;
import com.idea.mydiary.R;

import java.lang.ref.WeakReference;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

    public final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private List<Media> mMediaList;
    private OnItemClickListener listener;

    public MediaAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    public interface OnItemClickListener {
        void onImageButtonClickListener(int position);
        void onAudioButtonClickListener(String url, int position);
    }

    public void setOnImageButtonClickListener(OnItemClickListener listener){
        this.listener = listener;
    }

    @NonNull
    @Override
    public MediaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mLayoutInflater.inflate(R.layout.media_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final MediaAdapter.ViewHolder holder, int position) {
        Media media = mMediaList.get(position);
        if (media.getMediaType() == MediaType.IMAGE) {
            new DecodeFileTask(holder).execute(media);
        } else {
            holder.mImageButton.setImageResource(R.drawable.ic_music_note);
            holder.mImageButton.setColorFilter(mContext.getColor(R.color.contrastTextColor));
        }
    }

    public void setMediaList(List<Media> mediaList){
        this.mMediaList = mMediaList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (mMediaList == null) return 0;
        return mMediaList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageButton mImageButton;
        private Media mMedia;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageButton = itemView.findViewById(R.id.imageButton);
            mImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMedia = mMediaList.get(getLayoutPosition());
                    if(listener != null){
                        if(mMedia.getMediaType() == MediaType.IMAGE){
                            listener.onImageButtonClickListener(getLayoutPosition());
                        } else{
                            listener.onAudioButtonClickListener(mMedia.getUrl(), getLayoutPosition());
                        }
                    }
                }
            });
        }
    }

    private static class DecodeFileTask extends AsyncTask<Media, Void, Bitmap> {

        private final WeakReference<ViewHolder> mViewHolderWeakReference;

        DecodeFileTask(ViewHolder holder) {
            mViewHolderWeakReference = new WeakReference<>(holder);
        }

        @Override
        protected Bitmap doInBackground(Media... mediaList) {
            Media media = mediaList[0];
            return BitmapFactory.decodeFile(media.getUrl());
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ViewHolder holder = mViewHolderWeakReference.get();
            holder.mImageButton.setImageBitmap(bitmap);
        }
    }
}
