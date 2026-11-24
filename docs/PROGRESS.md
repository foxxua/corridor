# Прогрес

Оновлено: 24.11.2026

Цей файл — точка входу для того, хто повертається до проєкту після перерви: що вже стоїть, чому саме так і з чого продовжувати. Живий стан, а не історія: застаріле виправляється, а не дописується внизу.

## Де ми зараз

Стоїмо на кроці 3 з 10 — локальна інфраструктура. Готовий каркас збірки й перший модуль, перевірено збірку. `compose.yaml` ще не створений, тому Postgres не піднімався, і жоден застосунок Spring ще не існує.

## Що вже зроблено

| Що | Стан | Як перевірено |
|---|---|---|
| Корінь-агрегатор Gradle | готово | `.\gradlew.bat build` → BUILD SUCCESSFUL |
| Модуль `libs/contract` з `OrderId` | готово, компілюється | той самий запуск |
| `.gitignore` із політикою для `.idea` | готово | `git status` чистий |
| Перший коміт | `a84e198` | `git log --oneline` |
| `compose.yaml` | ні, наступний крок | — |
| Сервіси Spring | ні | — |

Точний вміст кореневих файлів станом на зараз:

- `settings.gradle.kts` — плагін foojay, `dependencyResolutionManagement` з `mavenCentral()`, `include(":libs:contract")`;
- `build.gradle.kts` — тільки `kotlin("jvm") version "2.4.20" apply false`, `group`, `version`;
- `libs/contract/build.gradle.kts` — `kotlin("jvm")` без версії і `jvmToolchain(21)`.

## Рішення, які вже прийняті

| Рішення | Чому | Що буде, якщо змінити |
|---|---|---|
| Один репозиторій, один Gradle multi-project build. Не git submodules | один розробник; submodules додають подвійний коміт на кожну зміну контракту й ламають рефакторинг через межу модулів | submodules виправдані лише за різних релізних циклів або різних прав доступу |
| Корінь не застосовує плагінів, лише `apply false` | інакше корінь претендує бути застосунком і `build` збирає jar із теки без `main` | прибрати `apply false` — і збірка почне падати на Spring Boot |
| Репозиторії централізовано в `settings.gradle.kts` | вони властивість білда, а не модуля; інакше копіюються в кожен `build.gradle.kts` | локальний `repositories` у модулі перекриє централізований (режим PREFER_PROJECT) |
| `.idea` частково в git: `misc`, `gradle`, `vcs`, `kotlinc`, `runConfigurations`, `codeStyles`, `inspectionProfiles` | налаштування мають бути однакові в IDE, CLI та агента; `workspace.xml` та згенеровані файли — ні | ігнорувати `.idea` повністю означає втратити run-конфіги при переносі |
| JDK 21 через `jvmToolchain`, плагін foojay | у PATH стоїть Java 26; toolchain фіксує компіляцію, foojay дає Gradle право скачати JDK | без foojay — `No matching toolchains found` |
| Навчальна теорія живе в `C:\Users\foxx\theory` | код і навчальний текст мають різні цикли й різних читачів | дублювання в двох репозиторіях розповзається за тиждень |

## Версії

Зафіксовані зараз: Kotlin `2.4.20` (те, що вбудувала IDEA), Gradle `9.6.0` (wrapper), JDK `21`.

Заплановані, але ще не підключені: Spring Boot `4.2.0`, Spring Cloud `2026.0.0` (Gateway 5.1.0), springdoc `3.2.1`, Kafka `4.4.0`, PostgreSQL `18`, Redis `8`.

Каталог версій `gradle/libs.versions.toml` з'явиться разом зі Spring Boot — до того він додає шум без користі.

## Питання, на якому зупинились

У `compose.yaml` для Postgres є блок `ports: "5432:5432"`. Питання: **що станеться, якщо його прибрати, а `core` запустити через `bootRun`?** Відповідь потрібна до переходу до `volumes` і `healthcheck`, бо без неї ці блоки виглядають як магія.

## Наступні кроки, по порядку

1. `compose.yaml` з одним сервісом Postgres; `docker compose up -d postgres`; перевірка `psql -c "select version();"`.
2. `volumes` (чому дані зникають без них) і `healthcheck` (чим `depends_on` без `condition` відрізняється від `condition: service_healthy`).
3. Redis: `command` з `appendonly`, перевірка `redis-cli ping`.
4. Kafka в KRaft-режимі: чому ZooKeeper більше немає, і головна пастка — `KAFKA_ADVERTISED_LISTENERS` (`localhost:9092` для хоста проти `kafka:9092` для мережі compose).
5. kafka-ui (`kafbat/kafka-ui`) для очей.
6. Сервіс `core`: Spring Boot, `/actuator/health`, підключення до Postgres.
7. Далі за планом 10 днів: ідемпотентність, outbox, geo-route, плагін, адмінка.

## Як відновити середовище з нуля

```powershell
cd C:\Users\foxx\IdeaProjects\corridor
.\gradlew.bat build --console=plain
docker compose up -d
docker compose ps
```

Інфраструктура з піднятими портами: Postgres `5432`, Redis `6379`, Kafka `9092`, kafka-ui `8090`. Зарезервовані під сервіси: gateway `8080`, core `8081`, geo-route `8082`, delivery-plugin `8083`.

## Домовленості

- Корінь не застосовує плагінів; модуль не вказує версію плагіна; модуль не має блоку `repositories`.
- Пакети `io.github.foxx.corridor.<модуль>`, тека дзеркалить пакет.
- `libs/contract` містить лише те, що літає по дроту. Entity, репозиторії й сервіси — ні.
- Команди Gradle на Windows — через `.\gradlew.bat`.
