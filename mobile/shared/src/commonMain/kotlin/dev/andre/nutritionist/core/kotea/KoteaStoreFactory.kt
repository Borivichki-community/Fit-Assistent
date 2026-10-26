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

/**
 * Фабрика для создания экземпляров [Store].
 */
object KoteaStoreFactory {

    fun <State : Any, Event : Any, UiEvent : Event, Command : Any, News : Any> create(
        initialState: State,
        update: Update<State, Event, Command, News>,
        commandsFlowHandler: CommandsFlowHandler<Command, Event>,
        scope: CoroutineScope,
    ): Store<State, UiEvent, News> =
        StoreImpl(
            initialState = initialState,
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            scope = scope,
        )
}
