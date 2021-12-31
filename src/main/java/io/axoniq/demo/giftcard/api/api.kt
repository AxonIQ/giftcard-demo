package io.axoniq.demo.giftcard.api

import org.axonframework.modelling.command.TargetAggregateIdentifier

// Commands
data class IssueCardCommand(@TargetAggregateIdentifier val id: String, val amount: Int)
data class RedeemCardCommand(@TargetAggregateIdentifier val id: String, val amount: Int)
data class CancelCardCommand(@TargetAggregateIdentifier val id: String)

// Events
data class CardIssuedEvent(val id: String, val amount: Int)
data class CardRedeemedEvent(val id: String, val amount: Int)
data class CardCanceledEvent(val id: String)

// Queries
data class FetchCardSummariesQuery(val offset: Int, val limit: Int)
class CountCardSummariesQuery {
    override fun toString(): String = "CountCardSummariesQuery"
}

// Query Responses
data class CountCardSummariesResponse(val count: Int, val lastEvent: Long)
data class CardSummary(var id: String = "", var initialValue: Int = 0, var remainingValue: Int = 0)
class CountChangedUpdate
