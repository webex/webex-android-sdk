package com.cisco.spark.android.locus.model;
// Core piece copied from: https://sqbu-github.cisco.com/WebExSquared/cisco-spark-base/blob/dbf8ec21a59dec1fae6f8da0aae75ac967d53d46/wx2-core/common/src/main/java/com/cisco/wx2/util/JsonUtil.java

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {
    public static String toJson(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writer().writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
