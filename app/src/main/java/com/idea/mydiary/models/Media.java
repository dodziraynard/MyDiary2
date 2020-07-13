package com.idea.mydiary.models;

public class Media {
    MediaType mMediaType;
    String mUrl;

    public Media(String url, MediaType mediaType) {
        mMediaType = mediaType;
        mUrl = url;
    }

    public MediaType getMediaType() {
        return mMediaType;
    }

    public String getUrl() {
        return mUrl;
    }
}
