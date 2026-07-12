package com.example.project

import io.kvision.Application
import io.kvision.CoreModule
import io.kvision.BootstrapModule
import io.kvision.BootstrapCssModule
import io.kvision.Hot
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.p
import io.kvision.html.button
import io.kvision.form.text.textArea
import io.kvision.form.text.TextArea
import io.kvision.panel.root
import io.kvision.panel.vPanel
import io.kvision.panel.hPanel
import io.kvision.startApplication
import io.kvision.utils.px

class App : Application() {
    override fun start() {
        root("kvapp") {
            // Главный вертикальный контейнер с отступами
            vPanel(spacing = 20) {
                padding = 20.px

                // Шапка приложения (Navbar)
                div(className = "navbar navbar-dark bg-dark rounded p-3 shadow-sm") {
                    h1("Визуализатор Топологической Сортировки", className = "h4 text-white m-0")
                }

                // Основная рабочая область (две колонки)
                hPanel(spacing = 30) {

                    // Левая колонка: Панель управления (Ввод графа)
                    vPanel(spacing = 15, className = "card p-4 shadow-sm") {
                        width = 350.px

                        p("Ввод структуры графа:", className = "fw-bold")

                        // Поле ввода списка ребер
                        val inputArea = textArea {
                            rows = 6
                            placeholder = "Пример:\n1 -> 2\n2 -> 3\n1 -> 3"
                            addCssClass("form-control")
                        }

                        // Кнопка для запуска алгоритма
                        button("Запустить сортировку", className = "btn btn-primary w-100") {
                            onClick {
                                val text = inputArea.value
                                println("Входные данные для TopSort: $text")
                                // TODO: Сюда добавим парсинг и вызов алгоритма
                            }
                        }
                    }

                    // Правая колонка: Область вывода результатов
                    vPanel(spacing = 15, className = "card p-4 shadow-sm flex-grow-1") {
                        p("Результат топологической сортировки:", className = "fw-bold")

                        // Контейнер, куда будет выводиться результат
                        div(className = "alert alert-secondary") {
                            p("Здесь будет отображаться отсортированный массив вершин или граф.")
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    startApplication(
        ::App,
        js("import.meta.webpackHot").unsafeCast<Hot?>(),
        BootstrapModule,
        BootstrapCssModule,
        CoreModule
    )
}