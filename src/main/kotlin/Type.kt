package com.github.valentinaebi.capybara

sealed interface Type

data class ReferenceType(val fullName: String) : Type
