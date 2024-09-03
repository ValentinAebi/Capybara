package com.github.valentinaebi.capybara.execution

import com.github.valentinaebi.capybara.cfg.BasicBlock
import com.github.valentinaebi.capybara.cfg.Method
import com.github.valentinaebi.capybara.repositories.ArraysRepository
import com.github.valentinaebi.capybara.repositories.OwnedObjectsRepository
import com.github.valentinaebi.capybara.values.ProgramValue
import org.objectweb.asm.tree.analysis.Frame


fun executeSymbolically(method: Method) {
    TODO()
}

private data class ProgramState(
    val frame: Frame<ProgramValue>,
    val nextBlock: BasicBlock,
    val ownedObjectsRepository: OwnedObjectsRepository,
    val arraysRepository: ArraysRepository
)
