package com.ciscowebex.androidsdk.team;

import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.membership.MembershipClient;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.ciscowebex.androidsdk.WebexTestRunner.getWebex;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TeamClientTest {
    static Webex webex;
    static TeamClient client;

    @BeforeClass
    public static void setUpBeforeClass() {
        webex = getWebex();
        client = webex.teams();
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
        client.create("", result -> {
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
        client.update("", "", result -> {
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