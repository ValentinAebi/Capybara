package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.Type

data class Catch(
    val exceptionTypes: Type,
    val handler: BasicBlock,
    val parentCatch: Catch?,
    val childrenCatches: List<Catch>
)
