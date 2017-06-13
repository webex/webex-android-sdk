package com.cisco.spark.android.model;

import com.cisco.spark.android.sync.ActorRecord;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class User implements Actor {
    private String id;
    private String email;
    private String name;
    private String orgId;
    private String department;
    private Date created;
    private HashSet<String> entitlements;
    private HashSet<PhoneNumber> phoneNumbers;
    private String type;

    public static class PhoneNumber {
        private String type, value;

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PhoneNumber that = (PhoneNumber) o;

            if (type != null ? !type.equals(that.type) : that.type != null) return false;
            if (value != null ? !value.equals(that.value) : that.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }
    }

    public String getId() {
        return id;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getDepartment() {
        return department;
    }

    public ActorRecord.ActorKey getActorKey() {
        return new ActorRecord.ActorKey(id);
    }

    public Date getCreated() {
        return created;
    }

    public long createdInMillis() {
        return (created != null) ? created.getTime() : 0;
    }

    public Set<String> getEntitlements() {
        return entitlements != null ? entitlements : Collections.EMPTY_SET;
    }

    public Set<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers != null ? phoneNumbers : Collections.EMPTY_SET;
    }

    public String getUuid() {
        return id;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        if (id != null ? !id.equals(user.id) : user.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (email != null ? email.hashCode() : 0);
        return result;
    }
}
