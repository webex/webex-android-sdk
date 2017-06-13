package com.cisco.spark.android.locus.model;

import java.io.Serializable;

public class CallInNumbersInfo implements Serializable {

    // Fields set by gson
    PstnNumber callInTollNumber;
    PstnNumber callInTollFreeNumber;
    String globalCallInNumberUrl;

    private CallInNumbersInfo() {
        // use builder
    }

    public PstnNumber getCallInTollFreeNumber() {
        return callInTollFreeNumber;
    }

    public PstnNumber getCallInTollNumber() {
        return callInTollNumber;
    }

    public String getGlobalCallInNumbersUrl() {
        return globalCallInNumberUrl;
    }

    public static class CallInNumbersInfoBuilder {

        private CallInNumbersInfo callInNumbersInfo;

        public CallInNumbersInfoBuilder() {
            this.callInNumbersInfo = new CallInNumbersInfo();
        }

        public CallInNumbersInfo build() {
            return callInNumbersInfo;
        }

        public CallInNumbersInfoBuilder setCallInTollFreeNumber(String number) {
            PstnNumber tollFreeNumber = new PstnNumber.PstnNumberBuilder().setNumber(number).setIsTollFree(true).build();
            callInNumbersInfo.callInTollFreeNumber = tollFreeNumber;
            return this;
        }

        public CallInNumbersInfoBuilder setCallInTollNumber(String number) {
            PstnNumber tollNumber = new PstnNumber.PstnNumberBuilder().setNumber(number).setIsTollFree(false).build();
            callInNumbersInfo.callInTollNumber = tollNumber;
            return this;
        }

        public CallInNumbersInfoBuilder setGlobalCallInNumberUrl(String globalCallInNumberUrl) {
            callInNumbersInfo.globalCallInNumberUrl = globalCallInNumberUrl;
            return this;
        }
    }
}
