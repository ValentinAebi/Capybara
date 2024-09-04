package com.github.valentinaebi.capybara.checks

import com.github.valentinaebi.capybara.cfg.Class
import com.github.valentinaebi.capybara.cfg.Method

data class Issue(
    val clazz: Class,
    val method: Method,
    val line: Int?,
    val description: String
)
