package com.github.valentinaebi.capybara.loading

import com.github.valentinaebi.capybara.API_LEVEL
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.MethodNode


class MethodBuilder(methodNode: MethodNode) : MethodVisitor(API_LEVEL, methodNode)
