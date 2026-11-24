# Прогрес

Оновлено: 24.11.2026

Цей файл — точка входу для того, хто повертається до проєкту після перерви: що вже стоїть, чому саме так і з чого продовжувати. Живий стан, а не історія: застаріле виправляється, а не дописується внизу.

## Де ми зараз

Сервіс `core` працює на Spring Boot 4.2.0. Він слухає порт `8081`, віддає `/actuator/health` і `/actuator/info`, підключається до Postgres через JDBC і `JdbcClient`. Локальні секрети читаються з `.env` через профіль `local`. Є інтеграційний тест на Testcontainers; `.\gradlew.bat build` проходить.

Redis і Kafka описані в `compose.yaml` і перевірені окремо, але `core` до них ще не підключений. Доменних таблиць і міграцій немає.

Наступний крок — перший вертикальний зріз `orders`: Flyway, таблиця, репозиторій, REST і тести. Після нього — подія `OrderCreated`, Kafka та outbox.

## Що вже зроблено

| Що | Стан | Як перевірено |
|---|---|---|
| Корінь-агрегатор Gradle і каталог версій | готово | `.\gradlew.bat build` → BUILD SUCCESSFUL |
| Модуль `libs/contract` з `OrderId` | готово, компілюється | той самий запуск |
| `.gitignore` із політикою для `.idea` | готово | `git status` |
| Коміти основи проєкту | `a84e198`, `be23776` | `git log --oneline` |
| `compose.yaml`: Postgres, Redis, Kafka, kafka-ui | готово | `docker compose ps`, `psql`, `PING`, тема Kafka |
| `.env.example` | готово | `.env` у `.gitignore`, секрет не в git |
| `services/core` на Spring Boot 4.2.0 | готово | `bootRun`, лог `Started CoreApplicationKt` |
| `/actuator/health` на `8081` | готово | `status: UP`, компонент `db: UP` |
| `/actuator/info` з даними Postgres | готово | `database`, `user`, `version` реального з'єднання |
| Postgres через JDBC/JdbcClient | готово | health та info виконують запити до `corridor` |
| Профіль `local` і секрети з `.env` | готово | `bootRun` з кореня, `db: UP` |
| Інтеграційний тест на Testcontainers | готово | `:services:core:test` → 1 тест, 0 падінь |
| Redis і Kafka в `core` | ні, наступні кроки | — |
| Flyway, домен і таблиці | ні, наступний крок | — |
| Dockerfile для `core` | ні, після домену й подій | — |

Станом на це оновлення зміни етапу `core` ще не закомічені: `git status` показує зміни в build-файлах, `settings.gradle.kts`, `libs/contract`, `compose.yaml`, `.env.example`, `docs/PROGRESS.md` і нову теку `services/`. Наступний крок — зробити checkpoint-коміт, перш ніж додавати домен.

### Стан інфраструктури

- **Postgres 18.6**, том `corridor-pg` змонтований у `/var/lib/postgresql` (не в `/data`), порт `5432`, healthcheck через `pg_isready -U corridor -d corridor`.
- **Redis 8.12.0**, `--appendonly yes`, том `corridor-redis` у `/data`, порт `127.0.0.1:6379`.
- **Kafka 4.4.0** в KRaft, вузол 1, ролі `broker,controller`. Слухачі: `INTERNAL:29092`, `EXTERNAL:9092`, `CONTROLLER:9093`. Рекламуються `kafka:29092` для compose і `localhost:9092` для хоста. Том `corridor-kafka` у `/var/lib/kafka/data`, `KAFKA_LOG_DIRS` вказує туди ж. Порт `127.0.0.1:9092`.
- **kafka-ui v1.5.1**, порт `8090`, bootstrap `kafka:29092`, стартує після `condition: service_healthy`.
- Тема `corridor.orders` створена вручну, розширена до 3 партицій. `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`, тому нові теми створюють явно.
- Перевірені ключі, партиції, offsets і consumer group `corridor-learners` з `LAG=0`.
- `container_name` прибрано, щоб не ламати масштабування й копії проєкту.

## Рішення, які вже прийняті

