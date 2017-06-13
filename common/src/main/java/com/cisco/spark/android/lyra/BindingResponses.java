package com.cisco.spark.android.lyra;

import java.util.List;

public class BindingResponses {


    private boolean availableForBinding;
    private boolean bound;
    private List<BindingResponse> bindings;

    public BindingResponses(boolean availableForBinding, boolean bound, List<BindingResponse> bindings) {
        this.availableForBinding = availableForBinding;
        this.bound = bound;
        this.bindings = bindings;
    }

    public List<BindingResponse> getItems() {
        return bindings;
    }

    public boolean getAvailableForBinding() {
        return availableForBinding;
    }
}
