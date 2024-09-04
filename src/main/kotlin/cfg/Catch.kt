package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.TypeDescriptor


class Catch(
    val handledExceptionType: TypeDescriptor,
    private var _handler: BasicBlock,
    val parentCatch: Catch?,
    val childrenCatches: List<Catch>,
    val catchIdx: Int
) {

    val handler: BasicBlock get() = _handler

    fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _handler = resolver[_handler]!!
    }

    override fun toString(): String = "Catch#$catchIdx"

}
