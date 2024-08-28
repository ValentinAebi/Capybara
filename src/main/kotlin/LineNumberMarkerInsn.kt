package com.github.valentinaebi.capybara

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode

class LineNumberMarkerInsn(val line: Int) : AbstractInsnNode(-1) {

    override fun getType(): Int = LINE

    override fun accept(methodVisitor: MethodVisitor?) {
        throw UnsupportedOperationException()
    }

    override fun clone(clonedLabels: Map<LabelNode?, LabelNode?>?): AbstractInsnNode? {
        return LineNumberMarkerInsn(line)
    }

}
