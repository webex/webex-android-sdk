package com.cisco.spark.android.roap;

import com.cisco.spark.android.roap.model.RoapAnswerMessage;
import com.cisco.spark.android.roap.model.RoapBaseMessage;
import com.cisco.spark.android.roap.model.RoapErrorMessage;
import com.cisco.spark.android.roap.model.RoapErrorType;
import com.cisco.spark.android.roap.model.RoapSessionCallbacks;
import com.cisco.spark.android.roap.model.RoapOfferMessage;
import com.cisco.spark.android.roap.model.RoapOkMessage;
import com.cisco.spark.android.roap.model.RoapSession;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.times;

public class RoapSessionTest {

    RoapSession session;

    RoapSessionCallbacks sender;

    @Before
    public void setup() {
        sender = Mockito.mock(RoapSessionCallbacks.class);
        session = new RoapSession(sender);
    }

    //test basic offer answer

    @Test
    public void testOfferAnswerWithAutoOK() throws Exception {
        final List<String> sdps = Arrays.asList("abc");
        final List<String> answerSdps = Arrays.asList("123");
        session.sendRoapMessage(sdps.get(0),  RoapBaseMessage.TIEBREAKER_MAX);
        session.setAutoSendOK(true);

        ArgumentCaptor<RoapBaseMessage> sendCaptor = ArgumentCaptor.forClass(RoapBaseMessage.class);
        Mockito.verify(sender, times(1)).sendRoapMessage(sendCaptor.capture());

        assertTrue(sendCaptor.getValue() instanceof RoapOfferMessage);

        RoapOfferMessage offer = (RoapOfferMessage) sendCaptor.getValue();
        assertEquals(offer.getSdps(), sdps);
        assertEquals(offer.getSeq(), new Integer(1));

        RoapAnswerMessage answer = new RoapAnswerMessage(offer.getSeq(), answerSdps);

        // receive a roap message from the remote side from the webhook callback. receive the sdp
        assertEquals(session.processRoapMessage(answer), answerSdps);

        ArgumentCaptor<RoapBaseMessage> answerCaptor = ArgumentCaptor.forClass(RoapBaseMessage.class);
        Mockito.verify(sender, times(2)).sendRoapMessage(answerCaptor.capture());

        assertTrue(answerCaptor.getValue() instanceof RoapOkMessage);

        RoapOkMessage ok = (RoapOkMessage) answerCaptor.getValue();
        assertEquals(ok.getSeq(), offer.getSeq());
    }

    private void basicOutgoingOfferAnswer(Integer expSeq)  throws Exception {
        //get and SDP from for example a SIP Invite
        final List<String> sdps = Arrays.asList("abc");
        final List<String> answerSdps = Arrays.asList("123");
        session.sendRoapMessage(sdps.get(0), RoapBaseMessage.TIEBREAKER_MAX);

        ArgumentCaptor<RoapBaseMessage> sendCaptor = ArgumentCaptor.forClass(RoapBaseMessage.class);
        Mockito.verify(sender, times(1)).sendRoapMessage(sendCaptor.capture());

        assertTrue(sendCaptor.getValue() instanceof RoapOfferMessage);

        RoapOfferMessage offer = (RoapOfferMessage) sendCaptor.getValue();

        assertEquals(offer.getSdps(), sdps);
        assertEquals(offer.getSeq(), expSeq);
        assertEquals(offer.getTieBreaker(),  RoapBaseMessage.TIEBREAKER_MAX);

        RoapAnswerMessage answer = new RoapAnswerMessage(offer.getSeq(), answerSdps);

        // receive a roap message from the remote side from the webhook callback. receive the sdp
        assertEquals(session.processRoapMessage(answer), answerSdps);

        session.sendOK();

        ArgumentCaptor<RoapBaseMessage> answerCaptor = ArgumentCaptor.forClass(RoapBaseMessage.class);
        Mockito.verify(sender, times(2)).sendRoapMessage(answerCaptor.capture());

        assertTrue(answerCaptor.getValue() instanceof RoapOkMessage);

        RoapOkMessage ok = (RoapOkMessage) answerCaptor.getValue();
        assertEquals(ok.getSeq(), offer.getSeq());

        Mockito.reset(sender);
    }

    @Test
    public void testOfferAnswer() throws Exception {
        for (int expSeq = 1; expSeq < 3; expSeq++) {
            basicOutgoingOfferAnswer(expSeq);
        }
    }

    private void basicIncomingOfferAnswer(Integer expSeq)  throws Exception {
        final List<String> sdps = Arrays.asList("abc");
        final List<String> answerSdps = Arrays.asList("123");

        RoapOfferMessage offer = new RoapOfferMessage(expSeq, sdps, 1L);
        assertEquals(session.processRoapMessage(offer), sdps);

        session.sendRoapMessage(answerSdps.get(0),  RoapBaseMessage.TIEBREAKER_MAX);

        ArgumentCaptor<RoapBaseMessage> answerCaptor = ArgumentCaptor.forClass(RoapBaseMessage.class);
        Mockito.verify(sender, times(1)).sendRoapMessage(answerCaptor.capture());

        assertTrue(answerCaptor.getValue() instanceof RoapAnswerMessage);

        RoapAnswerMessage answer = (RoapAnswerMessage) answerCaptor.getValue();
        assertEquals(answer.getSdps(), answerSdps);
        assertEquals(offer.getSeq(), expSeq);

        Mockito.reset(sender);
    }

    @Test
    public void testReceiveOfferAfterOfferSent() throws Exception {
        int startingSeq = 1;
        basicOutgoingOfferAnswer(startingSeq);
        basicIncomingOfferAnswer(startingSeq + 1);
    }


    //negative offer answer

    @Test
    public void testOfferSendErrorMsg() throws Exception {
        //get and SDP from for example a SIP Invite
        final List<String> sdps = Arrays.asList("abc");
        session.sendRoapMessage(sdps.get(0),  RoapBaseMessage.TIEBREAKER_MAX);

        ArgumentCaptor<RoapBaseMessage> sendCaptor = ArgumentCaptor.forClass(RoapBaseMessage.class);
        Mockito.verify(sender, times(1)).sendRoapMessage(sendCaptor.capture());

        assertTrue(sendCaptor.getValue() instanceof RoapOfferMessage);

        RoapOfferMessage offer = (RoapOfferMessage) sendCaptor.getValue();
        assertEquals(offer.getSdps(), sdps);
        assertEquals(offer.getSeq(), new Integer(1));

        session.sendError(RoapErrorType.FAILED, "something bad happened");

        ArgumentCaptor<RoapBaseMessage> errorCaptor = ArgumentCaptor.forClass(RoapBaseMessage.class);
        Mockito.verify(sender, times(2)).sendRoapMessage(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof RoapErrorMessage);
        RoapErrorMessage error = (RoapErrorMessage) errorCaptor.getValue();
        assertEquals(error.getErrorType(), RoapErrorType.FAILED);
        assertEquals(error.getErrorCause(), "something bad happened");
        assertEquals(error.getSeq(), offer.getSeq());
    }


}
