package com.ciscospark.androidsdk.membership;

import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;
import com.ciscospark.androidsdk.Spark;
import com.ciscospark.androidsdk.auth.JWTAuthenticator;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by zhiyuliu on 31/08/2017.
 */

public class MembershipClientTest {

    private static String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJreWxlX2RlbW8iLCJuYW1lIjoia3lsZV9kZW1vIiwiaXNzIjoiY2Q1YzlhZjctOGVkMy00ZTE1LTk3MDUtMDI1ZWYzMGIxYjZhIn0.KuabN0TWa00F2Auv4vnu8DXrXAiVM9p1dL8fJUEScbg";
    private static String testPersonId = "Y2lzY29zcGFyazovL3VzL1BFT1BMRS83MmEyYTY4ZS1kNjc5LTRkMTUtOTdlOC1mNzRiZWViYTA2OWU";
    private static JWTAuthenticator authenticator;
    private static MembershipClient mClient;
    private static Spark mSpark;
    private static boolean authCompleted = false;

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("setup test case");
        authenticator = new JWTAuthenticator();
        authenticator.authorize(auth_token);
        mClient = new MembershipClient(authenticator);
    }

    @Test
    public void list() throws Exception {
        mClient.list(null, null, null, -1, new CompletionHandler<List<Membership>>() {
            @Override
            public void onComplete(Result<List<Membership>> result) {
                System.out.println(result);
            }
        });
        Thread.sleep(10 * 1000);
    }

    @Test
    public void delete() throws Exception {
        mClient.delete("Y2lzY29zcGFyazovL3VzL01FTUJFUlNISVAvNzJhMmE2OGUtZDY3OS00ZDE1LTk3ZTgtZjc0YmVlYmEwNjllOmQzM2VmMWIwLThmOWItMTFlNy04MmRjLTk3ZTgzOWJlYjM3ZQ", new CompletionHandler<Void>() {
            @Override
            public void onComplete(Result<Void> result) {
                System.out.println(result);
            }
        });

        Thread.sleep(10 * 1000);
    }
}
