package com.github.valentinaebi.capybara.programstruct

import com.github.valentinaebi.capybara.InternalName
import com.github.valentinaebi.capybara.cfg.Cfg
import com.github.valentinaebi.capybara.cfg.buildCfg
import com.github.valentinaebi.capybara.checking.insertAssertions
import org.objectweb.asm.tree.MethodNode

typealias MethodIdentifier = String

fun mkMethodIdentifier(className: InternalName, methodName: String, descriptor: String): MethodIdentifier =
    "$className#$methodName$descriptor"

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

    fun computeCfgWithAssertions() {
        if (cfg != null) {
            throw IllegalStateException("CFG has already been computed")
        }
        val augmentedInstructions = insertAssertions(methodNode.instructions.toArray())
        cfg = buildCfg(augmentedInstructions, methodNode.tryCatchBlocks)
    }

}
