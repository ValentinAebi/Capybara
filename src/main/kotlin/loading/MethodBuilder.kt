package com.github.valentinaebi.capybara.loading

import com.github.valentinaebi.capybara.API_LEVEL
import com.github.valentinaebi.capybara.programstruct.Method
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.MethodNode


class MethodBuilder(
    private val methodNode: MethodNode,
    private val mayBeOverridden: Boolean,
    private val hasReceiver: Boolean,
    private val classMethods: MutableMap<String, Method>
) : MethodVisitor(API_LEVEL, methodNode) {

    private var numLocals: Int? = null
    private var maxStack: Int? = null

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        this.numLocals = maxLocals
        this.maxStack = maxStack
    }

    override fun visitEnd() {
        classMethods[methodNode.name] = Method(
            methodNode,
            mayBeOverridden,
            hasReceiver,
            numLocals,
            maxStack
        )
    }

}
