package it.jaschke.alexandria.objects;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kyleparker on 9/14/2015.
 */
public class Book implements Parcelable, Comparable<Book> {
    private boolean deleted;
    private String desc;
    private String id;
    private String imageUrl;
    private String subtitle;
    private String title;
    private String url;

    public Author author = new Author();
    public Category category = new Category();

    public boolean getDeleted() {
        return deleted;
    }
    public void setDeleted(boolean value) {
        this.deleted = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Book() { }

    private Book(Parcel in) {
        deleted = in.readByte() == 1;
        desc = in.readString();
        id = in.readString();
        imageUrl = in.readString();
        subtitle = in.readString();
        title = in.readString();
        url = in.readString();

        ClassLoader classLoader = getClass().getClassLoader();
        author = in.readParcelable(classLoader);
        category = in.readParcelable(classLoader);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (deleted ? 1 : 0));
        dest.writeString(desc);
        dest.writeString(id);
        dest.writeString(imageUrl);
        dest.writeString(subtitle);
        dest.writeString(title);
        dest.writeString(url);

        dest.writeParcelable(author, 0);
        dest.writeParcelable(category, 0);
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + ((desc == null) ? 0 : desc.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((imageUrl == null) ? 0 : imageUrl.hashCode());
        result = prime * result + ((subtitle == null) ? 0 : subtitle.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }

        if (obj == null) { return false; }

        if (getClass() != obj.getClass()) { return false; }

        Book other = (Book) obj;

        if (desc == null) {
            if (other.desc != null) { return false; }
        } else if (!desc.equals(other.desc)) { return false; }

        if (id == null) {
            if (other.id != null) { return false; }
        } else if (!id.equals(other.id)) { return false; }

        if (imageUrl == null) {
            if (other.imageUrl != null) { return false; }
        } else if (!imageUrl.equals(other.imageUrl)) { return false; }

        if (subtitle == null) {
            if (other.subtitle != null) { return false; }
        } else if (!subtitle.equals(other.subtitle)) { return false; }

        if (title == null) {
            if (other.title != null) { return false; }
        } else if (!title.equals(other.title)) { return false; }

        if (url == null) {
            if (other.url != null) { return false; }
        } else if (!url.equals(other.url)) { return false; }

        return true;
    }

    public int compareTo(Book another) {
        if (another == null) return 1;
        return another.id.compareTo(id);
    }
}
