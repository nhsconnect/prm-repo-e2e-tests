package uk.nhs.prm.e2etests.model;

import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@Getter
public class NemsEventMessage {
    private final UUID id;
    private final String message;

    public static class Builder {
        private UUID id;
        private String message;

        public void id(UUID id) {
            this.id = id;
        }

        public void message(String message) {
            this.message = message;
        }

        public NemsEventMessage build() {
            return new NemsEventMessage(this);
        }
    }

    public NemsEventMessage(Builder builder) {
        this.id = builder.id;
        this.message = builder.message;
    }

    public NemsEventMessage(UUID id, String message) {
        this.id = id;
        this.message = message;
    }

    @Override
    public String toString() {
        return "NemsEventMessage{" +
                "id=" + id +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NemsEventMessage message1 = (NemsEventMessage) o;
        return Objects.equals(id, message1.id) && Objects.equals(message, message1.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, message);
    }
}
