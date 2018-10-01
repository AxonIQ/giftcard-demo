package io.axoniq.demo.giftcard.query;

import lombok.Value;

@Value
public class FetchCardSummariesQuery {

    int offset;
    int limit;
    CardSummaryFilter filter;

}
