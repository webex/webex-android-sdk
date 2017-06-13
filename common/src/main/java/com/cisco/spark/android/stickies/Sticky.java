package com.cisco.spark.android.stickies;


import android.net.Uri;


public class Sticky {

    // Is MD5 hash of image file
    private String id;

    // Optional. Free form text available for human readable description. Set on the service end
    //
    // Note: The description is in US English and comes as part of the Sticky object from the Stickies service.
    //       If this needs to be localized, some non-trivial work will need to be done on the service end.
    private String description;

    // Actual location of image
    private Uri location;

    public Sticky() {
    }

    public static Sticky createSticky(String id, String description, Uri location) {
        Sticky sticky = null;

        if (id != null && location != null) {
            sticky = new Sticky();
            sticky.setDescription(description);
            sticky.setId(id);
            sticky.setLocation(location);
        }

        return sticky;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Uri getLocation() {
        return location;
    }

    public void setLocation(Uri location) {
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sticky sticky = (Sticky) o;

        if (!id.equals(sticky.id)) return false;
        return location.equals(sticky.location);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + location.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Sticky{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", location=" + location +
                '}';
    }
}
