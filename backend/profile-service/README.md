# Profile Service (`:backend:profile-service`)

> Микросервис управления биометрическим профилем пользователя, целями питания (КБЖУ) и недельным бюджетом.

---

## 1. База данных и схема (`profile_db`)

- **СУБД:** PostgreSQL 17 (порт `5432`, база `profile_db`).
- **Инструмент миграций:** Liquibase (`src/main/resources/db/changelog/db.changelog-master.xml`).
- **Стратегия первичных ключей:** UUID v7 / Sequential UUID.

### Схема таблицы `profiles` (`001-init-profile-table.xml`)

```sql
CREATE TABLE profiles (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    height_cm INTEGER NOT NULL,
    weight_kg NUMERIC(5, 2) NOT NULL,
    gender VARCHAR(20) NOT NULL,            -- MALE, FEMALE, UNSPECIFIED
    activity_level VARCHAR(30) NOT NULL,    -- SEDENTARY, LIGHT, MODERATE, VERY_ACTIVE, UNSPECIFIED
    goal_type VARCHAR(30) NOT NULL,         -- LOSE_WEIGHT, MAINTAIN, GAIN_WEIGHT, UNSPECIFIED
    target_calories INTEGER NOT NULL,
    target_proteins_grams NUMERIC(6, 2) NOT NULL,
    target_fats_grams NUMERIC(6, 2) NOT NULL,
    target_carbs_grams NUMERIC(6, 2) NOT NULL,
    target_weight_kg NUMERIC(5, 2) NOT NULL,
    weekly_budget NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profiles_user_id ON profiles(user_id);
```

---

## 2. JPA Сущности

- **`ProfileEntity`** (`dev.andre.nutritionist.profile.persistence.entity.ProfileEntity`):
  Корень агрегата профиля. Содержит биометрию, пол, коэффициент активности, бюджет, тип цели и встраиваемый объект `nutritionGoal`.
- **`NutritionGoalEmbeddable`** (`dev.andre.nutritionist.profile.persistence.entity.NutritionGoalEmbeddable`):
  Встраиваемый (`@Embeddable`) компонент нутрициологических целей (целевые калории, белки, жиры, углеводы, целевой вес). Соответствует `NutritionGoalProto`.
- **`Enums`**:
  - `Gender` (`MALE`, `FEMALE`, `UNSPECIFIED`)
  - `ActivityLevel` (`SEDENTARY`, `LIGHT`, `MODERATE`, `VERY_ACTIVE`, `UNSPECIFIED`) — коэффициент образа жизни для расчета метаболизма (TDEE).
  - `GoalType` (`LOSE_WEIGHT`, `MAINTAIN`, `GAIN_WEIGHT`, `UNSPECIFIED`)

---

## 3. Protobuf Контракт

- **Файл контракта:** `proto/nutritionist/profile/v1/profile.proto`
- **Пакет:** `com.budgetntr.profile.v1` (Java/Kotlin: `com.budgetntr.proto.profile.v1`)

```protobuf
syntax = "proto3";

package com.budgetntr.profile.v1;

import "google/protobuf/timestamp.proto";

service ProfileService {
  rpc GetProfile(GetProfileRequest) returns (ProfileProto);
  rpc UpdateProfile(UpdateProfileRequest) returns (ProfileProto);
}

enum GenderProto {
  GENDER_UNSPECIFIED = 0;
  GENDER_MALE = 1;
  GENDER_FEMALE = 2;
}

enum ActivityLevelProto {
  ACTIVITY_LEVEL_UNSPECIFIED = 0;
  ACTIVITY_LEVEL_SEDENTARY = 1;
  ACTIVITY_LEVEL_LIGHT = 2;
  ACTIVITY_LEVEL_MODERATE = 3;
  ACTIVITY_LEVEL_VERY_ACTIVE = 4;
}

enum GoalTypeProto {
  GOAL_TYPE_UNSPECIFIED = 0;
  GOAL_TYPE_LOSE_WEIGHT = 1;
  GOAL_TYPE_MAINTAIN = 2;
  GOAL_TYPE_GAIN_WEIGHT = 3;
}

message NutritionGoalProto {
  int32 target_calories = 1;
  double target_proteins_grams = 2;
  double target_fats_grams = 3;
  double target_carbs_grams = 4;
  double target_weight_kg = 5;
}

message ProfileProto {
  string id = 1;
  string user_id = 2;
  int32 height_cm = 3;
  double weight_kg = 4;
  GenderProto gender = 5;
  ActivityLevelProto activity_level = 6;
  GoalTypeProto goal_type = 7;
  NutritionGoalProto nutrition_goal = 8;
  google.protobuf.Timestamp updated_at = 9;
  double weekly_budget = 10;
}
```

---

## 4. Межсервисные события (Kafka + Avro)

- **Produced Events:**
  - Топик: `user.profile.updated`
  - Схема Avro: `shared/contracts/src/main/avro/UserRegisteredEvent.avsc`
  - Потребители: `catalog-price-service`, `recipe-ai-service`.
