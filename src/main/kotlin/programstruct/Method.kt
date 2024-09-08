package com.github.valentinaebi.capybara.programstruct

import com.github.valentinaebi.capybara.cfg.Cfg
import com.github.valentinaebi.capybara.symbolicexecution.buildCfg
import org.objectweb.asm.tree.MethodNode

data class Method(
    val methodNode: MethodNode,
    val mayBeOverridden: Boolean
) {

    var cfg: Cfg? = null
        private set

    fun computeCfg() {
        cfg = buildCfg(methodNode)
    }

}
