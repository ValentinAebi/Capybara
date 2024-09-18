package com.github.valentinaebi.capybara.loading

import com.github.valentinaebi.capybara.API_LEVEL
import com.github.valentinaebi.capybara.GraphBuilder
import com.github.valentinaebi.capybara.programstruct.Method
import com.github.valentinaebi.capybara.programstruct.MethodIdentifier
import com.github.valentinaebi.capybara.programstruct.mkMethodIdentifier
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.MethodNode


class MethodBuilder(
    private val methodId: MethodIdentifier,
    private val methodNode: MethodNode,
    private val mayBeOverridden: Boolean,
    private val hasReceiver: Boolean,
    private val classMethods: MutableMap<String, Method>,
    private val callGraph: GraphBuilder<MethodIdentifier>
) : MethodVisitor(API_LEVEL, methodNode) {

    private var numLocals: Int? = null
    private var maxStack: Int? = null

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        val invokedMethodId = mkMethodIdentifier(owner, name, descriptor)
        callGraph.addEdge(methodId, invokedMethodId)
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

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
