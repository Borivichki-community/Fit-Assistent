# Recipe AI Service (`:backend:recipe-ai-service`)

> Микросервис генерации персонализированных блюд и меню под остаток суточного калоража и оставшийся бюджет с использованием ИИ (Gemini / LLM) и RAG по базе рецептов.

---

## 1. База данных и хранилище

- **СУБД / Кэш:** Redis 7 (порт `6379`).
- **Назначение Redis:**
  - Кэширование сгенерированных рецептов по хэшу параметров: `recipe:ai:cache:{hash(targetCalories, budget, dietaryFlags)}`;
  - Хранение векторных эмбеддингов для семантического поиска блюд;
  - Fallback-буфер при сбоях или таймаутах внешнего API нейросети.

---

## 2. Сущности

- **`GeneratedMenuCache`**:
  - `cacheKey`: String (MD5/SHA256 от параметров запроса);
  - `recipesJson`: String (JSON-сериализованный список блюд);
  - `expiresAt`: Long (TTL 24 часа).
- **`UserDietaryContext`** (in-memory):
  - `userId`: String;
  - `remainingCalories`: Double;
  - `remainingBudget`: Double;
  - `consumedProteinsGrams`: Double;
  - `consumedFatsGrams`: Double;
  - `consumedCarbsGrams`: Double.

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

message GenerateDishRequest {
  string user_id = 1;
  double remaining_calories = 2;
  double remaining_budget = 3;
  string meal_type = 4; // BREAKFAST, LUNCH, DINNER, SNACK
  repeated string exclude_ingredients = 5;
}

message GenerateDishResponse {
  string recipe_name = 1;
  string description = 2;
  repeated string instructions = 3;
  double estimated_calories = 4;
  double estimated_price = 5;
  repeated AiIngredientProto ingredients = 6;
}

message AiIngredientProto {
  string name = 1;
  double quantity_grams = 2;
  double estimated_price = 3;
}

message DailyMealPlanRequest {
  string user_id = 1;
  double target_calories = 2;
  double daily_budget = 3;
}

message DailyMealPlanResponse {
  repeated GenerateDishResponse meals = 1;
  double total_calories = 2;
  double total_price = 3;
}
```

---

## 4. Межсервисные события и отказоустойчивость

- **Consumed Events:**
  - Топик: `diary.entry.created` (Avro `CaloriesConsumedEvent`) — вычитает съеденные калории и потраченные деньги из суточного/недельного лимита пользователя.
- **Produced Events:**
  - Топик: `recipe.generated`
- **Resilience4j:**
  - `@CircuitBreaker(name = "llm-api", fallbackMethod = "getCachedRecipes")`
  - `@TimeLimiter(10s)`
  - При недоступности внешнего LLM API сервис возвращает проверенные популярные рецепты из Redis, не ломая пользовательский опыт.
