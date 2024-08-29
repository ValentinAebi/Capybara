package com.github.valentinaebi.capybara.values

sealed interface ValuePredicate

enum class UnaryValuePredicate : ValuePredicate {
    IsZero, LessThanZero, GreaterThanZero, IsNull
}

enum class BinaryValuePredicate : ValuePredicate {
    Equal, LessThan, GreaterThan
}
