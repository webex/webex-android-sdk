package com.cisco.spark.android.presence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PresenceStatusRequest {
    private List<String> subjects;
    private Integer subscriptionTtl;

    public PresenceStatusRequest(Collection<String> subjects) {
        this.subjects = new ArrayList<String>(subjects);
        this.subscriptionTtl = null;
    }

    public PresenceStatusRequest(Collection<String> subjects, long time, TimeUnit timeUnit) {
        this.subjects = new ArrayList<String>(subjects);
        setSubscriptionTtl(time, timeUnit);
    }

    public void addSubject(String subject) {
        this.subjects.add(subject);
    }

    public void removeSubject(String subject) {
        this.subjects.remove(subject);
    }

    public List<String> getSubjects() {
        return this.subjects;
    }

    public void removeFirstSubject() {
        this.subjects.remove(0);
    }

    public boolean containsSubject(String subject) {
        return subjects.contains(subject);
    }


    public int subjectCount() {
        return subjects.size();
    }

    public void setSubscriptionTtl(long time, TimeUnit base) {
        this.subscriptionTtl = (int) base.toSeconds(time);
    }
}
