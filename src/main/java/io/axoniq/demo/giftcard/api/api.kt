package io.axoniq.demo.giftcard.api

import io.axoniq.plugin.data.protection.annotation.SensitiveData
import io.axoniq.plugin.data.protection.annotation.SensitiveDataHolder
import io.axoniq.plugin.data.protection.annotation.SubjectId
import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.axonframework.serialization.Revision

// Commands
data class IssueCardCommand(@TargetAggregateIdentifier val id: String, val amount: Int)
data class RedeemCardCommand(@TargetAggregateIdentifier val id: String, val amount: Int)
data class CancelCardCommand(@TargetAggregateIdentifier val id: String)

// Events
data class CardIssuedEvent(val id: String, val amount: Int)
@SensitiveDataHolder
data class CardRedeemedEvent(val id: String, @SensitiveData(replacementValue = "hidden amount") val amount: Int)
data class CardCanceledEvent(val id: String)

//Example event for data protection plugin config generation
@SensitiveDataHolder //Only needs to be placed on the events, not on any contained objects such as the address in this example.
@Revision("1")
data class ExampleEvent(@SubjectId val id: String, @SensitiveData val ssn: String, val address: Address)
data class Address(@SensitiveData val addressLine1: String, val postalCode: String)

// Queries
data class FetchCardSummariesQuery(val offset: Int, val limit: Int)
class CountCardSummariesQuery {
    override fun toString(): String = "CountCardSummariesQuery"
}

// Query Responses
data class CountCardSummariesResponse(val count: Int, val lastEvent: Long)
data class CardSummary(var id: String = "", var initialValue: Int = 0, var remainingValue: Int = 0)
class CountChangedUpdate
