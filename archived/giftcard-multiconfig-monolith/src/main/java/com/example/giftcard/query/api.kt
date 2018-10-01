package com.example.giftcard.query

import java.time.Instant
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class CardSummary(
        @Id var id: String? = null,
        var initialValue: Int? = null,
        var issuedAt: Instant? = null,
        var remainingValue: Int? = null
        ) {

        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || javaClass != other.javaClass) return false
                val that = other as CardSummary?
                return id == that?.id;
        }

        override fun hashCode(): Int {
                return Objects.hash(id)
        }
}

data class FetchCardSummariesQuery(val offset: Int, val limit: Int)
class CountCardSummariesQuery { override fun toString() : String = "CountCardSummariesQuery" }
class CountChanged { override fun toString() : String = "CountChanged" }