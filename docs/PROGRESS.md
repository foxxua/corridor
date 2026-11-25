# Прогрес

Оновлено: 25.11.2026

Цей файл — точка входу для того, хто повертається до проєкту після перерви: що вже стоїть, чому саме так і з чого продовжувати. Живий стан, а не історія: застаріле виправляється, а не дописується внизу.

## Де ми зараз

Сервіс `core` працює на Spring Boot 4.2.0. Є перший вертикальний зріз `orders`: Flyway-міграція створює таблицю, домен і `JdbcClient`-репозиторій читають і пишуть замовлення, `POST /api/v1/orders` і `GET /api/v1/orders/{id}` працюють, неіснуючий id дає `404`.

Тести проходять: `PostgresConnectionTest`, `OrderRepositoryTest` (зріз `@JdbcTest`) і `OrderApiTest` (повний контекст + `@AutoConfigureMockMvc`). Разом 4 тести, 0 падінь. `.\gradlew.bat build` зелений.

`libs/contract` поки що порожній: `OrderId` переїхав у домен `core` і тепер обгортає `UUID`. Wire-типи повернуться в контракт разом із першою подією.

Redis і Kafka описані в `compose.yaml` і перевірені окремо, але `core` до них ще не підключений. Outbox і Dockerfile попереду.

## Що вже зроблено

| Що | Стан | Як перевірено |
|---|---|---|
| Корінь-агрегатор Gradle і каталог версій | готово | `.\gradlew.bat build` → BUILD SUCCESSFUL |
| `.gitignore` із політикою для `.idea` | готово | `git status` |
| Коміти основи | `a84e198`, `be23776`, `d48dc33` | `git log --oneline` |
| `compose.yaml`: Postgres, Redis, Kafka, kafka-ui | готово | `docker compose ps`, `psql`, `PING`, тема Kafka |
| `.env.example` | готово | `.env` у `.gitignore`, секрет не в git |
| `services/core` на Spring Boot 4.2.0 | готово | `bootRun`, лог `Started CoreApplicationKt` |
| `/actuator/health` і `/actuator/info` на `8081` | готово | `db: UP`, info віддає дані Postgres |
| Профіль `local` і секрети з `.env` | готово | `bootRun` з кореня, `db: UP` |
| Flyway і міграція `V1__create_orders.sql` | готово | `flyway_schema_history`, таблиця `orders` |
| Домен `Order`, `OrderStatus`, `OrderId(UUID)` | готово | компіляція й тести |
| `OrderRepository` на `JdbcClient` | готово | `OrderRepositoryTest` |
| `POST /api/v1/orders` і `GET /api/v1/orders/{id}` | готово | `OrderApiTest` (MockMvc) |
| `404` через `ProblemDetail` | готово | `OrderApiTest` |
| Testcontainers: API і репозиторій | готово | 4 тести, 0 падінь |
| `libs/contract` | порожній, чекає `OrderCreated` | — |
| Redis і Kafka в `core` | ні, наступні кроки | — |
| Outbox | ні, після `OrderCreated` | — |
| Dockerfile для `core` | ні, після подій | — |

Поточний зріз `orders` ще не закомічений. `git status` показує зміни в `gradle/libs.versions.toml` і `services/core/build.gradle.kts`, видалений `libs/contract/.../OrderId.kt` і нові теки `core/config`, `core/order`, `resources/db`, `test/.../order`. Наступний крок — checkpoint-коміт цього зрізу.

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
| JDBC + `JdbcClient`, без JPA | для невеликого домену SQL видно явно; JPA додає Hibernate і `ddl-auto` | JPA виправдана, коли з'явиться багато зв'язків і запитів |
| Схему володіє Flyway, `ddl-auto` не використовуємо | міграції версійовані й повторювані локально та в тестах | `ddl-auto=update` робить схему непередбачуваною |
| Застосовані міграції не редагують | Flyway зберігає checksum у `flyway_schema_history` | зміна `V1` після застосування ламає старт; зміни — новим `V2` |
| `flyway-database-postgresql` додається окремо | з Flyway 10+ підтримка баз винесена в модулі | без нього — `Unsupported Database: PostgreSQL` |
| `OrderId` — доменний `value class` над `UUID` у `core.order`, а не wire-тип | типобезпека в коді, прямий мапінг на `uuid` у БД | рядковий `OrderId` вимагає конверсій і допускає сміття |
| На дріт віддаємо `UUID`, а не `OrderId` | Jackson/Kafka серіалізують `UUID` як рядок без спеціальних анотацій | value class у JSON може дати зайвий об'єкт або вимагати `@JsonValue` |
| Статус — `varchar(32)` + check constraint | явно документує допустимі стани й ловить помилки в БД | новий статус вимагає міграції; це прийнятна ціна |
| `POST /api/v1/orders` поки без тіла | перший зріз перевіряє ланцюжок, а не модель замовлення | поля з'являться разом із `CreateOrderRequest` і валідацією |
| `Clock` як бін | тести можуть фіксувати час, а не залежати від `Instant.now()` | без нього тест створення залежить від реального часу |
| Jackson 3-модуль `tools.jackson.module:jackson-module-kotlin` + `kotlin-reflect` | Boot 4 використовує Jackson 3, не Jackson 2 | Jackson 2-модуль не буде підхоплений |
| API-тест — повний `@SpringBootTest` + `@AutoConfigureMockMvc` | перевіряє контролер, сервіс, репозиторій, Jackson і Flyway разом | зріз не побачить повного ланцюжка |
| Репозиторний тест — `@JdbcTest` з `@AutoConfigureTestDatabase(NONE)` | швидкий зріз без вебшару й актуатора | без `Replace.NONE` Boot підмінить `DataSource` вбудованою базою |
| У `@JdbcTest` Flyway імпортується явно | зріз JDBC не включає Flyway-автоконфігурацію | без `FlywayAutoConfiguration` таблиці `orders` не буде |
| Testcontainers 2.0.5, `@TestConfiguration` + `@ServiceConnection` | контейнер описаний в одному місці, життєвий цикл керує Spring Boot | `@Container` у кожному тесті дублює конфігурацію; разом із біном буде два контейнери |
| Образ у тестах — `postgres:18.6`, як у compose | локальна й тестова бази не розходяться за версією | `latest` робить тести непередбачуваними |
| Тести не використовують профіль `local` і `.env` | Testcontainers сам дає URL, користувача й пароль; тест повторюваний у CI | тести на `localhost:5432` залежать від стану машини й можуть псувати дані |
| `show-details: always` для health | локальна діагностика: видно компоненти й причину `DOWN` | у продакшені деталі health розкривають інфраструктуру; там їх ховають |
| `core` спершу через `bootRun` | швидкий цикл розробки; контейнеризація після подій | контейнер одразу вимагає Dockerfile і перезбірки на кожну зміну |

