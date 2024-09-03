package com.github.valentinaebi.capybara.cfg

sealed interface BasicBlockTerminator {
    fun resolve(resolver: Map<BasicBlock, BasicBlock>)
    fun fullDescr(): String
}

data class ReturnTerminator(val mustPopValue: Boolean) : BasicBlockTerminator {
    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) = Unit
    override fun toString(): String = "return"
    override fun fullDescr(): String = if (mustPopValue) "nonvoid-return" else "void-return"
}

data object ThrowTerminator : BasicBlockTerminator {
    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) = Unit
    override fun toString(): String = "throw"
    override fun fullDescr(): String = "throw"
}

class SingleSuccessorTerminator(private var _successor: BasicBlock) : BasicBlockTerminator {

    val successor: BasicBlock get() = _successor

    override fun toString(): String = "goto $successor"
    override fun fullDescr(): String = "goto $successor"

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _successor = resolver[_successor]!!
    }
}

class IteTerminator(
    val cond: OperandStackPredicate,
    private var _succIfTrue: BasicBlock,
    private var _succIfFalse: BasicBlock
) : BasicBlockTerminator {

    val succIfTrue get() = _succIfTrue
    val succIfFalse get() = _succIfFalse

    override fun toString(): String = "$succIfTrue or $succIfFalse"
    override fun fullDescr(): String = "if $cond then $succIfTrue else $succIfFalse"

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

    override fun toString(): String = "tableswitch"
    override fun fullDescr(): String {
        val sb = StringBuilder()
        sb.append("tableswitch\n")
        for ((idx, block) in cases.withIndex()){
            val key = minKey + idx
            sb.append(" ").append(key).append(" -> ").append(block).append("\n")
        }
        sb.append(" default -> ").append(default).append("\n")
        return sb.toString()
    }

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

    override fun toString(): String = "lookupswitch"
    override fun fullDescr(): String {
        val sb = StringBuilder()
        sb.append("lookupswitch\n")
        for ((key, block) in keys){
            sb.append(" ").append(key).append(" -> ").append(block).append("\n")
        }
        sb.append(" default -> ").append(default).append("\n")
        return sb.toString()
    }

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        _cases = _cases.map { (pv, bb) -> pv to resolver[bb]!! }.toMap()
        _default = resolver[_default]!!
    }
}
