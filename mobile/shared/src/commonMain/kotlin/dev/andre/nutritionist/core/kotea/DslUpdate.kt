/*
 * Copyright 2021-2023 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.andre.nutritionist.core.kotea

/**
 * DSL-обёртка для декларативного описания редьюсера.
 */
abstract class DslUpdate<State : Any, in Event : Any, Command : Any, News : Any> :
    Update<State, Event, Command, News> {

    final override fun update(state: State, event: Event): Next<State, Command, News> {
        val builder = NextBuilder<State, Command, News>(state)
        builder.update(event)
        return builder.build()
    }

    protected abstract fun NextBuilder<State, Command, News>.update(event: Event)

    class NextBuilder<State : Any, Command : Any, News : Any>(
        val state: State,
    ) {
        @PublishedApi
        internal var newState: State? = null
        private val commandsList = mutableListOf<Command>()
        private val newsList = mutableListOf<News>()

        /**
         * Установка нового состояния через лямбду трансформации текущего состояния.
         */
        inline fun state(transform: State.() -> State) {
            val current = newState ?: state
            newState = current.transform()
        }

        /**
         * Добавление асинхронных команд на исполнение.
         */
        fun commands(vararg commands: Command) {
            commandsList.addAll(commands)
        }

        fun commands(commands: Iterable<Command>) {
            commandsList.addAll(commands)
        }

        /**
         * Добавление разовых событий (news / UI эффектов).
         */
        fun news(vararg news: News) {
            newsList.addAll(news)
        }

        fun news(news: Iterable<News>) {
            newsList.addAll(news)
        }

        fun build(): Next<State, Command, News> =
            Next(
                state = newState,
                commands = commandsList.toList(),
                news = newsList.toList(),
            )
    }
}
