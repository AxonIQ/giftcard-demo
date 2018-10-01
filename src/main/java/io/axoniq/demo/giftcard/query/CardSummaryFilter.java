package io.axoniq.demo.giftcard.query;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
public class CardSummaryFilter {

    @NonNull
    String idStartsWith;

}
