package com.bignerdranch.android.photogallery.model;

import java.util.ArrayList;
import java.util.List;

/**
 * in Gson the name of the FIELDS in the class should match the ones in the
 * JSON string.
 * Otherwise use the SerializedName annotation
 */
public class Gallery {

    private Photos photos;

    public Photos getPhotos() {
        return photos;
    }

    public void setPhotos(Photos photos) {
        this.photos = photos;
    }

    public List<GalleryItem> getLegacyItems(){
        List<GalleryItem> items = new ArrayList<>();

        for (Photo photo : photos.getPhoto()) {
            GalleryItem item = new GalleryItem();
            item.setId(photo.getId());
            item.setCaption(photo.getTitle());
            item.setUrl(photo.getUrl_s());
            items.add(item);
        }

        return items;
    }


    public class Photos {

        private List<Photo> photo;

        public List<Photo> getPhoto() {
            return photo;
        }

        public void setPhoto(List<Photo> photo) {
            this.photo = photo;
        }
    }

    public class Photo {

        private String id;
        private String title;
        private String url_s;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl_s() {
            return url_s;
        }

        public void setUrl_s(String url_s) {
            this.url_s = url_s;
        }
    }

}
