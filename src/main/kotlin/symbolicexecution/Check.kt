package com.github.valentinaebi.capybara.symbolicexecution

enum class Check(val msg: String) {
    INVK_NULL_REC("invocation receiver might be null"),
    FLD_GET_NULL("field owner might be null"),
    // TODO others
}
