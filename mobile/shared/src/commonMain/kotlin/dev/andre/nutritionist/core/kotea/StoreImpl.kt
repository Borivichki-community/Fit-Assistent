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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * KMP-реализация [Store] на корутинах и каналах.
 */
class StoreImpl<State : Any, Event : Any, UiEvent : Event, Command : Any, News : Any>(
    initialState: State,
    private val update: Update<State, Event, Command, News>,
    private val commandsFlowHandler: CommandsFlowHandler<Command, Event>,
    scope: CoroutineScope,
) : Store<State, UiEvent, News> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<State> = _state.asStateFlow()

    private val _news = Channel<News>(Channel.BUFFERED)
    override val news: Flow<News> = _news.receiveAsFlow()

    private val eventsChannel = Channel<Event>(Channel.UNLIMITED)
    private val commandsChannel = Channel<Command>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (event in eventsChannel) {
                processEvent(event)
            }
        }

        scope.launch {
            commandsFlowHandler.handle(commandsChannel.receiveAsFlow()).collect { event ->
                eventsChannel.send(event)
            }
        }
    }

    override fun dispatch(event: UiEvent) {
        eventsChannel.trySend(event)
    }

    private suspend fun processEvent(event: Event) {
        val currentState = _state.value
        val next = update.update(currentState, event)

        next.state?.let { newState ->
            _state.value = newState
        }

        next.news.forEach { newsItem ->
            _news.send(newsItem)
        }

        next.commands.forEach { command ->
            commandsChannel.send(command)
        }
    }
}
