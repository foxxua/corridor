# Corridor

Модульна логістична платформа. Пет-проєкт: вчимось і показуємо.

## Що це і в якому стані

Скелет навчального проєкту на Kotlin + Spring Boot. Зараз є корінь-агрегатор Gradle і перший модуль `libs/contract`. Spring Boot, Postgres, Kafka і сервіси ще не підключені — йдемо крок за кроком.

## Команди

```powershell
.\gradlew.bat build                 # зібрати все
.\gradlew.bat build --console=plain # те саме, але видно список тасків
.\gradlew.bat :libs:contract:build  # один модуль
docker compose up -d                # локальна інфраструктура
docker compose down                 # зупинити (дані в volume лишаються)
docker compose down -v              # зупинити й стерти дані
```

На Windows завжди `.\gradlew.bat`, не `.\gradlew` — другий файл це bash-скрипт.

## Структура

```text
build.gradle.kts        корінь: лише оголошення версій плагінів (apply false)
settings.gradle.kts     список модулів + централізовані репозиторії
gradle.properties       дрібні прапорці Gradle
libs/contract           wire-типи: події, коди помилок, id. Без Spring і без логіки
compose.yaml            postgres, redis, kafka, kafka-ui (поки тільки postgres)
```

Порти, зарезервовані під сервіси: gateway `8080`, core `8081`, geo-route `8082`, delivery-plugin `8083`, kafka-ui `8090`.

## Домовленості

- **Корінь не застосовує плагінів.** Тільки `apply false`. Інакше він претендує бути застосунком, і `build` почне збирати jar із теки без `main`.
- **Модуль не вказує версію плагіна.** Версія оголошена в корені; інакше в білді зʼявиться дві різні версії одного плагіна.
- **Модуль не має блоку `repositories`.** Репозиторії роздає `dependencyResolutionManagement` у `settings.gradle.kts`.
- **Пакети:** `io.github.foxx.corridor.<модуль>`. Тека дзеркалить пакет.
- **`libs/contract` містить лише те, що літає по дроту.** Entity, репозиторії й сервіси туди не потрапляють, інакше модульність стає distributed monolith.
- **Версії бібліотек** зійдуться в `gradle/libs.versions.toml`, коли зʼявиться Spring Boot. Зараз передчасно.

## Теорія — не тут

Навчальні тексти (розділи, пояснення, довідники, лабораторії) живуть в окремому репозиторії `C:\Users\foxx\theory` і пишуться за скілом `.opencode/theory-writing/SKILL.md` у ньому.

У цьому репозиторії — код, `README.md` і, згодом, короткі ADR у `docs/adr/`. Якщо під час роботи виникає матеріал «як це працює всередині» — він іде в `theory`, а тут лишається посилання.
