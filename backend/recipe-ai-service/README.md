# Recipe AI Service (`:backend:recipe-ai-service`)

> Микросервис генерации персонализированных блюд и меню под остаток суточного калоража и оставшийся бюджет с использованием ИИ (Gemini / LLM) и RAG по базе рецептов.

> **Статус:** целевой API генерации; рабочей реализации и recipe_ai.proto пока нет. Персональный баланс на сервере не ведётся.

---

## 1. База данных и хранилище

- **СУБД / Кэш:** Redis 7 (порт `6379`).
- **Назначение Redis:**
  - Ключ кэша: SHA-256 канонического полного запроса, субъекта авторизации, версии модели и версии используемых данных каталога. Учитываются КБЖУ, денежный предел, исключения, избранное и тип приёма пищи; частичный ключ по калориям/бюджету запрещён.
  - Векторный поиск/RAG — отдельное будущее расширение; текущая спецификация не требует нового индекса.
  - Fallback-буфер при сбоях или таймаутах внешнего API нейросети.

---

## 2. Сущности

- **`GeneratedMenuCache`**:
  - `cacheKey`: String (MD5/SHA256 от параметров запроса);
  - `recipesJson`: String (JSON-сериализованный список блюд);
  - `expiresAt`: Long (TTL 24 часа).
- **Контекст генерации** приходит от мобильного клиента в каждом запросе. Он включает доступные для этой генерации калории и БЖУ, выделенный денежный лимит, исключённые ингредиенты и ID выбранного избранного.
- Мобильный клиент сам определяет, сколько оставшегося недельного бюджета выделить на блюдо или день. На сервер не передаётся инструкция вычитать прошлые расходы; пользовательская сумма для рекомендаций не является платёжным лимитом.

---

## 3. Protobuf Контракт

- **Файл контракта:** `proto/nutritionist/recipe/v1/recipe_ai.proto`
- **Пакет:** `com.budgetntr.recipe.v1` (Java/Kotlin: `com.budgetntr.proto.recipe.v1`)

```protobuf
syntax = "proto3";

package com.budgetntr.recipe.v1;

service RecipeAiService {
  // GenerateDishSuggestion генерирует блюдо строго под оставшиеся КБЖУ и оставшиеся деньги
  rpc GenerateDishSuggestion(GenerateDishRequest) returns (GenerateDishResponse);

  // GenerateDailyMealPlan составляет полный рацион на день с минимизацией стоимости
  rpc GenerateDailyMealPlan(DailyMealPlanRequest) returns (DailyMealPlanResponse);
}

message Money {
  int64 amount_minor = 1;
  string currency_code = 2;
}

message GenerationContext {
  double available_calories = 1;
  double available_proteins_grams = 2;
  double available_fats_grams = 3;
  double available_carbs_grams = 4;
  Money spending_limit = 5;
  repeated string exclude_ingredients = 6;
  repeated string preferred_food_item_ids = 7;
  repeated string preferred_recipe_ids = 8;
  repeated string preferred_daily_meal_plan_ids = 9;
}

message GenerateDishRequest {
  GenerationContext context = 1;
  string meal_type = 2;
}

message GenerateDishResponse {
  string recipe_name = 1;
  string description = 2;
  repeated string instructions = 3;
  double estimated_calories = 4;
  Money estimated_price = 5;
  repeated AiIngredientProto ingredients = 6;
}

message AiIngredientProto {
  string food_item_id = 1;
  string name = 2;
  double quantity_grams = 3;
}

message DailyMealPlanRequest {
  GenerationContext context = 1;
}

message DailyMealPlanResponse {
  repeated GenerateDishResponse meals = 1;
  double total_calories = 2;
  Money estimated_price = 3;
}
```

---

## 4. Данные для генерации и отказоустойчивость

- Мобильный клиент вычисляет контекст по текущему профилю и дневнику Room, включая ещё не отправленные записи. Избранное берётся из локального желаемого состояния, включая офлайн-нажатия. Для генерации не нужно предварительно отправлять весь дневник или ждать Kafka.
- `context` и `spending_limit` обязательны; лимиты конечны и неотрицательны, деньги — RUB/копейки. Отрицательный остаток клиент показывает как превышение и не запускает генерацию с отрицательным пределом. При неполной истории или неизвестных стоимостях UI сообщает, что бюджет приблизительный, и пользователь задаёт лимит генерации явно.
- Recipe AI использует переданный лимит для подбора кандидатов по текущим данным каталога. Он может оценивать стоимость будущего предложения при подборе, но не рассчитывает заново расходы дневника и не изменяет бюджет профиля. `estimated_price` не гарантирует цену покупки; отсутствующее поле означает неизвестную оценку.
- Итог выбранной порции мобильный доменный слой рассчитывает по выбранным пользователем ценам и количеству, затем сохраняет снимок в дневнике. Сервер не выдаёт свой результат за подтверждённую стоимость клиента.
- ID предпочтений относятся только к существующим объектам каталога. Сервер проверяет доступ к ним и пропускает удалённые/недоступные предпочтения; ограничения по исключённым ингредиентам имеют приоритет. Список предпочтений из запроса не изменяет серверное избранное.
- Подписки на `diary.entry.created`, `user.profile.updated`, `user.favorite.changed` не нужны этому сценарию. В Redis нет накапливаемого по событиям персонального денежного баланса. `recipe.generated` остаётся отложенным событием без нового обязательного потребителя.
- Отказоустойчивость реализуется в инфраструктурном клиенте функциональными корутинными обёртками согласно [ADR-002](../../docs/architecture/adr/adr-002-resilience4j-integration.md), с таймаутом и пробросом отмены. Fallback должен удовлетворять тем же ограничениям; если подходящего кэша нет, возвращается явная недоступность, а не произвольное блюдо.
