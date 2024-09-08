package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.InternalName


class Catch(
    val handledExceptionType: InternalName,
    handlerBlock: BasicBlock,
    val parentCatch: Catch?,
    val childrenCatches: List<Catch>,
    val catchIdx: Int
) {

    var handler: BasicBlock = handlerBlock
        private set

    fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        handler = resolver[handler]!!
    }

    override fun toString(): String = "Catch#$catchIdx"

}
