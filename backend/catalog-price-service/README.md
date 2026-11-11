# Catalog & Price Service (`:backend:catalog-price-service`)

> Микросервис каталога продуктов питания, базы рецептов блюд, истории и актуальных цен из супермаркетов, а также пользовательских избранных блюд.

> **Статус:** целевая спецификация. Таблицы, JPA-сущности и Catalog API ещё не реализованы; SQL и Protobuf ниже — проекты, не миграции и не опубликованные контракты.

---

## 1. База данных и схема (`catalog_db`)

- **СУБД:** PostgreSQL 17 (порт `5432`, база `catalog_db`).
- **Кэш:** Redis 7 (кэширование цен и каталога для быстрых выборок).
- **Инструмент миграций:** Liquibase (`src/main/resources/db/changelog/db.changelog-master.yaml`).

### Схема таблиц каталога (`001-catalog-schema.xml`)

```sql
-- Категории продуктов (Крупы, Мясо, Овощи, Молочные продукты и т.д.)
CREATE TABLE food_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    icon_url VARCHAR(255)
);

-- Продукты питания со справочными КБЖУ на 100 грамм
CREATE TABLE food_items (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES food_categories(id),
    name VARCHAR(255) NOT NULL,
    barcode VARCHAR(64),
    calories_per_100g NUMERIC(6, 2) NOT NULL,
    proteins_per_100g NUMERIC(6, 2) NOT NULL,
    fats_per_100g NUMERIC(6, 2) NOT NULL,
    carbs_per_100g NUMERIC(6, 2) NOT NULL,
    price_per_100g NUMERIC(10, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_food_category_protein ON food_items(category_id, proteins_per_100g);
CREATE INDEX idx_food_barcode ON food_items(barcode);

-- Рецепты блюд
CREATE TABLE recipes (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    instructions TEXT,
    total_calories NUMERIC(7, 2) NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    prep_time_minutes INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recipes_calories_price ON recipes(total_calories, total_price);

-- Ингредиенты рецепта (связка N:M между рецептами и продуктами)
CREATE TABLE recipe_ingredients (
    id UUID PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    food_item_id UUID NOT NULL REFERENCES food_items(id),
    quantity_grams NUMERIC(7, 2) NOT NULL
);

CREATE INDEX idx_recipe_ingredients_recipe_id ON recipe_ingredients(recipe_id);

-- Избранные блюда и рецепты пользователей
CREATE TABLE user_favorite_recipes (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_favorite UNIQUE(user_id, recipe_id)
);

CREATE INDEX idx_user_favorite_user ON user_favorite_recipes(user_id);
```

---

## 2. JPA Сущности

- **`FoodItemEntity`**: справочный продукт (КБЖУ на 100 г, базовая цена).
- **`FoodCategoryEntity`**: категория продуктов.
- **`RecipeEntity`**: рецепт с ленивой связью `@OneToMany` на `RecipeIngredientEntity` (защита от N+1 через `@EntityGraph`).
- **`RecipeIngredientEntity`**: ингредиент рецепта с граммовкой.
- **`UserFavoriteRecipeEntity`**: закладка избранного блюда пользователя.

---

## 3. Protobuf Контракт

- **Файл контракта:** `proto/nutritionist/catalog/v1/catalog.proto`
- **Пакет:** `com.budgetntr.catalog.v1` (Java/Kotlin: `com.budgetntr.proto.catalog.v1`)

