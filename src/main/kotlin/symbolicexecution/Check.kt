package com.github.valentinaebi.capybara.symbolicexecution

enum class Check(val msg: String) {

    // Null pointers
    INVK_NULL_REC("invocation receiver might be null"),
    FLD_NULL_OWNER("field owner might be null"),
    INDEXING_NULL_ARRAY("array might be null"),

    // Arrays indices and length
    ARRAY_INDEX_OUT("array index might be out of bounds"),
    NEG_ARRAY_LEN("array length might be negative"),

    // Arithmetic
    DIV_BY_ZERO("divisor might be zero")

    // TODO others
}
