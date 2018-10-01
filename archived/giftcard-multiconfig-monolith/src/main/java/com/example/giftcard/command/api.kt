package com.example.giftcard.command

import org.axonframework.commandhandling.TargetAggregateIdentifier

data class IssueCmd(val id: String, val amount: Int)
data class IssuedEvt(val id: String, val amount: Int)
data class RedeemCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class RedeemedEvt(val id: String, val amount: Int)
