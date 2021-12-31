package io.axoniq.demo.giftcard.api

import org.axonframework.modelling.command.TargetAggregateIdentifier

// Commands

data class IssueCommand(@TargetAggregateIdentifier val id: String, val amount: Int)
data class RedeemCommand(@TargetAggregateIdentifier val id: String, val amount: Int)
data class CancelCommand(@TargetAggregateIdentifier val id: String)

// Events

data class IssuedEvent(val id: String, val amount: Int)
data class RedeemedEvent(val id: String, val amount: Int)
data class CancelEvent(val id: String)

// Queries
data class FetchCardSummariesQuery(val offset: Int, val limit: Int, val filter: CardSummaryFilter)
class CountCardSummariesQuery(val filter: CardSummaryFilter = CardSummaryFilter()) {
    override fun toString(): String = "CountCardSummariesQuery"
}
data class CardSummaryFilter(val idStartsWith: String = "")

// Query Responses

data class CountCardSummariesResponse(val count: Int, val lastEvent: Long)
data class CardSummary(var id: String, var initialValue: Int, var remainingValue: Int) {
    constructor() : this("", 0, 0)
}
class CountChangedUpdate
