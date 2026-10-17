package com.bidder.application.configs.kafka;

public enum EventTopics {
    NOTIFICATION("notification"),
    PAYMENT("payment");

    private final String topic;

    EventTopics(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return this.topic;
    }
}

