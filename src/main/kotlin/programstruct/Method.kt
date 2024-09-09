package com.github.valentinaebi.capybara.programstruct

import com.github.valentinaebi.capybara.cfg.Cfg
import com.github.valentinaebi.capybara.cfg.buildCfg
import org.objectweb.asm.tree.MethodNode

data class Method(
    val methodNode: MethodNode,
    val mayBeOverridden: Boolean,
    val hasReceiver: Boolean,
    val numLocals: Int?,
    val maxStack: Int?
) {

    val methodName get() = methodNode.name
    val isAbstract: Boolean get() = (numLocals == null)

    var cfg: Cfg? = null
        private set

    fun computeCfg() {
        if (cfg != null) {
            throw IllegalStateException("CFG has already been computed")
        }
        cfg = buildCfg(methodNode)
    }

}
