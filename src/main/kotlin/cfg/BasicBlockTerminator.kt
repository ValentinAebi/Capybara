package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.values.ValuePredicate

sealed interface BasicBlockTerminator {
    fun resolve(resolver: Map<BasicBlock, BasicBlock>)
}

data class ReturnTerminator(val mustPopValue: Boolean) : BasicBlockTerminator {
    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) = Unit
}

data object ThrowTerminator : BasicBlockTerminator {
    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) = Unit
}

class SingleSuccessorTerminator(private var _successor: BasicBlock) : BasicBlockTerminator {

    val successor: BasicBlock get() = _successor

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _successor = resolver[_successor]!!
    }
}

class IteTerminator(
    val cond: ValuePredicate,
    private var _succIfTrue: BasicBlock,
    private var _succIfFalse: BasicBlock
) : BasicBlockTerminator {

    val succIfTrue get() = _succIfTrue
    val succIfFalse get() = _succIfFalse

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _succIfTrue = resolver[_succIfTrue]!!
        _succIfFalse = resolver[_succIfFalse]!!
    }
}

class TableSwitchTerminator(
    val minKey: Int,
    private var _cases: List<BasicBlock>,
    private var _default: BasicBlock?
) : BasicBlockTerminator {

    val cases: List<BasicBlock> get() = _cases
    val default: BasicBlock? get() = _default

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _cases = _cases.map { resolver[it]!! }
        _default = resolver[_default]!!
    }
}

class LookupSwitchTerminator(
    private var _cases: Map<Any, BasicBlock>,
    private var _default: BasicBlock?
) : BasicBlockTerminator {

    val keys: Map<Any, BasicBlock> get() = _cases
    val default: BasicBlock? get() = _default

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _cases = _cases.map { (pv, bb) -> pv to resolver[bb]!! }.toMap()
        _default = resolver[_default]!!
    }
}
