package com.cisco.spark.android.model;

import com.cisco.spark.android.sync.ActorRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Participants {
    private final List<Person> list = new ArrayList<>();

    public Participants() {
    }

    public List<Person> get() {
        synchronized (list) {
            return Collections.unmodifiableList(list);
        }
    }

    public String[] getIds() {
        synchronized (list) {
            ArrayList<String> keys = new ArrayList<>();
            for (Person person : list) {
                keys.add(person.getId());
            }
            return keys.toArray(new String[]{});
        }
    }

    public void add(ActorRecord participant) {
        add(new Person(participant));
    }

    public void add(Person participant) {
        if (participant == null) {
            return;
        }
        synchronized (list) {
            if (!list.contains(participant))
                list.add(participant);
        }
    }

    public void addAll(Collection<Person> people) {
        synchronized (list) {
            list.removeAll(people);
            // adding one at a time to ensure no duplicates
            for (Person person : people) {
                add(person);
            }
        }
    }

    public void remove(Person participant) {
        synchronized (list) {
            list.remove(participant);
        }
    }

    public boolean contains(Person person) {
        synchronized (list) {
            return list.contains(person);
        }
    }

    public boolean isEmpty() {
        synchronized (list) {
            return list.isEmpty();
        }
    }

    public void clear() {
        synchronized (list) {
            list.clear();
        }
    }

    public void set(Collection<Person> participants) {
        synchronized (list) {
            list.clear();
            addAll(participants);
        }
    }

    public int size() {
        synchronized (list) {
            return list.size();
        }
    }
}
