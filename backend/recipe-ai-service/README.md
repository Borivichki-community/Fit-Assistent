# Recipe AI Service

`:backend:recipe-ai-service` генерирует недельное меню по суточной цели в ккал и недельному пределу в копейках RUB. Можно передать UUID исключённых и предпочтительных продуктов. Исключения обязательны, предпочтения учитываются среди доступных кандидатов.

## Контракт

[`GenerateWeekMenu`](../../proto/nutritionist/recipe/v1/recipe.proto) принимает `daily_calories`, `weekly_budget_minor`, `excluded_product_ids` и `preferred_product_ids`. После проверки всей недели сервер передаёт семь событий `DAY_READY` в порядке 1–7, затем одно `FINAL` с итоговой оценкой и источником меню.

- Catalog отдаёт продукты и среднюю цену за 100 г. Клиент собирает до двух страниц и дополнительно запрашивает первые 12 доступных предпочтений. В LLM передаются до 35 кандидатов с известной ценой. Если даже минимальная оценка по этим продуктам выше бюджета, LLM не вызывается.
- Gemini получает структурированную схему и возвращает семь дней с завтраком, обедом и ужином. Сервис проверяет ID продуктов и целые граммы, сам рассчитывает калории и цены. По умолчанию калории каждого дня должны быть в пределах ±10%, а стоимость всей недели — в пределах бюджета. Стоимость каждого продукта в порции округляется до копейки.
- Обращения к Catalog ограничены дедлайном на попытку. На `UNAVAILABLE` и `DEADLINE_EXCEEDED` выполняется до трёх попыток с backoff. У вызова LLM до двух попыток с таймаутом 12 секунд на каждую. Retry обрабатывает временные HTTP/сетевые ошибки и невалидное меню.
- Redis хранит результат 24 часа. Ключ учитывает пользователя, модель, параметры и состав снимка кандидатов. Предгенерированные меню для точной пары ккал × бюджет живут 48 часов. Fallback применим только к запросам без предпочтений и исключений. При отсутствии подходящего меню сервер завершает поток с `UNAVAILABLE`.
- На пользователя выделяется 10 попыток LLM за сутки UTC. Повторы занимают отдельную попытку. Когда Redis квоты недоступен, платная генерация останавливается. Общий лимит маршрута Envoy составляет 30 запросов в минуту. Actuator экспортирует состояние Circuit Breaker, число попыток, токены и оценку стоимости по заданному тарифу.

## Конфигурация

| Переменная | Назначение |
|---|---|
| `RECIPE_LLM_API_KEY` | Ключ Gemini из окружения или Secret `recipe-llm`, поле `api-key` |
| `RECIPE_LLM_MODEL` | Идентификатор модели Gemini |
| `RECIPE_CATALOG_TARGET` | Адрес gRPC Catalog с методами `SearchProducts`, `GetProduct` и `GetAveragePrices` |
| `RECIPE_CATALOG_PLAINTEXT` | Явное включение plaintext для локального Catalog. По умолчанию используется TLS |
| `RECIPE_CATALOGCAFILE` | Путь к PEM CA для TLS Catalog, если CA отсутствует в системном trust store |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Подключение к Redis |
| `RECIPE_WARMUP_TARGETS` | Пары вида `1800:300000,2200:400000`. Пустой список отключает фоновые вызовы Gemini |
| `RECIPE_LLM_INPUT_USD_PER_MILLION`, `RECIPE_LLM_OUTPUT_USD_PER_MILLION` | Тариф входа и выхода в USD за миллион токенов. Заполняются оба значения |

Kubernetes-манифесты находятся в [infrastructure/k8s/recipe](../../infrastructure/k8s/recipe/). Для маршрута нужны существующие Envoy, Keycloak, Redis и работающий gRPC Catalog. Ветка T-048 содержит сервер Catalog и его контракт.

Для TLS нужны три объекта. В namespace `budget-nutritionist` секрет `recipe-server-tls` содержит `tls.crt`, `tls.key` и `client-ca.crt`. Сертификат сервера должен иметь SAN `recipe-ai-service.budget-nutritionist.svc.cluster.local`, его CA размещается в ConfigMap `recipe-server-ca` в поле `ca.crt`. В namespace `envoy-gateway-system` TLS Secret `recipe-envoy-client-tls` содержит сертификат клиента с субъектом `CN=fitassistant-envoy` и закрытый ключ. JWT с `sub` проверяется в Envoy. Recipe AI принимает утверждённый шлюзом `sub` через канал mTLS.

После подготовки сертификатов и образа:

```bash
kubectl -n envoy-gateway-system patch envoyproxy custom-proxy-config --type=merge \
  --patch-file infrastructure/k8s/recipe/envoy-client-tls.patch.yaml
kubectl apply -f infrastructure/k8s/recipe/recipe.yaml
```

Для локального запуска через Compose gRPC без входного mTLS доступен для health-проверок. Для вызова генерации нужен TLS-профиль `gateway` и сертификат клиента Envoy. Пути к серверному PEM, ключу и CA клиента можно задать через `RECIPE_TLS_CERTIFICATE`, `RECIPE_TLS_PRIVATE_KEY` и `RECIPE_TLS_CLIENT_CA` с префиксом `file:`.

## Проверки

```bash
./gradlew -PbackendOnly=true :backend:recipe-ai-service:check :backend:recipe-ai-service:bootJar
```

WireMock покрывает успешный запрос, HTTP 500, медленный ответ, таймаут и невалидный JSON. In-process Catalog проверяет повтор запроса при временной ошибке, поток дней и fallback через кэш. Проверка атомарной квоты и хранения меню использует изолированный Redis в Testcontainers.
