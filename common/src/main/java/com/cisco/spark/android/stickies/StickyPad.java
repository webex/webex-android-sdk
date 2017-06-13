package com.cisco.spark.android.stickies;


import java.util.List;
import java.util.UUID;


public class StickyPad {

    private UUID id;
    private String description;
    private List<Sticky> stickies;

    public StickyPad() {
    }

    public static StickyPad createStickyPad(UUID id, String description, List<Sticky> stickies) {
        StickyPad pad = null;
        if (id != null && stickies != null) {
            pad = new StickyPad();
            pad.setStickies(stickies);
            pad.setDescription(description);
            pad.setId(id);
        }

        return pad;
    }

    public List<Sticky> getStickies() {
        return stickies;
    }

    public void setStickies(List<Sticky> stickies) {
        this.stickies = stickies;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StickyPad stickyPad = (StickyPad) o;

        if (!id.equals(stickyPad.id)) return false;
        return stickies.equals(stickyPad.stickies);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + stickies.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "StickyPad{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", stickies=" + stickies +
                '}';
    }
}
