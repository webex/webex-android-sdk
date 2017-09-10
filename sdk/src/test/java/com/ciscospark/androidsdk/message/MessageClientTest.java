package com.ciscospark.androidsdk.message;

import com.ciscospark.androidsdk.Spark;
import com.ciscospark.androidsdk.auth.JWTAuthenticator;
import com.google.gson.Gson;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by zhiyuliu on 02/09/2017.
 */

public class MessageClientTest {

    private static String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJreWxlX2RlbW8iLCJuYW1lIjoia3lsZV9kZW1vIiwiaXNzIjoiY2Q1YzlhZjctOGVkMy00ZTE1LTk3MDUtMDI1ZWYzMGIxYjZhIn0.KuabN0TWa00F2Auv4vnu8DXrXAiVM9p1dL8fJUEScbg";
    private static String testPersonId = "Y2lzY29zcGFyazovL3VzL1BFT1BMRS83MmEyYTY4ZS1kNjc5LTRkMTUtOTdlOC1mNzRiZWViYTA2OWU";
    private static JWTAuthenticator authenticator;
    private static MessageClient mClient;
    private static Spark mSpark;
    private static boolean authCompleted = false;

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("setup test case");
        authenticator = new JWTAuthenticator();
        authenticator.authorize(auth_token);
        mClient = new MessageClient(authenticator);
    }

    @Test
    public void post() throws Exception {
        mClient.post(null, null, "zhiyuliu@cisco.com", "Hello", null, null, result -> {
            System.out.println(result);
        });
        Thread.sleep(10 * 1000);
    }

    @Test
    public void test() {

        Gson gson = new Gson();
        Message m = gson.fromJson("{\"roomType\":\"drr\"}", Message.class);

        System.out.println(gson.toJson(m));


    }

}
