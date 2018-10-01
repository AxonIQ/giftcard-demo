package io.axoniq.demo.giftcard.api

import io.axoniq.demo.giftcard.query.CardSummary
import org.axonframework.commandhandling.TargetAggregateIdentifier

import java.time.Instant

data class IssueCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class IssuedEvt(val id: String, val amount: Int)
data class RedeemCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class RedeemedEvt(val id: String, val amount: Int)
data class CancelCmd(@TargetAggregateIdentifier val id: String)
data class CancelEvt(val id: String)

class CountCardSummariesQuery { override fun toString() : String = "CountCardSummariesQuery" }
data class CountCardSummariesResponse(val count: Int, val lastEvent: Long)
