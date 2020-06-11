package com.ciscowebex.androidsdk.internal;

import com.ciscowebex.androidsdk.utils.WebexId;

import java.util.Objects;

public class Identifier {

    private WebexId id;

    private String url;

    public Identifier(String base64Id) {
        this(Objects.requireNonNull(WebexId.from(base64Id)));
    }

    public Identifier(WebexId id) {
        this.id = id;
    }

    public Identifier(WebexId id, String url) {
        this.id = id;
        this.url = url;
    }

    public String uuid() {
        return id.getUUID();
    }

    public String url() {
        return url;
    }

    public String url(Device device) {
        if (url == null) {
            // TODO Find the cluster for the identifier instead of use home cluster always.
            if (id.is(WebexId.Type.ROOM)) {
                url = Service.Conv.baseUrl(device) + "/conversations/" + uuid();
            }
            else if (id.is(WebexId.Type.MESSAGE)) {
                url = Service.Conv.baseUrl(device) + "/activities/" + uuid();
            }
            else if (id.is(WebexId.Type.TEAM)) {
                url = Service.Conv.baseUrl(device) + "/teams/" + uuid();
            }
        }
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identifier that = (Identifier) o;
        if (url != null && that.url != null) {
            return url.equals(that.url);
        }
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url);
    }
}
