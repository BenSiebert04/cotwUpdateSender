package org.sie.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HistoryEntry {
    private final String message;
    private final String timestamp;

    @JsonCreator
    public HistoryEntry(
            @JsonProperty("message") String message,
            @JsonProperty("timestamp") String timestamp
    ) {
        this.message = message;
        this.timestamp = timestamp;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("timestamp")
    public String getTimestamp() {
        return timestamp;
    }
}