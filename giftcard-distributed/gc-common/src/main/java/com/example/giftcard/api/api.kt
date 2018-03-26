package com.example.giftcard.api

import java.time.Instant
import org.axonframework.commandhandling.TargetAggregateIdentifier

data class IssueCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class IssuedEvt(val id: String, val amount: Int)
data class RedeemCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class RedeemedEvt(val id: String, val amount: Int)
data class CancelCmd(@TargetAggregateIdentifier val id: String)
data class CanceledEvt(val id: String)

data class FindCardSummariesQuery(val offset: Int, val limit: Int)
data class FindCardSummariesResponse(val data: List<CardSummary>)

class CountCardSummariesQuery { override fun toString() : String = "CountCardSummariesQuery" }
data class CountCardSummariesResponse(val count: Int)

data class CardSummary(
        var id: String? = null,
        var initialValue: Int? = null,
        var issuedAt: Instant? = null,
        var remainingValue: Int? = null
        )
