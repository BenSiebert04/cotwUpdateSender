package org.sie.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

    private final String id;
    private final boolean isAuthorized;

    @JsonCreator
    public User(
            @JsonProperty("id") String id,
            @JsonProperty("isAuthorized") boolean isAuthorized
    ) {
        this.id = id;
        this.isAuthorized = isAuthorized;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("isAuthorized")
    public boolean isAuthorized() {
        return isAuthorized;
    }
}
