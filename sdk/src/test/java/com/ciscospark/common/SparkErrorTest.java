package com.ciscospark.common;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created on 30/06/2017.
 */
public class SparkErrorTest {
    enum MyError {
        MY_ERROR,
    }

    @Test
    public void testSparkError() throws Exception {

        // Test default constructor
        SparkError defaultError = new SparkError();
        assertTrue(defaultError.getErrorCode() == null);
        assertEquals(defaultError.getErrorMessage(), "");
        assertEquals(defaultError.toString(), "|");

        // Test constructor with ErrorCode
        SparkError unknownError = new SparkError(SparkError.ErrorCode.UNKNOWN);
        assertTrue(unknownError.getErrorCode() == SparkError.ErrorCode.UNKNOWN);
        assertEquals(unknownError.getErrorMessage(), "");
        assertEquals(unknownError.toString(), "UNKNOWN|");

        // Test constructor with ErrorCode and ErrorMessage
        SparkError myError = new SparkError(SparkError.ErrorCode.UNKNOWN, "My Error");
        assertTrue(myError.getErrorCode() == SparkError.ErrorCode.UNKNOWN);
        assertEquals(myError.getErrorMessage(), "My Error");
        assertEquals(myError.toString(), "UNKNOWN|My Error");

        // Test customer ErrorType
        SparkError<MyError> customerError = new SparkError<>(MyError.MY_ERROR, "my error");
        assertTrue(customerError.getErrorCode() == MyError.MY_ERROR);
        assertEquals(customerError.getErrorMessage(), "my error");
        assertEquals(customerError.toString(), "MY_ERROR|my error");
    }
}
