package com.github.valentinaebi.capybara.programstruct

import com.github.valentinaebi.capybara.InternalName
import com.github.valentinaebi.capybara.cfg.Cfg
import com.github.valentinaebi.capybara.cfg.buildCfg
import com.github.valentinaebi.capybara.checking.insertAssertions
import org.objectweb.asm.tree.MethodNode

data class MethodIdentifier(val className: InternalName, val methodName: String, val descriptor: String){
    val returnTypeDescr: String get() = descriptor.takeLastWhile { it != ')' }
    override fun toString(): String = "$className#$methodName$descriptor"
}

data class Method(
    val methodId: MethodIdentifier,
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
