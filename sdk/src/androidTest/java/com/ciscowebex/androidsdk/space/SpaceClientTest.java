package com.ciscowebex.androidsdk.space;

import com.ciscowebex.androidsdk.Webex;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.ciscowebex.androidsdk.WebexTestRunner.getWebex;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SpaceClientTest {
    static Webex webex;
    static SpaceClient client;
    static String spaceId;

    @BeforeClass
    public static void setUpBeforeClass() {
        webex = getWebex();
        client = webex.spaces();
    }

    @Test
    public void list() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        client.list(null, 10, null, null, result -> {
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
        final CountDownLatch signal = new CountDownLatch(1);
        String title = "test space create";
        client.create(title, null, result -> {
            if (result.isSuccessful()) {
                System.out.println(result.getData());
                spaceId = result.getData().getId();
                assertNotNull(spaceId);
                assertEquals(title, result.getData().getTitle());
            } else {
                System.out.println(result.getError());
            }
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }


    @Test
    public void testB_get() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        assertNotNull(spaceId);
        client.get(spaceId, result -> {
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
    public void testC_update() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        assertNotNull(spaceId);
        String newTitle = "test update space";
        client.update(spaceId, newTitle, result -> {
            if (result.isSuccessful()) {
                System.out.println(result.getData());
                assertEquals(newTitle, result.getData().getTitle());
            } else {
                System.out.println(result.getError());
            }
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void testD_delete() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        assertNotNull(spaceId);
        client.delete(spaceId, result -> {
            if (result.isSuccessful()) {
                System.out.println(result.getData());
            } else {
                System.out.println(result.getError());
            }
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }
}