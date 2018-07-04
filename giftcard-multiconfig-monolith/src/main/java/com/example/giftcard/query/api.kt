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

class CardProjectionUpdated { override fun toString() : String = "CardProjectionUpdated" }

data class FindCardSummariesQuery(val offset: Int, val limit: Int)

class CountCardSummariesQuery { override fun toString() : String = "CountCardSummariesQuery" }
