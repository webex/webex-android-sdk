package com.cisco.spark.android.metrics.value;

public class SpaceBindingMetricsValues {

    public static class BindingMetricValue {
        public static final String BIND = "bind";
        private String lyraBindingType;
        private String result;
        private String message;

        public BindingMetricValue(String lyraBindingType, String result, String message) {
            this.lyraBindingType = lyraBindingType;
            this.result = result;
            this.message = message;
        }

        @Override
        public String toString() {
            return "SpaceBindingMetricsValues{" +
                    "lyraBindingType='" + lyraBindingType + '\'' +
                    "result=" + result +
                    "message=" + message +
                    '}';
        }

        @Override
        public int hashCode() {
            return  this.toString().hashCode();
        }
    }
}