| Рішення | Чому | Що буде, якщо змінити |
|---|---|---|
| Один репозиторій, один Gradle multi-project build. Не git submodules | один розробник; submodules додають подвійний коміт на кожну зміну контракту й ламають рефакторинг через межу модулів | submodules виправдані лише за різних релізних циклів або різних прав доступу |
| Корінь не застосовує плагінів, лише `apply false` | інакше корінь претендує бути застосунком і `build` збирає jar із теки без `main` | прибрати `apply false` — і збірка почне падати на Spring Boot |
| Версії плагінів і бібліотек — у `gradle/libs.versions.toml`; модуль застосовує alias без версії | одне джерело версій; інакше в білді з'являються дві різні версії одного плагіна | дублювання версій у модулях швидко розсинхронізується |
| Spring Boot BOM підключається як `platform(...)`, без `io.spring.dependency-management` | штатний механізм Gradle; менше конфліктів із новими можливостями Gradle | dependency-management plugin додає зайвий шар і конфліктує з BOM |
| Kotlin `2.4.21` для плагінів і BOM Spring Boot | BOM Boot 4.2.0 керує саме `2.4.21`; інакше компілятор і `kotlin-stdlib` розходяться | patch-версії зазвичай сумісні, але діагностувати розбіжність довше |
| Стартер `spring-boot-starter-webmvc` замість `web` | у Boot 4.2 `web` позначений deprecated і є обгорткою | повернення `web` тягне попередження й непотрібний шар |
| `application.yaml` — спільне; `application-local.yaml` — лише локальний запуск | тести не отримують локальний datasource і `.env`; прод працює через змінні середовища | якщо тримати все в базовому файлі, тести залежать від машини розробника |
| Профіль `local` активується ззовні (`bootRun` args, IDE, env) | у `application.yaml` він успадкувався б і тестами | `spring.profiles.active: local` у базовому файлі робить тести залежними від `.env` |
| `bootRun` працює з робочою текою — корінь репозиторію | тоді `optional:file:.env` читається однозначно і з CLI, і з IDE | з текою модуля шлях стає `../../.env` і ламається в IDE |
| Секрети — лише в `.env`; у YAML тільки `${POSTGRES_PASSWORD}` | `.env` ігнорується git, `application-local.yaml` можна комітити | пароль у YAML потрапляє в історію репозиторію |
| JDBC + `JdbcClient`, без JPA і Flyway на першому кроці | немає доменних сутностей; JPA додає мапінг, `ddl-auto` і Hibernate без користі | JPA зараз збільшує кількість рухомих частин до появи моделі |
| `JdbcClient` замість `JdbcTemplate` для нового коду | іменовані параметри й ланцюжковий API; `JdbcTemplate` лишається під капотом | `JdbcTemplate` не deprecated, але з багатьма параметрами легше помилитися |
| Testcontainers 2.0.5 і конфігурація через `@TestConfiguration` + `@ServiceConnection` | контейнер описаний в одному місці, життєвий цикл керує Spring Boot; `@Testcontainers` не потрібен | `@Container` у кожному тесті дублює конфігурацію; разом із біном буде два контейнери |
| Образ у тестах — `postgres:18.6`, як у compose | локальна й тестова бази не розходяться за версією | `latest` робить тести непередбачуваними |
| Тести не використовують профіль `local` і `.env` | Testcontainers сам дає URL, користувача й пароль; тест повторюваний у CI | тести на `localhost:5432` залежать від стану машини й можуть псувати дані |
| `show-details: always` для health | локальна діагностика: видно компоненти й причину `DOWN` | у продакшені деталі health розкривають інфраструктуру; там їх ховають |
| `core` спершу через `bootRun` | швидкий цикл розробки; контейнеризація після робочого домену й подій | контейнер одразу вимагає Dockerfile і перезбірки на кожну зміну |

## Версії

Зафіксовані зараз: Kotlin `2.4.21`, Gradle `9.6.0` (wrapper), JDK `21`.

Spring Boot `4.2.0` керує: Spring Framework `7.1.0`, Tomcat `11.0.27`, HikariCP `7.1.0`, драйвер PostgreSQL `42.7.14`, Testcontainers `2.0.5`, JUnit Jupiter `6.1.3`, Flyway `12.11.0`, Jackson `3.1.8`.

Інфраструктура: `postgres:18.6`, `redis:8.12.0`, `apache/kafka:4.4.0`, `kafbat/kafka-ui:v1.5.1`.

Заплановані, але ще не підключені: Spring Cloud `2026.0.0` (Gateway `5.1.0`), springdoc `3.2.1`. Flyway підключається разом із першим доменом. JPA — лише якщо буде вирішено відмовитися від явного SQL.

Важливо для Boot 4: Jackson 3 має координату Kotlin-модуля `tools.jackson.module:jackson-module-kotlin`, а не `com.fasterxml.jackson.module`. Для REST із Kotlin `data class` знадобляться цей модуль і `kotlin-reflect`.

