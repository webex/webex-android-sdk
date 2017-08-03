package com.cisco.spark.android.model;

public class UpdateUserRequest {
    private final String[] schemas = {"urn:scim:schemas:core:1.0", "urn:scim:schemas:extension:cisco:commonidentity:1.0"};
    private String password;

    public static class Builder {
        UpdateUserRequest updateUserRequest;

        public Builder() {
            updateUserRequest = new UpdateUserRequest();
        }

        public Builder withPassword(String password) {
            updateUserRequest.password = password;
            return this;
        }

        public UpdateUserRequest build() {
            return updateUserRequest;
        }
    }
}
