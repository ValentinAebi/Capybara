package com.github.valentinaebi.capybara

import com.github.valentinaebi.capybara.cfg.Method
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.MethodNode


class ClassAnalyzer(val methodsPerClasses: MutableMap<String, List<Method>>) : ClassVisitor(API_LEVEL) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val methods = mutableListOf<Method>()
        methodsPerClasses[name!!] = methods
        return MethodAnalyzer(MethodNode(access, name, descriptor, signature, exceptions), methods)
    }

}