## Питання, на якому зупинились

1. Що робити наступним: перший зріз `orders` чи спершу підключити Redis і Kafka? Рекомендація — `orders`, бо без домену й події Kafka лишається демонстрацією.
2. Якщо `orders`: доступ до бази через `JdbcClient` чи вводити JPA вже зараз?
3. `OrderId` у контракті лишається обгорткою над `String` чи стає `UUID`?
4. Як зберігати статус замовлення: `varchar` + check constraint чи окремий Postgres enum?

## Наступні кроки, по порядку

1. Зробити checkpoint-коміт поточного стану й оновити цей файл разом із ним.
2. Підключити Flyway: `spring-boot-starter-flyway` + `flyway-database-postgresql`, створити `V1__create_orders.sql`.
3. Зробити вертикальний зріз `orders`: домен, репозиторій, сервіс, `POST /api/v1/orders`, `GET /api/v1/orders/{id}`.
4. Додати для REST `tools.jackson.module:jackson-module-kotlin` і `kotlin-reflect`; за потреби — `spring-boot-starter-validation`.
5. Покрити Testcontainers-тестами міграцію, створення, читання й `404`.
6. Додати `OrderCreated` у `libs/contract`.
7. Підключити Kafka: публікація події, consumer group, outbox для гарантії доставки.
8. Redis — для ідемпотентності або кешу.
9. Dockerfile для `core` і перехід на імена сервісів `postgres`, `redis`, `kafka`.
10. Далі за планом: geo-route, delivery-plugin, адмінка.

## Як відновити середовище з нуля

```powershell
cd C:\Users\foxx\IdeaProjects\corridor
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d
docker compose ps
.\gradlew.bat build
```

Запуск `core`:

```powershell
.\gradlew.bat :services:core:bootRun
```

`bootRun` сам активує профіль `local`, бере `.env` з кореня й слухає `8081`.

Перевірка:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8081/actuator/info
```

`health` має показати `db: UP`, `info` — реальні `database`, `user` і `version` з Postgres.

Тести:

```powershell
.\gradlew.bat :services:core:test
```

Тестам потрібен запущений Docker: Testcontainers сам піднімає `postgres:18.6`, compose для тестів не потрібен.

Перевірка інфраструктури:

```powershell
docker compose exec postgres psql -U corridor -d corridor -c "select current_database(), current_user;"
docker compose exec redis redis-cli ping
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Якщо том Kafka видалили, тему створюють явно:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic corridor.orders --partitions 3 --replication-factor 1
```

Порти: Postgres `5432`, Redis `127.0.0.1:6379`, Kafka `127.0.0.1:9092`, kafka-ui `8090`. Зарезервовані під сервіси: gateway `8080`, core `8081`, geo-route `8082`, delivery-plugin `8083`.

Якщо `core` не може підключитися з `password authentication failed`, а `.env` містить правильний пароль — том Postgres був ініціалізований з іншим паролем. Пароль задається лише під час першої ініціалізації; варіанти — узяти старий пароль або перестворити том (`docker compose down -v`, дані буде втрачено).

В IDE для `core` потрібні: робоча тека — корінь репозиторію, активний профіль — `local`.

Теорія: розділ Docker і Compose завершено в `C:\Users\foxx\theory\docker\containers-and-compose` (17 підрозділів і лаби). Теорія Kafka поки що живе лише в чаті: окремий розділ у `theory/kafka` не створювали.

## Домовленості

- Корінь не застосовує плагінів; модуль не вказує версію плагіна; модуль не має блоку `repositories`.
- Пакети `io.github.foxx.corridor.<модуль>`, тека дзеркалить пакет.
- `libs/contract` містить лише те, що літає по дроту. Entity, репозиторії й сервіси — ні.
- Команди Gradle на Windows — через `.\gradlew.bat`.
- `compose.yaml` живе в корені репозиторію; `.env` не комітиться, `.env.example` комітиться.
- Теми Kafka називаються `corridor.<агрегат>`, наприклад `corridor.orders`.
- Локальний конфіг — `application-local.yaml`; базовий `application.yaml` не містить ні `localhost`, ні секретів.
- Профіль `local` активують лише для ручного запуску; тести його не використовують.
- Тести з базою — через Testcontainers; образ має збігатися з `compose.yaml`.
- Секрети передаються через `.env` або змінні середовища; у YAML — тільки посилання на них.
