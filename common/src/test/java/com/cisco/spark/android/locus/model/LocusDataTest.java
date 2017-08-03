package com.cisco.spark.android.locus.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static com.cisco.spark.android.locus.model.LocusParticipant.Type.ANONYMOUS;
import static com.cisco.spark.android.locus.model.LocusParticipant.Type.MEETING_BRIDGE;
import static com.cisco.spark.android.locus.model.LocusParticipant.Type.RESOURCE_ROOM;
import static com.cisco.spark.android.locus.model.LocusParticipant.Type.USER;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class LocusDataTest {

    @Test
    public void testGetJoinedParticipants() {

        Locus mockLocus = mock(Locus.class);
        LocusData locusData = new LocusData(mockLocus);

        // None - empty
        when(mockLocus.getParticipants()).thenReturn(Collections.<LocusParticipant>emptyList());
        assertEquals(0, locusData.getJoinedParticipants().size());

        final LocusParticipant joined = createParticipant(LocusParticipant.State.JOINED, "");
        final LocusParticipant declined = createParticipant(LocusParticipant.State.DECLINED, "");

        // Just one joined, noone else
        when(mockLocus.getParticipants()).thenReturn(Collections.singletonList(joined));
        assertEquals(1, locusData.getJoinedParticipants().size());

        // One joined, one declined (https://sqbu-github.cisco.com/Acano/sparkling/issues/915)
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(joined, declined));
        assertEquals(1, locusData.getJoinedParticipants().size());

        // Two joined, one declined (https://sqbu-github.cisco.com/Acano/sparkling/issues/915)
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(joined, joined, declined));
        assertEquals(2, locusData.getJoinedParticipants().size());
    }

    @Test
    public void testOnlyMeJoined() {

        Locus mockLocus = mock(Locus.class);

        String selfId = "me";

        LocusSelfRepresentation mockSelf = mock(LocusSelfRepresentation.class);
        LocusParticipantInfo selfPerson = mock(LocusParticipantInfo.class);
        when(mockSelf.getPerson()).thenReturn(selfPerson);
        when(selfPerson.getId()).thenReturn(selfId);
        when(mockLocus.getSelf()).thenReturn(mockSelf);

        LocusData locusData = new LocusData(mockLocus);

        // None - empty
        when(mockLocus.getParticipants()).thenReturn(Collections.<LocusParticipant>emptyList());
        assertEquals(false, locusData.onlyMeJoined());

        final LocusParticipant meJoined = createParticipant(LocusParticipant.State.JOINED, selfId);
        final LocusParticipant otherJoined = createParticipant(LocusParticipant.State.JOINED, "not me");
        final LocusParticipant otherDeclined = createParticipant(LocusParticipant.State.DECLINED, "also not me");

        // Just one joined, noone else
        when(mockSelf.getState()).thenReturn(LocusParticipant.State.JOINED);
        when(mockLocus.getParticipants()).thenReturn(Collections.singletonList(meJoined));
        assertEquals(true, locusData.onlyMeJoined());

        // One joined, one declined (https://sqbu-github.cisco.com/Acano/sparkling/issues/915)
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(meJoined, otherDeclined));
        assertEquals(true, locusData.onlyMeJoined());

        // Two joined, one declined (https://sqbu-github.cisco.com/Acano/sparkling/issues/915)
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(otherJoined, otherJoined, otherDeclined));
        assertEquals(false, locusData.onlyMeJoined());
    }

    private LocusParticipant createParticipant(LocusParticipant.State state, String id) {

        LocusParticipant ppt = mock(LocusParticipant.class);
        when(ppt.getState()).thenReturn(state);

        LocusParticipantInfo person = mock(LocusParticipantInfo.class);
        when(ppt.getPerson()).thenReturn(person);
        when(person.getId()).thenReturn(id);

        return ppt;
    }

    private LocusParticipant getParticipant(LocusParticipant.Type type, String name, LocusParticipant.State joinState) {
        LocusParticipantDevice device = new LocusParticipantDevice.Builder()
                .setDeviceType("ANDROID")
                .setState(LocusParticipant.State.JOINED)
                .build();
        return getParticipant(type, name, joinState, device);
    }

    private LocusParticipant getParticipant(LocusParticipant.Type type, String name, LocusParticipant.State joinState, LocusParticipantDevice device) {
        return getParticipant(type, name, joinState, device, new LocusParticipantInfo(UUID.randomUUID().toString(), name, name + "@email.com"));
    }

    private LocusParticipant getParticipant(LocusParticipant.Type type, String name, LocusParticipant.State joinState, LocusParticipantDevice device, LocusParticipantInfo info) {
        return new LocusParticipant.Builder().setType(type)
                .setPerson(info)
                .setId(UUID.fromString(info.getId()))
                .addDevice(device)
                .setState(joinState)
                .build();
    }

    private LocusParticipantDevice createPairedDevice(String associatedWith) {
        LocusParticipant.Intent intent = new LocusParticipant.Intent(UUID.randomUUID(),
                LocusParticipant.IntentType.OBSERVE,
                associatedWith);
        return new LocusParticipantDevice.Builder().setIntent(intent).build();
    }

    @Test
    public void testIsBridgeJoined() {

        Locus mockLocus = mock(Locus.class);
        LocusData locusData = new LocusData(mockLocus);

        // Unpaired
        LocusParticipant fry = getParticipant(USER, "Fry", LocusParticipant.State.JOINED);
        LocusParticipant leela = getParticipant(USER, "Leela", LocusParticipant.State.JOINED);
        LocusParticipant zoidberg = getParticipant(USER, "Zoidberg", LocusParticipant.State.JOINED);

        LocusParticipant clamps = getParticipant(RESOURCE_ROOM, "Clamps", LocusParticipant.State.JOINED);
        LocusParticipant robotDevil = getParticipant(RESOURCE_ROOM, "Robot Devil", LocusParticipant.State.JOINED);
        LocusParticipant bender = getParticipant(RESOURCE_ROOM, "Bender", LocusParticipant.State.JOINED);

        LocusParticipant hypnotoad = getParticipant(ANONYMOUS, "Hynotoad", LocusParticipant.State.JOINED);
        LocusParticipant planetExpress = getParticipant(MEETING_BRIDGE, "Planet Express", LocusParticipant.State.JOINED);

        // Paired
        UUID flexoUUID = UUID.randomUUID();
        LocusParticipantDevice device = createPairedDevice(flexoUUID.toString());
        LocusParticipant zappPaired = getParticipant(USER, "Zapp", LocusParticipant.State.JOINED, device);
        LocusParticipant amyPaired = getParticipant(USER, "Amy", LocusParticipant.State.JOINED, device);

        LocusParticipant flexo = new LocusParticipant.Builder().setType(RESOURCE_ROOM).setState(LocusParticipant.State.JOINED)
                                                               .setId(flexoUUID).build();



        // Null
        when(mockLocus.getParticipants()).thenReturn(null);
        assertFalse("Shouldn't be bridge with null participants list", locusData.isBridge());

        // Empty
        when(mockLocus.getParticipants()).thenReturn(new ArrayList<LocusParticipant>());
        assertFalse("Shouldn't be bridge with empty participants list", locusData.isBridge());

        // One user
        when(mockLocus.getParticipants()).thenReturn(Collections.singletonList(fry));
        assertFalse("One user isn't a bridge", locusData.isBridge());

        // Two users
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, leela));
        assertFalse("Two users aren't a bridge", locusData.isBridge());

        // One Machine (unpaired)
        when(mockLocus.getParticipants()).thenReturn(Collections.singletonList(bender));
        assertFalse("One unpaired machine isn't a bridge", locusData.isBridge());

        // Two machines (unpaired)
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(bender, clamps));
        assertFalse("Two unpaired machines aren't a bridge", locusData.isBridge());

        // Three users
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, leela, zoidberg));
        assertTrue("Three users are a bridge", locusData.isBridge());

        // Two users + unpaired machine
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, leela, // Users
                                                                   flexo)); // Unpaired machine
        assertTrue("Two users and an unpaired machine are a bridge", locusData.isBridge());

        // Three unpaired machines
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(bender, clamps, robotDevil));
        assertTrue("Three unpaired machines are a bridge", locusData.isBridge());

        // Two users and a paired machine
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, // User
                                                                   zappPaired, bender)); // Paired user + machine
        assertFalse("A User, and a user paired with a machine are not a bridge", locusData.isBridge());

        // User, machine, bridge
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, // User
                                                                   bender, // machine
                                                                   planetExpress)); // bridge
        assertTrue("User, machine, bridge are a bridge", locusData.isBridge());

        // User, bridge, anonymous
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, // user
                                                                   planetExpress, // bridge
                                                                   hypnotoad)); // anon
        assertTrue("User, bridge, anonymous are a bridge", locusData.isBridge());
    }

    @Test
    public void testDeclined() {

        Locus mockLocus = mock(Locus.class);
        LocusData locusData = new LocusData(mockLocus);

        LocusParticipant fry = getParticipant(USER, "Fry", LocusParticipant.State.JOINED);
        LocusParticipant leela = getParticipant(USER, "Leela", LocusParticipant.State.JOINED);

        LocusParticipant amyLeft = getParticipant(USER, "Amy", LocusParticipant.State.LEFT);
        LocusParticipant amyDeclined = getParticipant(USER, "Amy", LocusParticipant.State.DECLINED);

        // 1 joined, 1 left
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, // joined
                                                                   amyLeft)); // left
        assertFalse("1 user left on their own is not a bridge", locusData.isBridge());

        // 2 joined, 1 left
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, leela, // joined
                                                                   amyLeft)); // left
        assertTrue("2 users left on their own is a bridge", locusData.isBridge());

        // 2 joined, 1 left
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, leela, // joined
                                                                   amyDeclined)); // declined
        assertTrue("2 users joined with a decline is also a bridge", locusData.isBridge());
    }

    @Test
    public void testPairedUsers() {

        Locus mockLocus = mock(Locus.class);
        LocusData locusData = new LocusData(mockLocus);

        UUID flexoUUID = UUID.randomUUID();
        UUID benderUUID = UUID.randomUUID();

        // Unpaired
        LocusParticipant fry = getParticipant(USER, "Fry", LocusParticipant.State.JOINED);
        LocusParticipant leela = getParticipant(USER, "Leela", LocusParticipant.State.JOINED);
        LocusParticipant fryLeft = getParticipant(USER, "Fry", LocusParticipant.State.LEFT);
        LocusParticipant leelaLeft = getParticipant(USER, "Leela", LocusParticipant.State.LEFT);

        // Paired users
        LocusParticipantDevice deviceFlexo = createPairedDevice(flexoUUID.toString());
        LocusParticipantDevice deviceBender = createPairedDevice(benderUUID.toString());

        LocusParticipant zappPaired = getParticipant(USER, "Zapp", LocusParticipant.State.JOINED, deviceFlexo);
        LocusParticipant amyPaired = getParticipant(USER, "Amy", LocusParticipant.State.JOINED, deviceBender);
        LocusParticipant amyPairedLeft = getParticipant(USER, "Amy", LocusParticipant.State.LEFT, deviceBender);

        // Room systems
        LocusParticipantInfo flexoUser = new LocusParticipantInfo(flexoUUID.toString(), "Flexo", "Flexo@email.com");
        LocusParticipant flexo = getParticipant(RESOURCE_ROOM, "Flexo", LocusParticipant.State.JOINED, new LocusParticipantDevice(), flexoUser);

        LocusParticipantInfo benderUser = new LocusParticipantInfo(benderUUID.toString(), "Bender", "Bender@email.com");
        LocusParticipant bender = getParticipant(RESOURCE_ROOM, "Bender", LocusParticipant.State.JOINED, new LocusParticipantDevice(), benderUser);
        LocusParticipant benderLeft = getParticipant(RESOURCE_ROOM, "Bender", LocusParticipant.State.LEFT, new LocusParticipantDevice(), benderUser);

        // Self
        LocusSelfRepresentation selfFlexo = new LocusSelfRepresentation.Builder().setState(LocusParticipant.State.JOINED).setPerson(flexoUser).build();
        when(mockLocus.getSelf()).thenReturn(selfFlexo);

        // 1 room system, 1 observer, 2 remote users
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, leela, zappPaired, flexo));
        assertFalse("Three users joined", locusData.onlyMeJoined());

        // 1 room system, 1 observer, 2 remote users left. Observers are not counted as joined participants
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fryLeft, leelaLeft, zappPaired, flexo));
        assertTrue("Only one room system and its observer joined", locusData.onlyMeJoined());

        // 1 room system, 2 observers, 2 remote users left. Observers are not counted as joined participants
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fryLeft, leelaLeft, zappPaired, amyPaired, flexo));
        assertTrue("Only one room system and its observers joined", locusData.onlyMeJoined());

        // 2 room systems with an observer each, 2 remote users. Observers are not counted as joined participants
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fry, leela, zappPaired, amyPaired, flexo, bender));
        assertFalse("Two room systems, their observers and two remote users joined", locusData.onlyMeJoined());

        // 2 room systems with an observer each, 2 remote users left. Observers are not counted as joined participants
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fryLeft, leelaLeft, zappPaired, amyPaired, flexo, bender));
        assertFalse("Two room systems and their observers joined", locusData.onlyMeJoined());

        // 2 room systems with an observer each, 2 remote users left. A room system and its observer leave. Observers are not counted as joined participants
        when(mockLocus.getParticipants()).thenReturn(Arrays.asList(fryLeft, leelaLeft, zappPaired, amyPairedLeft, flexo, benderLeft));
        assertTrue("Only one room system and its observer joined", locusData.onlyMeJoined());

    }
}
