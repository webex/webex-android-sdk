package com.ciscowebex.androidsdk.message;

import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.space.Space;
import com.ciscowebex.androidsdk.space.SpaceClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.ciscowebex.androidsdk.WebexTestRunner.getWebex;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MessageClientTest {

    private static Webex webex;
    private static MessageClient messageClient;
    private static Space room;
    private static Message msg;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        webex = getWebex();
        System.out.println("is Authorized " + webex.getAuthenticator().isAuthorized());
        messageClient = webex.messages();
        createSpace();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        removeRoom();
    }

    private static void createSpace() throws InterruptedException {
        System.out.println(":::: create room");
        final CountDownLatch signal = new CountDownLatch(1);
        SpaceClient spaceClient = webex.spaces();
        spaceClient.create("message test room", null, result -> {
            if (result.isSuccessful()) {
                System.out.println(":::: create room success " + result.getData());
                room = result.getData();
            } else {
                System.out.println(":::: create room failed " + result.getError());
            }
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    private static void removeRoom() throws InterruptedException {
        System.out.println(":::: remove room");
        final CountDownLatch signal = new CountDownLatch(1);
        SpaceClient roomClient = webex.spaces();
        roomClient.delete(room.getId(), result -> signal.countDown());
        signal.await(30, TimeUnit.SECONDS);
    }

    private void waitForMessage(CountDownLatch signal, String text) {
        messageClient.setMessageObserver(event -> {
            assertThat(event, instanceOf(MessageObserver.MessageArrived.class));
            msg = ((MessageObserver.MessageArrived) event).getMessage();
            assertNotNull(msg);
            assertThat(msg.getText(), is(equalTo(text)));
            messageClient.setMessageObserver(null);
            signal.countDown();
        });
    }

    @Test
    public void testA1_post() throws InterruptedException {
        System.out.println("::::post 1");
        assertNotNull(room);
        final String text = "test post 1";
        final CountDownLatch signal = new CountDownLatch(1);

        messageClient.post(room.getId(), null, null, text, null, null, result -> {
            assertTrue(result.isSuccessful());
        });
        waitForMessage(signal, text);
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void testA2_post() throws InterruptedException {
        System.out.println("::::post 2");
        assertNotNull(room);
        final String text = "test post 2";

        final CountDownLatch signal = new CountDownLatch(1);
        messageClient.post(room.getId(), text, null, null, result -> {
            assertTrue(result.isSuccessful());
        });
        waitForMessage(signal, text);
        signal.await(30, TimeUnit.SECONDS);
    }


    @Test
    public void testB_list() throws InterruptedException {
        System.out.println("::::list");
        final CountDownLatch signal = new CountDownLatch(1);
        messageClient.list(room.getId(), null, null, null, 10, result -> {
            assertTrue(result.isSuccessful());
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }


    @Test
    public void testC_get() throws InterruptedException {
        System.out.println("::::get");
        assertNotNull(msg);
        final CountDownLatch signal = new CountDownLatch(1);
        messageClient.get(msg.getId(), result -> {
            assertTrue(result.isSuccessful());
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }

    @Test
    public void testD_delete() throws InterruptedException {
        System.out.println("::::delete");
        assertNotNull(msg);
        final CountDownLatch signal = new CountDownLatch(1);
        messageClient.delete(msg.getId(), result -> {
            assertTrue(result.isSuccessful());
            signal.countDown();
        });
        signal.await(30, TimeUnit.SECONDS);
    }
}