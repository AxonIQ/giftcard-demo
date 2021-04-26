package io.axoniq.demo.giftcard.api

import org.axonframework.modelling.command.TargetAggregateIdentifier

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

// Commands

data class IssueCommand(@TargetAggregateIdentifier val id: String, val amount: Int)
data class RedeemCommand(@TargetAggregateIdentifier val id: String, val amount: Int)
data class CancelCommand(@TargetAggregateIdentifier val id: String)

// Events

data class IssuedEvent(val id: String, val amount: Int)
data class RedeemedEvent(val id: String, val amount: Int)
data class CancelEvent(val id: String)

// Queries

data class CardSummaryFilter(val idStartsWith: String = "")
class CountCardSummariesQuery(val filter: CardSummaryFilter = CardSummaryFilter()) {
    override fun toString(): String = "CountCardSummariesQuery"
}

data class FetchCardSummariesQuery(val offset: Int, val limit: Int, val filter: CardSummaryFilter)
class CountChangedUpdate

// Query Responses

@Entity
@NamedQueries(
        NamedQuery(
                name = "CardSummary.fetch",
                query = "SELECT c FROM CardSummary c WHERE c.id LIKE CONCAT(:idStartsWith, '%') ORDER BY c.id"
        ),
        NamedQuery(
                name = "CardSummary.count",
                query = "SELECT COUNT(c) FROM CardSummary c WHERE c.id LIKE CONCAT(:idStartsWith, '%')"
        )
)
data class CardSummary(@Id var id: String, var initialValue: Int, var remainingValue: Int) {
    constructor() : this("", 0, 0)
}

data class CountCardSummariesResponse(val count: Int, val lastEvent: Long)
