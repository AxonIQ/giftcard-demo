package io.axoniq.demo.giftcard.api

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class IssueCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class IssuedEvt(val id: String, val amount: Int)
data class RedeemCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class RedeemedEvt(val id: String, val amount: Int)
data class CancelCmd(@TargetAggregateIdentifier val id: String)
data class CancelEvt(val id: String)

data class CardSummary(var id: String, var initialValue: Int, var remainingValue: Int) {
    constructor() : this("", 0, 0)
}

data class CardSummaryFilter(val idStartsWith: String = "")
class CountCardSummariesQuery(val filter: CardSummaryFilter = CardSummaryFilter()) { override fun toString() : String = "CountCardSummariesQuery" }
data class CountCardSummariesResponse(val count: Int, val lastEvent: Long)
data class FetchCardSummariesQuery(val offset: Int, val limit: Int, val filter: CardSummaryFilter)

class CountChangedUpdate()