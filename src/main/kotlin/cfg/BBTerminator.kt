package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.ProgramValue

sealed interface BBTerminator

data object ReturnTerminator : BBTerminator

data class SingleSuccessorTerminator(val successor: BasicBlock) : BBTerminator

data class IteTerminator(
    val cond: (ProgramValue) -> Boolean,
    val succIfTrue: BasicBlock,
    val succIfFalse: BasicBlock
) : BBTerminator

data class SwitchTerminator(val keys: Map<ProgramValue, BasicBlock>, val default: BasicBlock?) : BBTerminator
