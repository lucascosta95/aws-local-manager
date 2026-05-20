package dev.lucascosta.awslocalmanager.features.inspector.handler

object InspectorHandlerRegistry {
    private val handlers = mutableListOf<InspectorServiceHandler>()

    fun register(handler: InspectorServiceHandler) {
        handlers.add(handler)
    }

    fun all(): List<InspectorServiceHandler> = handlers.toList()

    fun forKey(key: String): InspectorServiceHandler? = handlers.find { it.serviceKey == key }
}
