package com.newproject.blog.events;

import java.time.OffsetDateTime;

public class DomainEvent {
    private final String eventType;
    private final String aggregateType;
    private final String aggregateId;
    private final OffsetDateTime occurredAt;
    private final Object payload;

    public DomainEvent(String eventType, String aggregateType, String aggregateId, OffsetDateTime occurredAt, Object payload) {
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
        this.payload = payload;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public Object getPayload() {
        return payload;
    }
}
