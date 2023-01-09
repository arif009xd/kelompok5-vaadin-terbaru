package com.example.application.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;

@Entity
public class Category extends AbstractEntity {

    private String nameCategory;
    private String slugProduct;
    private String totalProduct;
    @Lob
    @Column(length = 1000000)
    private byte[] thumbnailSlug;

    public String getNameCategory() {
        return nameCategory;
    }
    public void setNameCategory(String nameCategory) {
        this.nameCategory = nameCategory;
    }
    public String getSlugProduct() {
        return slugProduct;
    }
    public void setSlugProduct(String slugProduct) {
        this.slugProduct = slugProduct;
    }
    public String getTotalProduct() {
        return totalProduct;
    }
    public void setTotalProduct(String totalProduct) {
        this.totalProduct = totalProduct;
    }
    public byte[] getThumbnailSlug() {
        return thumbnailSlug;
    }
    public void setThumbnailSlug(byte[] thumbnailSlug) {
        this.thumbnailSlug = thumbnailSlug;
    }

}
