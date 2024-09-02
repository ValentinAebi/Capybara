package com.github.valentinaebi.capybara

import com.github.valentinaebi.capybara.values.ProgramValue

sealed interface Formula

data class Maybe(val possiblyTrue: Formula) : Formula
data class Not(val negated: Formula) : Formula

data class Equals(val left: ProgramValue, val right: ProgramValue) : Formula
data class IsNull(val value: ProgramValue) : Formula