## Версії

Зафіксовані зараз: Kotlin `2.4.21`, Gradle `9.6.0` (wrapper), JDK `21`.

Spring Boot `4.2.0` керує: Spring Framework `7.1.0`, Tomcat `11.0.27`, HikariCP `7.1.0`, драйвер PostgreSQL `42.7.14`, Testcontainers `2.0.5`, JUnit Jupiter `6.1.3`, Flyway `12.11.0`, Jackson `3.1.8`.

Інфраструктура: `postgres:18.6`, `redis:8.12.0`, `apache/kafka:4.4.0`, `kafbat/kafka-ui:v1.5.1`.

Заплановані, але ще не підключені: Spring Cloud `2026.0.0` (Gateway `5.1.0`), springdoc `3.2.1`, Kafka-клієнт, Redis-клієнт. JPA — лише якщо буде вирішено відмовитися від явного SQL.

Важливо для Boot 4: Jackson 3 має координату Kotlin-модуля `tools.jackson.module:jackson-module-kotlin`, а не `com.fasterxml.jackson.module`. Модуль уже підключений і використовується для REST.

## Питання, на якому зупинились

1. Наступний крок: `OrderCreated` + outbox + Kafka чи спершу Redis та ідемпотентність? Логічний порядок — подія, outbox, Kafka, потім Redis.
2. Коли додавати поля замовлення (отримувач, адреса) і `CreateOrderRequest`: до Kafka чи після?
3. Формат `OrderId` на дроті: `UUID` у JSON чи явний `String` у DTO. Зараз використовується `UUID`.
4. Чи потрібен `OrderStatus` у contract зараз, чи достатньо в домені до появи подій зі статусами.

## Наступні кроки, по порядку

1. Закомітити зріз `orders` (Flyway, домен, репозиторій, API, тести).
2. Додати `OrderCreated` у `libs/contract`: `orderId: UUID`, `occurredAt: Instant` або ISO-рядок.
3. Створити outbox-таблицю і писати подію в тій самій транзакції, що й замовлення.
4. Підключити Kafka: продюсер читає outbox, публікує в `corridor.orders`; додати consumer.
5. Redis — для ідемпотентності `POST` або кешу.
6. Додати поля замовлення й `CreateOrderRequest` з валідацією.
7. Dockerfile для `core` і перехід на імена сервісів `postgres`, `redis`, `kafka`.
8. Далі за планом: geo-route, delivery-plugin, адмінка.

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

Створення й читання замовлення:

```powershell
curl.exe -i -X POST http://localhost:8081/api/v1/orders
curl.exe http://localhost:8081/api/v1/orders/<uuid>
```

Перевірка схеми:

```powershell
docker compose exec postgres psql -U corridor -d corridor -c "select id, status, created_at from orders;"
docker compose exec postgres psql -U corridor -d corridor -c "select version, description, success from flyway_schema_history;"
```

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
- `libs/contract` містить лише те, що літає по дроту. Entity, репозиторії й сервіси — ні. Зараз модуль порожній до появи `OrderCreated`.
- Команди Gradle на Windows — через `.\gradlew.bat`.
- `compose.yaml` живе в корені репозиторію; `.env` не комітиться, `.env.example` комітиться.
- Теми Kafka називаються `corridor.<агрегат>`, наприклад `corridor.orders`.
- Локальний конфіг — `application-local.yaml`; базовий `application.yaml` не містить ні `localhost`, ні секретів.
- Профіль `local` активують лише для ручного запуску; тести його не використовують.
- Тести з базою — через Testcontainers; образ має збігатися з `compose.yaml`.
- Секрети передаються через `.env` або змінні середовища; у YAML — тільки посилання на них.
- Схема БД — тільки через Flyway; застосовані міграції не редагують.
- `OrderId` живе в домені й обгортає `UUID`; на дріт іде `UUID`.
- JDBC-параметри: enum передаємо як `.name`, `timestamptz` — як `OffsetDateTime`.
- API-тести — повний `@SpringBootTest` + `@AutoConfigureMockMvc`; репозиторні — `@JdbcTest` з явним `FlywayAutoConfiguration`.
