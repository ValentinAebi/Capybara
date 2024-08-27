package com.github.valentinaebi.capybara

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.MethodNode


class ClassAnalyzer : ClassVisitor(API_LEVEL) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return MethodAnalyzer(name!!, MethodNode(access, name, descriptor, signature, exceptions))
    }

}