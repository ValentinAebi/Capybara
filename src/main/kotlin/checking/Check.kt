package com.github.valentinaebi.capybara.checking

import com.github.valentinaebi.capybara.*

enum class Check(val msg: String, val exception: InternalName) {

    // Null pointers
    INVK_NULL_REC("invocation receiver might be null", NULL_POINTER_EXCEPTION),
    FLD_NULL_OWNER("field owner might be null", NULL_POINTER_EXCEPTION),
    INDEXING_NULL_ARRAY("array might be null", NULL_POINTER_EXCEPTION),

    // Arrays indices and length
    ARRAY_INDEX_OUT("array index might be out of bounds", ARRAY_IDX_OUT_OF_BOUNDS_EXCEPTION),
    NEG_ARRAY_LEN("array length might be negative", NEG_ARRAY_SIZE_EXCEPTION),

    // Arithmetic
    DIV_BY_ZERO("divisor might be zero", ARITH_EXCEPTION)

    // TODO others
}
