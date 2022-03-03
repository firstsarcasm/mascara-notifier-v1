package org.mascara.notifier.dto.model

import org.mascara.notifier.utils.NullTypes
import java.time.LocalTime

data class TimePeriod(var startTime: LocalTime? = null, var endTime: LocalTime? = null) {
    constructor() : this(NullTypes.nullLocalTime, NullTypes.nullLocalTime)
    constructor(start: String?, end: String?) : this(LocalTime.parse(start), LocalTime.parse(end))
}