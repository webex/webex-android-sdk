package com.cisco.spark.android.metrics.value;

public class SpaceBindingMetricsValues {

    public static class BindingMetricValue {
        public static final String BIND = "bind";
        public static final String UNBIND = "unbind";
        private String lyraBindingType;
        private boolean success;

        public BindingMetricValue(String lyraBindingType, boolean success) {
            this.lyraBindingType = lyraBindingType;
            this.success = success;
        }

        @Override
        public String toString() {
            return "BindingMetricValue{" +
                    "lyraBindingType='" + lyraBindingType + '\'' +
                    "success=" + success +
                    '}';
        }

        @Override
        public int hashCode() {
            return  this.toString().hashCode();
        }
    }
}
