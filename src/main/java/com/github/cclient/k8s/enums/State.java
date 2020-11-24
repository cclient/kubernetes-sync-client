package com.github.cclient.k8s.enums;

public enum State {
    QUEUED("QUEUED"),
    FAILED("FAILED"),
    SUCCESS("SUCCESS"),
    RUNNING("RUNNING"),
    ;
    private final String value;

    State(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
