package com.ciscowebex.androidsdk.people;

import com.ciscowebex.androidsdk.BuildConfig;
import com.ciscowebex.androidsdk.Webex;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.ciscowebex.androidsdk.WebexTestRunner.getWebex;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PersonClientTest {

    private static Webex webex;
    private static PersonClient client;
    private static String personId;

    @BeforeClass
    public static void setUpBeforeClass() throws InterruptedException {
        System.out.println("Before class");
        webex = getWebex();
        client = webex.people();
    }

    @Test
    public void list() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        client.list(BuildConfig.TEST_USER_EMAIL, null, 0, result -> {
            if (result.isSuccessful()) {
                System.out.println(result.getData());
            } else {
                System.out.println(result.getError());
            }
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void getMe() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        client.getMe(result -> {
            if (result.isSuccessful()) {
                System.out.println(result.getData());
            } else {
                System.out.println(result.getError());
            }
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void testA_create() throws InterruptedException {
        /*
        final CountDownLatch signal = new CountDownLatch(1);
        client.list(BuildConfig.TEST_USER_EMAIL, null, 0, result -> {
            if (result.isSuccessful()) {
                System.out.println(result.getData());
            } else {
                System.out.println(result.getError());
            }
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
        */
    }

    @Test
    public void testB_get() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        client.get(BuildConfig.TEST_USER_EMAIL, result -> {
            if (result.isSuccessful()) {
                System.out.println(result.getData());
                personId = result.getData().getId();
            } else {
                System.out.println(result.getError());
            }
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void testC_update() throws InterruptedException {
        //assertNotNull(persionId);
        //final CountDownLatch signal = new CountDownLatch(1);
        //client.update();
        //signal.countDown();
        //signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void testD_delete() throws InterruptedException {
        /*
        final CountDownLatch signal = new CountDownLatch(1);
        client.list("xionxiao@cisco.com", null, 0, result -> {
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
        */
    }
}