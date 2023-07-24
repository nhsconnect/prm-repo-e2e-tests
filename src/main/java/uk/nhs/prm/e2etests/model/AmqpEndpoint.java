package uk.nhs.prm.e2etests.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AmqpEndpoint {
    private String protocol;
    private String hostname;
    private int port;

    @Override
    public String toString() {
        return String.format("%s://%s:%d", this.protocol, this.hostname, this.port);
    }
}