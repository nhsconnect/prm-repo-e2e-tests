package uk.nhs.prm.e2etests.model.request;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public record PdsAdaptorRequest(String previousGp,
                                String recordETag)
{ }