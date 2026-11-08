package dev.andre.nutritionist.domain.trace

interface TraceContextHolder {
    fun getTraceparent(): String?
}

class DefaultTraceContextHolder : TraceContextHolder {
    override fun getTraceparent(): String? = null
}

