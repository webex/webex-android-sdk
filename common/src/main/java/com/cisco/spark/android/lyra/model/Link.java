package com.cisco.spark.android.lyra.model;

public class Link {
    public enum Method {
        GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE
    }

    private String href;
    private Method method;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
