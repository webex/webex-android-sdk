package com.cisco.spark.android.status;

public class SparkComponentStatus {

    private String status;
    private String name;
    private String id;

    public SparkComponentStatus(String status, String name, String id) {
        this.status = status;
        this.name = name;
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

}
