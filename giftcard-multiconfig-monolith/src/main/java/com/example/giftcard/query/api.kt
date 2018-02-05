package com.example.giftcard.query

import java.time.Instant
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class CardSummary(
        @Id var id: String? = null,
        var initialValue: Int? = null,
        var issuedAt: Instant? = null,
        var remainingValue: Int? = null
        )

data class CardSummariesUpdatedEvt(val id: String)

data class FindCardSummariesQuery(val offset: Int, val limit: Int)
data class FindCardSummariesResponse(val data: List<CardSummary>)

class CountCardSummariesQuery { override fun toString() : String = "CountCardSummariesQuery" }
data class CountCardSummariesResponse(val count: Int)
