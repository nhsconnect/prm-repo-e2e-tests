package uk.nhs.prm.e2etests.model;

import lombok.*;

import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class NemsEventMessage {
    private final UUID id;
    private final String message;

    @Override
    public String toString() {
        return String.format("NemsEventMessage{id=%s, message='%s'}", id, message);
    }

}
