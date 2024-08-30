package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.types.Type

class Catch(
    val handledExceptionType: Type,
    private var _handler: BasicBlock,
    val parentCatch: Catch?,
    val childrenCatches: List<Catch>,
    val catchIdx: Int
) {

    val handler: BasicBlock get() = _handler

    fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _handler = resolver[_handler]!!
    }

    override fun toString(): String = "Catch $catchIdx"

    fun fullDescr(): String {
        var cnt = 0
        var catch: Catch? = this
        while (catch != null){
            catch = catch.parentCatch
            cnt += 1
        }
        return "Catch $catchIdx (nestingLevel $cnt)"
    }

}
