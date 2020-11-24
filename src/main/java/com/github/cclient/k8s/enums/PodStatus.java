package com.github.cclient.k8s.enums;

public enum PodStatus {
    PENDING("pending"),
    RUNNING("running"),
    FAILED("failed"),
    SUCCEEDED("succeeded"),
    ;
    private final String value;

    PodStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
