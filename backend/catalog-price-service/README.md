# Catalog & Price Service (`:backend:catalog-price-service`)

> Микросервис каталога продуктов питания, базы рецептов блюд, истории и актуальных цен из супермаркетов, а также пользовательских избранных блюд.

---

## 1. База данных и схема (`catalog_db`)

- **СУБД:** PostgreSQL 17 (порт `5432`, база `catalog_db`).
- **Кэш:** Redis 7 (кэширование цен и каталога для быстрых выборок).
- **Инструмент миграций:** Liquibase (`src/main/resources/db/changelog/db.changelog-master.xml`).

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
    price_per_100g NUMERIC(10, 2) NOT NULL,
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
  rpc ToggleFavoriteRecipe(ToggleFavoriteRecipeRequest) returns (ToggleFavoriteRecipeResponse);
}

message FoodItemProto {
  string id = 1;
  string name = 2;
  string category_id = 3;
  double calories_per_100g = 4;
  double proteins_per_100g = 5;
  double fats_per_100g = 6;
  double carbs_per_100g = 7;
  double price_per_100g = 8;
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
  double max_price = 1;
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
  double total_price = 5;
  int32 prep_time_minutes = 6;
  repeated RecipeIngredientProto ingredients = 7;
}

message RecipeIngredientProto {
  string food_item_id = 1;
  string food_name = 2;
  double quantity_grams = 3;
}

message ToggleFavoriteRecipeRequest {
  string user_id = 1;
  string recipe_id = 2;
}

message ToggleFavoriteRecipeResponse {
  bool is_favorite = 1;
}
```

---

## 4. Межсервисные события (Kafka + Avro)

- **Produced Events:**
  - Топик: `food.price.updated` (публикуется при изменении цен продуктов в каталоге).
- **Consumed Events:**
  - Топик: `user.profile.updated` (учитывает предпочтения и аллергии пользователя).
