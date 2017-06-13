package com.cisco.spark.android.locus.model;

import java.io.Serializable;

public class PstnNumber implements Serializable {

    // Fields set by gson
    private String number;
    boolean tollfree;

    private PstnNumber() {
        // use builder
    }

    public String getNumber() {
        return number;
    }

    public boolean isTollfree() {
        return tollfree;
    }

    public static class PstnNumberBuilder {

        private PstnNumber pstnNumber;

        public PstnNumberBuilder() {
            this.pstnNumber = new PstnNumber();
        }

        public PstnNumber build() {
            return pstnNumber;
        }

        public PstnNumberBuilder setNumber(String number) {
            pstnNumber.number = number;
            return this;
        }

        public PstnNumberBuilder setIsTollFree(boolean isTollFree) {
            pstnNumber.tollfree = isTollFree;
            return this;
        }
    }
}
