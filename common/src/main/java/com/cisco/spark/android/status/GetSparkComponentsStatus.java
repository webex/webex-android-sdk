package com.cisco.spark.android.status;

import java.util.ArrayList;
import java.util.List;

public class GetSparkComponentsStatus {

    private List<SparkComponentStatus> components;

    public GetSparkComponentsStatus() {
        components = new ArrayList<>();
    }

    public List<SparkComponentStatus> getStatus() {
        return components;
    }
}
