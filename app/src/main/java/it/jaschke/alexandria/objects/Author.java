package it.jaschke.alexandria.objects;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kyleparker on 9/14/2015.
 */
public class Author implements Parcelable, Comparable<Author> {
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String imageUrl) {
        this.name = imageUrl;
    }

    public Author() { }

    private Author(Parcel in) {
        id = in.readString();
        name = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
    }

    public static final Creator<Author> CREATOR = new Creator<Author>() {
        @Override
        public Author createFromParcel(Parcel in) {
            return new Author(in);
        }

        @Override
        public Author[] newArray(int size) {
            return new Author[size];
        }
    };

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }

        if (obj == null) { return false; }

        if (getClass() != obj.getClass()) { return false; }

        Author other = (Author) obj;

        if (id == null) {
            if (other.id != null) { return false; }
        } else if (!id.equals(other.id)) { return false; }

        if (name == null) {
            if (other.name != null) { return false; }
        } else if (!name.equals(other.name)) { return false; }

        return true;
    }

    public int compareTo(Author another) {
        if (another == null) return 1;
        return another.id.compareTo(id);
    }
}
