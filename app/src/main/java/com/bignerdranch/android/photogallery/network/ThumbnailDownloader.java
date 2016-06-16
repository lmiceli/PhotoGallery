package com.bignerdranch.android.photogallery.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import com.bignerdranch.android.photogallery.model.GalleryItem;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_CACHE = 1;
    public static final int MAX_SIZE = 50;
    private Handler mRequestHandler;
    private Handler mCacheHandler;
    private LruCache<String, Bitmap> imageCache = new LruCache<>(MAX_SIZE);

    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
        mCacheHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_CACHE) {
                    List<GalleryItem> items = (List<GalleryItem>) msg.obj;

                    for (GalleryItem item : items) {
                        String url = item.getUrl();
                        Log.i(TAG, "Got a cache request for URL: " + url);
                        if (url == null) {
                            return;
                        }
                        try {
                            getBitmap(url);
                        } catch (IOException ioe) {
                            Log.e(TAG, "Error downloading image", ioe);
                        }
                    }
                }
            }
        };
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }

    public void preloadCache(List<GalleryItem> items) {

        mCacheHandler.obtainMessage(MESSAGE_CACHE, items)
                .sendToTarget();

    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mCacheHandler.removeMessages(MESSAGE_CACHE);
    }

    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                return;
            }

            final Bitmap bitmap = getBitmap(url);

            mResponseHandler.post(new Runnable() {
                public void run() {
                    if (mRequestMap.get(target) != url) {
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });

        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    private Bitmap getBitmap(String url) throws IOException {
        Bitmap bitmap = imageCache.get(url);

        if (bitmap == null) {
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            bitmap = BitmapFactory
                    .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);

            imageCache.put(url, bitmap);
            Log.i(TAG, "Bitmap created");
        }
        else {
            Log.i(TAG, "Found Bitmap in cache");
        }

        return bitmap;
    }
}
