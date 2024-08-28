package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.ProgramValue

sealed interface BBTerminator {
    fun resolve(resolver: Map<BasicBlock, BasicBlock>)
}

data object ReturnTerminator : BBTerminator {
    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) = Unit
}

class SingleSuccessorTerminator(private var _successor: BasicBlock) : BBTerminator {

    val successor: BasicBlock get() = _successor

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _successor = resolver[_successor]!!
    }
}

class IteTerminator(
    val cond: (ProgramValue) -> Boolean,
    private var _succIfTrue: BasicBlock,
    private var _succIfFalse: BasicBlock
) : BBTerminator {

    val succIfTrue get() = _succIfTrue
    val succIfFalse get() = _succIfFalse

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _succIfTrue = resolver[_succIfTrue]!!
        _succIfFalse = resolver[_succIfFalse]!!
    }
}

class SwitchTerminator(
    private var _keys: Map<ProgramValue, BasicBlock>,
    private var _default: BasicBlock?
) : BBTerminator {

    val keys: Map<ProgramValue, BasicBlock> get() = _keys
    val default: BasicBlock? get() = _default

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _keys = _keys.map { (pv, bb) -> pv to resolver[bb]!! }.toMap()
        _default = resolver[_default]!!
    }
}
