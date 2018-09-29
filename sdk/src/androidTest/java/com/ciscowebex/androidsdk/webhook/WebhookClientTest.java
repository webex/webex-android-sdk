package com.ciscowebex.androidsdk.webhook;

import com.ciscowebex.androidsdk.Webex;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.ciscowebex.androidsdk.WebexTestRunner.getWebex;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WebhookClientTest {

    static Webex webex;
    static WebhookClient client;

    @BeforeClass
    public static void setUpBeforeClass() {
        webex = getWebex();
        client = webex.webhooks();
    }

    @Test
    public void list() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        client.list(10, result -> {
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
        client.create("", "", "", "", "", "", result -> {
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
        */
    }

    @Test
    public void testB_get() throws InterruptedException {
        /*
        final CountDownLatch signal = new CountDownLatch(1);
        client.get("", result -> {
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
        */
    }

    @Test
    public void testC_update() throws InterruptedException {
        /*
        final CountDownLatch signal = new CountDownLatch(1);
        client.update("", "", "", result -> {
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
        */
    }

    @Test
    public void testD_delete() throws InterruptedException {
        /*
        final CountDownLatch signal = new CountDownLatch(1);
        client.delete("", result -> {
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
        */
    }
}