package com.bignerdranch.android.photogallery;


import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * Created by cody on 3/24/2018.
 */


public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";
    private static final int images = 10;

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    private int mCurrentPage = 1;
    private int mFetchedPage = 0;
    private int mCurrentPosition = 0;

    private GridLayoutManager manager;


    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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
        mPhotoRecyclerView = (RecyclerView) v
                .findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        manager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();

        //GridLayoutManager manager = new GridLayoutManager(getActivity(),10);
        //mPhotoRecyclerView.setLayoutManager(manager);
       // PhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public  void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                //int firstpos = manager
                switch (newState){
                    case RecyclerView.SCROLL_STATE_IDLE:
                        Log.i(TAG, String.valueOf(mCurrentPage));

                        PhotoAdapter photoAdapter = (PhotoAdapter) mPhotoRecyclerView.getAdapter();
                        int start = manager.findLastVisibleItemPosition() +1;
                        int end1 = Math.min(start+10,photoAdapter.getItemCount()-1);
                        for (int i = start; i<end1; i++){
                            mThumbnailDownloader.preloadimage(photoAdapter.mGalleryItems.get(i).getUrl());
                            Log.i(TAG, String.valueOf(i));

                        }


                        start = manager.findLastVisibleItemPosition() - 1;
                        int end2 = Math.max(start-10,0);
                        for (int i = start; i>end2; i--){
                            mThumbnailDownloader.preloadimage(photoAdapter.mGalleryItems.get(i).getUrl());
                            Log.i(TAG, String.valueOf(i));

                        }

                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        mThumbnailDownloader.clearpreload();
                        break;

                }

                updatepage();

            }

        });

        setupAdapter();

        return v;
    }
    @Override  public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

        @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    private void setupAdapter() {
        if (isAdded()) {
            if (mItems!= null){
                mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));

            }else {
                mPhotoRecyclerView.setAdapter(null);

            }
            mPhotoRecyclerView.scrollToPosition(mCurrentPosition);
           /* mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
           @Override
            public  void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState){
                    case RecyclerView.SCROLL_STATE_IDLE:
                         manager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();

                        PhotoAdapter photoAdapter = (PhotoAdapter) mPhotoRecyclerView.getAdapter();
                        int start = manager.findLastVisibleItemPosition() +1;
                        int end1 = Math.min(start+10,photoAdapter.getItemCount()-1);
                        for (int i = start; i<end1; i++){
                            mThumbnailDownloader.preloadimage(photoAdapter.mGalleryItems.get(i).getUrl());
                            Log.i(TAG, String.valueOf(i));

                        }


                         start = manager.findLastVisibleItemPosition() - 1;
                        int end2 = Math.max(start-10,0);
                        for (int i = start; i>end2; i--){
                        mThumbnailDownloader.preloadimage(photoAdapter.mGalleryItems.get(i).getUrl());
                        Log.i(TAG, String.valueOf(i));

                    }

                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                      mThumbnailDownloader.clearpreload();
                        break;

                }
               //updatepage();

            }

            });*/
        }
    }
    private void updatepage(){
        if(manager.findLastVisibleItemPosition()== (manager.getItemCount()-1)&& mCurrentPage == mFetchedPage){
            mCurrentPosition = manager.findFirstVisibleItemPosition()+100;
            mCurrentPage++;
            Log.i(TAG, String.valueOf(mCurrentPage));

            new FetchItemsTask().execute(mCurrentPage);
        }
    }
    private class PhotoHolder extends RecyclerView.ViewHolder {
        private  ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);

            Bitmap cached = mThumbnailDownloader.getLruCache().get(galleryItem.getUrl());
            if (cached != null){
                photoHolder.bindDrawable(new BitmapDrawable(getResources(),cached));
            }else {
                Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
                photoHolder.bindDrawable(placeholder);
                mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
            }

        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer,Void,List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            return new FlickrFetchr().fetchItems();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {

            if (mItems == null){
                mItems = items;
            }else{
                if(items != null){
                    mItems.addAll(items);
                }
            }
            mFetchedPage++;

            setupAdapter();
        }

    }

}