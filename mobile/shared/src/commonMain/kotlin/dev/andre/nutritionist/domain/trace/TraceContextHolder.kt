package dev.andre.nutritionist.domain.trace


interface TraceContextHolder {
    fun getTraceparent(): String?
}
