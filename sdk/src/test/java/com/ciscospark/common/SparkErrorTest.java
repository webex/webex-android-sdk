package com.ciscospark.common;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created on 30/06/2017.
 */
public class SparkErrorTest {
    @Test
    public void testSparkError() throws Exception {

        // Test default constructor
        SparkError defaultError = new SparkError();
        assertTrue(defaultError.getErrorCode() == SparkError.ErrorCode.UNKNOWN);
        assertEquals(defaultError.getErrorMessage(), "Unknown Error");
        assertEquals(defaultError.toString(), "UNKNOWN: Unknown Error");

        // Test constructor with ErrorCode
        SparkError unknownError = new SparkError(SparkError.ErrorCode.UNKNOWN);
        assertTrue(unknownError.getErrorCode() == SparkError.ErrorCode.UNKNOWN);
        assertEquals(unknownError.getErrorMessage(), "Unknown Error");
        assertEquals(unknownError.toString(), "UNKNOWN: Unknown Error");

        // Test constructor with ErrorCode and ErrorMessage
        SparkError myError = new SparkError(SparkError.ErrorCode.UNKNOWN, "My Error");
        assertTrue(myError.getErrorCode() == SparkError.ErrorCode.UNKNOWN);
        assertEquals(myError.getErrorMessage(), "My Error");
    }
}
