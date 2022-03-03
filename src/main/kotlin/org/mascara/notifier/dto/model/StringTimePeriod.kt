package org.mascara.notifier.dto.model

data class StringTimePeriod(var startTime: String? = null, var endTime: String? = null) {
    constructor() : this(null, null) {
    }
}