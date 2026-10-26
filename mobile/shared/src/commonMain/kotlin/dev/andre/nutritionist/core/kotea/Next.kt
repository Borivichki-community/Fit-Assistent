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
 * Результат обработки события редьюсером.
 *
 * @param State тип состояния
 * @param Command тип асинхронных команд
 * @param News тип разовых UI-событий (эффектов)
 * @property state новое состояние (null, если состояние не изменилось)
 * @property commands набор асинхронных команд на выполнение
 * @property news список разовых уведомлений / эффектов для UI
 */
data class Next<out State : Any, out Command : Any, out News : Any>(
    val state: State? = null,
    val commands: List<Command> = emptyList(),
    val news: List<News> = emptyList(),
)
