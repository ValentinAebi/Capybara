package com.github.valentinaebi.capybara.cfg

data class Method(
    val methodName: String,
    val basicBlocks: List<BasicBlock>,
    val catches: List<Catch>
){

    val initBB get() = basicBlocks.first()

    val topLevelCatches get() = catches.filter { it.parentCatch == null }
    val bottomLevelCatches get() = catches.filter { it.childrenCatches.isEmpty() }

}
