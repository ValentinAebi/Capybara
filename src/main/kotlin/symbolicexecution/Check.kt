package com.github.valentinaebi.capybara.symbolicexecution

enum class Check(val msg: String) {

    // Null pointers
    INVK_NULL_REC("invocation receiver might be null"),
    FLD_GET_NULL("field owner might be null"),

    // Array indices
    ARRAY_READ_INDEX_OUT("index of array access might be out of bounds")

    // TODO others
}
