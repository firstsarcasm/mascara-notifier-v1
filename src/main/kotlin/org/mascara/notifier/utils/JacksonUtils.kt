package org.mascara.notifier.utils

import com.fasterxml.jackson.databind.ObjectMapper

object JacksonUtils {
    val om = ObjectMapper()
    public fun <E> String.deserialize(clazz: Class<Array<E>>) = om.readValue(this, clazz)
}