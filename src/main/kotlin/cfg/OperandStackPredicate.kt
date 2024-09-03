package com.github.valentinaebi.capybara.cfg

sealed interface OperandStackPredicate

enum class UnaryOperandStackPredicate : OperandStackPredicate {
    IsZero, LessThanZero, GreaterThanZero, IsNull
}

enum class BinaryOperandStackPredicate : OperandStackPredicate {
    Equal, LessThan, GreaterThan
}
