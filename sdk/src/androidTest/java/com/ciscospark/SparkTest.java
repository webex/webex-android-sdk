package com.ciscospark;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created on 12/06/2017.
 */
public class SparkTest {
    @Test
    public void version() throws Exception {
        Spark spark = new Spark();
        assertEquals(spark.version(), "0.1");
    }
}