package com.ciscowebex.androidsdk.webhook;

import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.membership.MembershipClient;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.ciscowebex.androidsdk.WebexTestRunner.getWebex;

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
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void create() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        client.create("", "", "", "", "", "", result -> {
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void get() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        client.get("", result -> {
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void update() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        client.update("", "", "", result -> {
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void update_secret() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        client.update("", "", "", "", "", result -> {
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void delete() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        client.delete("", result -> {
            System.out.println(result);
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }
}