# Food CV Service (`:backend:food-cv-service`)

> Микросервис компьютерного зрения (Computer Vision / Multimodal AI) для распознавания продуктов и готовых блюд по фотографии.

---

## 1. База данных и хранилище

- **СУБД / Кэш:** Redis 7 (порт `6379`).
- **Назначение Redis:**
  - Кэширование распознанных результатов по перцептивному хэшу изображения (`pHash`), исключая повторные платные запросы к Vision API для одинаковых блюд.

---

## 2. Сущности

- **`DetectedFoodItem`**:
  - `foodItemId`: UUID (связка с каталогом `food_items`);
  - `name`: String (название блюда/продукта);
  - `confidence`: Double (достоверность классификации от 0.0 до 1.0);
  - `estimatedQuantityGrams`: Double (оценочный вес порции по объёму на фото);
  - `boundingBox`: Координаты объекта на изображении.

---

## 3. Protobuf Контракт

- **Файл контракта:** `proto/nutritionist/food_cv/v1/food_cv.proto`
- **Пакет:** `com.budgetntr.foodcv.v1` (Java/Kotlin: `com.budgetntr.proto.foodcv.v1`)

```protobuf
syntax = "proto3";

package com.budgetntr.foodcv.v1;

service FoodCvService {
  rpc DetectFoodFromImage(DetectFoodRequest) returns (DetectFoodResponse);
}

message DetectFoodRequest {
  bytes image_data = 1;     // JPEG/PNG байты
  string image_format = 2;  // "jpeg", "png"
}

message DetectFoodResponse {
  repeated DetectedItemProto items = 1;
}

message DetectedItemProto {
  string matched_food_item_id = 1;
  string name = 2;
  double confidence = 3;
  double estimated_grams = 4;
  double calories = 5;
  double proteins = 6;
  double fats = 7;
  double carbs = 8;
}
```

---

## 4. Межсервисные события

- **Produced Events:**
  - Топик: `food.detected` (публикуется при успешной детекции для аналитики).