```protobuf
syntax = "proto3";

package com.budgetntr.catalog.v1;

service CatalogService {
  rpc GetFoodItem(GetFoodItemRequest) returns (FoodItemProto);
  rpc SearchFoodItems(SearchFoodItemsRequest) returns (SearchFoodItemsResponse);
  rpc GetBudgetRecipes(GetBudgetRecipesRequest) returns (GetBudgetRecipesResponse);
  rpc SetFavorite(SetFavoriteRequest) returns (SetFavoriteResponse);
  rpc ListFavorites(ListFavoritesRequest) returns (ListFavoritesResponse);
}

message GetFoodItemRequest {
  string id = 1;
}

message Money {
  int64 amount_minor = 1;
  string currency_code = 2;
}

message FoodItemProto {
  string id = 1;
  string name = 2;
  string category_id = 3;
  double calories_per_100g = 4;
  double proteins_per_100g = 5;
  double fats_per_100g = 6;
  double carbs_per_100g = 7;
  Money price_per_100g = 8;
  string barcode = 9;
}

message SearchFoodItemsRequest {
  string query = 1;
  int32 limit = 2;
  int32 offset = 3;
}

message SearchFoodItemsResponse {
  repeated FoodItemProto items = 1;
}

message GetBudgetRecipesRequest {
  Money max_price = 1;
  double target_calories = 2;
}

message GetBudgetRecipesResponse {
  repeated RecipeProto recipes = 1;
}

message RecipeProto {
  string id = 1;
  string name = 2;
  string description = 3;
  double total_calories = 4;
  Money estimated_price = 5;
  int32 prep_time_minutes = 6;
  repeated RecipeIngredientProto ingredients = 7;
}

message RecipeIngredientProto {
  string food_item_id = 1;
  string food_name = 2;
  double quantity_grams = 3;
}

enum FavoriteTargetType {
  FAVORITE_TARGET_TYPE_UNSPECIFIED = 0;
  FAVORITE_TARGET_TYPE_FOOD_ITEM = 1;
  FAVORITE_TARGET_TYPE_RECIPE = 2;
  FAVORITE_TARGET_TYPE_DAILY_MEAL_PLAN = 3;
}

message FavoriteTarget {
  FavoriteTargetType type = 1;
  string target_id = 2;
}

message SetFavoriteRequest {
  FavoriteTarget target = 1;
  optional bool is_favorite = 2;
}

message SetFavoriteResponse {
  FavoriteTarget target = 1;
  bool is_favorite = 2;
}

message ListFavoritesRequest {
  int32 page_size = 1;
  string page_token = 2;
}

message ListFavoritesResponse {
  repeated FavoriteTarget targets = 1;
  string next_page_token = 2;
}
```

---

## 4. Семантика избранного и цен

- Единственная мутация — `SetFavorite(target, is_favorite)`: `true` добавляет, `false` удаляет. Поле обязательно по presence; `UNSPECIFIED` и пустой ID запрещены. Пользователь определяется по проверенному субъекту авторизации, а не телу запроса.
- Каталог владеет продуктами (`FOOD_ITEM`), сохранёнными рецептами (`RECIPE`) и шаблонами дневных меню (`DAILY_MEAL_PLAN`). Связи `user_favorite_foods` и `user_favorite_meal_plans` аналогичны показанной `user_favorite_recipes`: UUID PK, user_id, FK на объект каталога, added_at, UNIQUE(user_id, target_id). Разовый результат AI пока не имеет постоянного ID каталога; его сохранение в избранное требует отдельной будущей функции сохранения в каталог.
- Повтор установки того же состояния успешен. Удаление отсутствующей связи также успешно, даже если объект каталога уже удалён. Добавление отсутствующего объекта — `NOT_FOUND`.
- Обязательный `idempotency-key` равен ID локальной операции. В одной транзакции сервер проверяет запись дедупликации `(subject, RPC, key)`, fingerprint запроса, меняет связь и сохраняет ответ. Уникальное ограничение сериализует конкурентные дубли. Повтор возвращает исходный ответ без изменения связи; другой payload со старым ключом — `INVALID_ARGUMENT`.
- Дедупликация хранится в БД вместе с аккаунтом, а не только в Redis с TTL. Очистка ключей по времени требует отдельного протокола ограничения возраста офлайн-операций. Иначе поздний дубль добавления может отменить новое удаление.
- `ListFavorites` восстанавливает данные на другом устройстве. page_size: 1–100 (0 означает 50); page_token привязан к пользователю и снимку списка. Клиент применяет снимок только если локальная очередь была пуста и не менялась с начала чтения; иначе повторяет чтение.
- Между устройствами действует порядок фиксации на сервере. Гарантии глобального порядка офлайн-нажатий нет. После отправки очереди клиент обновляет серверный снимок.
- Цены каталога — текущие справочные предложения. `Money` передаёт целые минимальные единицы, для MVP — RUB/копейки. Отсутствующий `price_per_100g` означает неизвестную цену, не ноль. Оценка рецепта нужна для поиска кандидатов; сумму выбранной порции рассчитывает мобильный клиент.

## 5. Межсервисные события

`food.price.updated` — отложенный вариант уведомления об обновлении каталога; схема и потребители не реализованы. Дневник не подписывается на него и не переписывает расходы.

Для текущего сценария не требуются `user.profile.updated` и `user.favorite.changed`: ограничения и локальное избранное приходят в запросе генерации. Новые Avro-схемы и подписки не вводятся. При появлении подтверждённого потребителя события публикация выполняется через Transactional Outbox.
