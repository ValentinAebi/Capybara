package com.github.valentinaebi.capybara

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun strongAssert(value: Boolean){
    contract {
        returns() implies value
    }
    if (!value){
        throw AssertionError()
    }
}
