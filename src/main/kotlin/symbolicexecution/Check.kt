package com.github.valentinaebi.capybara.symbolicexecution

enum class Check(val msg: String) {

    // Null pointers
    INVK_NULL_REC("invocation receiver might be null"),
    FLD_NULL_OWNER("field owner might be null"),
    INDEXING_NULL_ARRAY("array might be null"),

    // Array indices
    ARRAY_INDEX_OUT("array index might be out of bounds")

    // TODO others
}
