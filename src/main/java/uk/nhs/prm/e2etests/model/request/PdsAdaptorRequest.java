package uk.nhs.prm.e2etests.model.request;

import lombok.EqualsAndHashCode;

public record PdsAdaptorRequest(String previousGp,
                                String recordETag)
{ }