package com.github.valentinaebi.capybara.programstruct

import com.github.valentinaebi.capybara.InternalName

data class Class(
    val className: String,
    val fields: Map<String, InternalName>,
    val methods: LinkedHashMap<String, Method>
)
