package com.example.project

import org.w3c.dom.HTMLElement
import kotlin.js.Json

@JsName("vis.DataSet")
external class VisDataSet(data: Array<Json>) {
    fun add(item: Json)
    fun remove(id: String)
    fun update(item: Json)
    fun clear()
}

@JsName("vis.Network")
external class VisNetwork(container: HTMLElement, data: Json, options: Json) {
    fun setOptions(options: Json)
    fun destroy()
    fun getNodeAt(position: Json): String? // Возвращает ID вершины по координатам клика
    fun on(event: String, callback: (Json) -> Unit)
}