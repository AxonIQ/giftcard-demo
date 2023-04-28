package io.axoniq.demo.giftcard.command;

import io.axoniq.demo.giftcard.api.CardIssuedEvent;
import io.axoniq.demo.giftcard.api.CardRedeemedEvent;
import io.axoniq.demo.giftcard.api.IssueCardCommand;
import io.axoniq.demo.giftcard.api.RedeemCardCommand;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class GiftCardTest {

    private static final String CARD_ID = UUID.randomUUID().toString();
    private static final int AMOUNT = 1377;
    private static final int NEGATIVE_AMOUNT = -1337;

    private final FixtureConfiguration<GiftCard> testFixture = new AggregateTestFixture<>(GiftCard.class);

    @Test
    void testIssueCardCommandPublishesCardIssuedEvent() {
        testFixture.givenNoPriorActivity()
                   .when(new IssueCardCommand(CARD_ID, AMOUNT))
                   .expectSuccessfulHandlerExecution()
                   .expectEvents(new CardIssuedEvent(CARD_ID, AMOUNT));
    }

    @Test
    void testIssueCardCommandThrowsIllegalArgumentExceptionForNegativeAmount() {
        testFixture.givenNoPriorActivity()
                   .when(new IssueCardCommand(CARD_ID, NEGATIVE_AMOUNT))
                   .expectException(IllegalArgumentException.class);
    }

    @Test
    void testRedeemCardCommandPublishesCardRedeemedEvent() {
        testFixture.given(new CardIssuedEvent(CARD_ID, AMOUNT))
                   .when(new RedeemCardCommand(CARD_ID, AMOUNT))
                   .expectSuccessfulHandlerExecution()
                   .expectEvents(new CardRedeemedEvent(CARD_ID, AMOUNT));
    }

    @Test
    void testRedeemCardCommandThrowsIllegalArgumentExceptionForNegativeAmount() {
        testFixture.given(new CardIssuedEvent(CARD_ID, AMOUNT))
                   .when(new RedeemCardCommand(CARD_ID, NEGATIVE_AMOUNT))
                   .expectException(IllegalArgumentException.class);
    }

    @Test
    void testRedeemCardCommandThrowsIllegalStateExceptionForInsufficientFunds() {
        testFixture.given(new CardIssuedEvent(CARD_ID, AMOUNT), new CardRedeemedEvent(CARD_ID, AMOUNT))
                   .when(new RedeemCardCommand(CARD_ID, AMOUNT))
                   .expectException(IllegalStateException.class);
    }
}