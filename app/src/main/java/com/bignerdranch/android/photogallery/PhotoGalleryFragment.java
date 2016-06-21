package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bignerdranch.android.photogallery.model.GalleryItem;
import com.bignerdranch.android.photogallery.network.FlickrFetchr;
import com.bignerdranch.android.photogallery.network.ThumbnailDownloader;
import com.bignerdranch.android.photogallery.view.OnVerticalScrollListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lmiceli on 07/06/2016.
 */
public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int mCurrentPage; // challenge

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mCurrentPage = 1;
        new FetchItemsTask().execute(mCurrentPage);

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);

        mThumbnailDownloader.setThumbnailDownloadListener(
            new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                @Override
                public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                    Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                    photoHolder.bindDrawable(drawable);
                }
            }
        );

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        mPhotoRecyclerView.addOnScrollListener(new OnVerticalScrollListener() {
            @Override
            public void onScrolledUp() {
                //
            }

            @Override
            public void onScrolledDown() {

            }

            @Override
            public void onScrolledToTop() {
//                if (mCurrentPage > 1) {
//                    mCurrentPage--;
//                    new FetchItemsTask().execute(mCurrentPage);
//                }
            }

            @Override
            public void onScrolledToBottom() {
                mCurrentPage++;
                new FetchItemsTask().execute(mCurrentPage);
            }
        });

        setupAdapter();

        return v;
    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            return new FlickrFetchr().fetchItems(params[0]);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            // mItems = items;
            // challenge pagination while adding data
            mItems.addAll(items);

            setupAdapter();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mImageView;

        public PhotoHolder(View view) {
            super(view);
            mImageView = (ImageView) view;
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);

            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem item = mGalleryItems.get(position);

            Drawable placeholder = getResources().getDrawable(android.R.drawable.ic_menu_gallery);
            holder.bindDrawable(placeholder);

            mThumbnailDownloader.queueThumbnail(holder, item.getUrl());
            mThumbnailDownloader.preloadCache(getItemsWhichShouldBePreloaded(position));
        }

        /**
         * for asking to cache images for smoother ui experience.
         * @param position
         * @return
         */
        @NonNull
        private List<GalleryItem> getItemsWhichShouldBePreloaded(int position) {
            // avoid calling lots of times for the first items
            // the idea was to call the previous and next 10 but that is too slow, many requests for no reason
            // plus the previous ones are already stored in cache.
            // we will call one by one as it goes smoother.
            // first ten are loaded already, no need to preload
            if (position % 7 == 0) {
                try {
                    return mItems.subList(position, Math.min(position + 14, mItems.size()-1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return new ArrayList<>();
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    /**
     * clean the view holders on rotation to avoid issues, this holders are invalid after rotation.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }
}
