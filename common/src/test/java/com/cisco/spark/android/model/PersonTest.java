package com.cisco.spark.android.model;

import com.cisco.spark.android.sync.ActorRecord;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PersonTest {

    @Test
    public void testPersonConstructor() {
        Person person = new Person("a@b", "1234", "foo");
        checkPerson(person, "a@b", "1234", "foo");

        person = new Person(null, "1234", "foo");
        checkPerson(person, null, "1234", "foo");

        person = new Person("a@b", null, "foo");
        checkPerson(person, "a@b", null, "foo");

        person = new Person("a@b", null, null);
        checkPerson(person, "a@b", null, "a@b");

        person = new Person(newActorRecord("a@b", "1234", "foo"));
        checkPerson(person, "a@b", "1234", "foo");

        person = new Person(newActorRecord(null, "1234", "foo"));
        checkPerson(person, null, "1234", "foo");

        person = new Person(newActorRecord("a@b", null, "foo"));
        checkPerson(person, "a@b", null, "foo");

        person = new Person(newActorRecord("a@b", null, null));
        checkPerson(person, "a@b", null, "a@b");

        person = new Person(newUser("a@b", "1234", "foo"));
        checkPerson(person, "a@b", "1234", "foo");

        person = new Person(newUser(null, "1234", "foo"));
        checkPerson(person, null, "1234", "foo");

        person = new Person(newUser(null, "1234", null));
        checkPerson(person, null, "1234", null);

        try {
            new Person((User) null);
            fail("Person(null) should throw");
        } catch (Throwable e) {
        }

        try {
            new Person((ActorRecord) null);
            fail("Person(null) should throw");
        } catch (Throwable e) {
        }

        try {
            new Person(null, null, "foo");
            fail("Person(null, null, x) should throw");
        } catch (Throwable e) {
        }
    }

    void checkPerson(Person person, String email, String uuid, String displayName) {
        assertEquals(email, person.getEmail());
        assertEquals(uuid == null ? email : uuid, person.getId());
        assertEquals(uuid, person.getUuid());
        assertEquals("Wrong display name", displayName, person.getDisplayName());
    }

    ActorRecord newActorRecord(String email, String uuid, String displayname) {
        ActorRecord.ActorKey key = null;
        if (uuid != null)
            key = new ActorRecord.ActorKey(uuid);

        return new ActorRecord(key, email, displayname, false, null, false, null, null, null, 0, "");
    }

    User newUser(String email, String uuid, String displayname) {
        return new User.Builder().setEmail(email).setId(uuid == null ? email : uuid).setName(displayname).build();
    }
}
