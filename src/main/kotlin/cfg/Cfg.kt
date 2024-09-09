package com.github.valentinaebi.capybara.cfg

data class Cfg(val basicBlocks: List<BasicBlock>, val catches: List<Catch>){

    val initialBasicBlock: BasicBlock? = basicBlocks.firstOrNull()

}
