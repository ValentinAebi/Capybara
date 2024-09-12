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

class SingleSuccessorTerminator(successorBlock: BasicBlock) : BasicBlockTerminator {

    var successor: BasicBlock = successorBlock
        private set

    override fun toString(): String = "goto $successor"
    override fun fullDescr(): String = "goto $successor"

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        successor = resolver[successor]!!
    }
}

class IteTerminator(
    val cond: OperandStackPredicate,
    successorBlockIfTrue: BasicBlock,
    successorBlockIfFalse: BasicBlock
) : BasicBlockTerminator {

    var successorIfTrue = successorBlockIfTrue
        private set
    var successorIfFalse = successorBlockIfFalse
        private set

    override fun toString(): String = "$successorIfTrue or $successorIfFalse"
    override fun fullDescr(): String = "if $cond then $successorIfTrue else $successorIfFalse"

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        successorIfTrue = resolver[successorIfTrue]!!
        successorIfFalse = resolver[successorIfFalse]!!
    }
}

class TableSwitchTerminator(
    val minKey: Int,
    casesBlocks: List<BasicBlock>,
    defaultBlock: BasicBlock
) : BasicBlockTerminator {

    var cases: List<BasicBlock> = casesBlocks
        private set
    var default: BasicBlock = defaultBlock
        private set

    override fun toString(): String = "tableswitch"
    override fun fullDescr(): String {
        val sb = StringBuilder()
        sb.append("tableswitch\n")
        for ((idx, block) in cases.withIndex()) {
            val key = minKey + idx
            sb.append(" ").append(key).append(" -> ").append(block).append("\n")
        }
        sb.append(" default -> ").append(default).append("\n")
        return sb.toString()
    }

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        cases = cases.map { resolver[it]!! }
        default = resolver[default]!!
    }
}

class LookupSwitchTerminator(
    casesBlocks: Map<Any, BasicBlock>,
    defaultBlock: BasicBlock
) : BasicBlockTerminator {

    var cases: Map<Any, BasicBlock> = casesBlocks
        private set
    var default: BasicBlock = defaultBlock
        private set

    override fun toString(): String = "lookupswitch"
    override fun fullDescr(): String {
        val sb = StringBuilder()
        sb.append("lookupswitch\n")
        for ((key, block) in cases) {
            sb.append(" ").append(key).append(" -> ").append(block).append("\n")
        }
        sb.append(" default -> ").append(default).append("\n")
        return sb.toString()
    }

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        cases = cases.map { (pv, bb) -> pv to resolver[bb]!! }.toMap()
        default = resolver[default]!!
    }
}
