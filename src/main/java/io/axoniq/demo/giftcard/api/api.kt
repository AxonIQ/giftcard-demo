package io.axoniq.demo.giftcard.api

import org.axonframework.modelling.command.TargetAggregateIdentifier

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

data class IssueCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class IssuedEvt(val id: String, val amount: Int)
data class RedeemCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class RedeemedEvt(val id: String, val amount: Int)
data class CancelCmd(@TargetAggregateIdentifier val id: String)
data class CancelEvt(val id: String)

@Entity
@NamedQueries(
        NamedQuery(name = "CardSummary.fetch",
                query = "SELECT c FROM CardSummary c WHERE c.id LIKE CONCAT(:idStartsWith, '%') ORDER BY c.id"),
        NamedQuery(name = "CardSummary.count",
                query = "SELECT COUNT(c) FROM CardSummary c WHERE c.id LIKE CONCAT(:idStartsWith, '%')"))
data class CardSummary(@Id var id: String, var initialValue: Int, var remainingValue: Int) {
    constructor() : this("", 0, 0)
}

data class CardSummaryFilter(val idStartsWith: String = "")
class CountCardSummariesQuery(val filter: CardSummaryFilter = CardSummaryFilter()) { override fun toString() : String = "CountCardSummariesQuery" }
data class CountCardSummariesResponse(val count: Int, val lastEvent: Long)
data class FetchCardSummariesQuery(val offset: Int, val limit: Int, val filter: CardSummaryFilter)

class CountChangedUpdate()