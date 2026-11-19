package com.pulsedesk.model

data class KnowledgeItem(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val tags: List<String>,
    val lastUpdated: String,
    val viewsCount: Int
)
