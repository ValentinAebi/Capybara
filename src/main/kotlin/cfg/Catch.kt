package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.types.Type

class Catch(
    val handledExceptionType: Type,
    private var _handler: BasicBlock,
    val parentCatch: Catch?,
    val childrenCatches: List<Catch>
) {

    val handler: BasicBlock get() = _handler

    fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _handler = resolver[_handler]!!
    }

}
