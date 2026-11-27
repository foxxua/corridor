# Прогрес

Оновлено: 27.11.2026

Цей файл — точка входу для того, хто повертається до проєкту після перерви: що вже стоїть, чому саме так і з чого продовжувати. Живий стан, а не історія: застаріле виправляється, а не дописується внизу.

## Де ми зараз

Сервіс `core` працює на Spring Boot 4.2.0. Є вертикальний зріз `orders`: Flyway-міграції `V1` і `V2`, домен і `JdbcClient`-репозиторій читають і пишуть замовлення, `POST /api/v1/orders` і `GET /api/v1/orders/{id}` працюють, неіснуючий id дає `404`.

Тести проходять: `PostgresConnectionTest`, `OrderRepositoryTest` (зріз `@JdbcTest`), `OrderApiTest` (повний контекст, `@AutoConfigureMockMvc` і перевірка payload в outbox), `OrderMappingsTest` (чистий JUnit), `OutboxRepositoryTest`, `OrderCreationTransactionTest`, `OutboxPublisherTest` і `OutboxPublisherFailureTest`. Разом 9 тестів, 0 падінь. `.\gradlew.bat build` зелений.

`libs/contract` містить `OrderCreated(orderId: UUID, occurredAt: Instant)` і не має жодної залежності. `core` залежить від контракту; домен у контракт не протікає, `OrderId` лишається в `core`, на дріт іде `UUID`.

Подія пишеться в outbox у тій самій транзакції, що й замовлення (`V2`), а реле `OutboxPublisher` публікує її в `corridor.orders`, беручи рядки `FOR UPDATE SKIP LOCKED`, і позначає `published_at`. Фоновий цикл вмикає лише профіль `local`; тести викликають `publishBatch()` напряму. Consumer і Redis ще не підключені, Dockerfile — після consumer'а.

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
| Testcontainers: Postgres і Kafka | готово | 9 тестів, 0 падінь |
| `OrderCreated` у `libs/contract` | готово | `:libs:contract:build`, нуль залежностей |
| `core → contract` і мапінг `Order.toOrderCreated()` | готово | `OrderMappingsTest` |
| `Clock` з точністю до мікросекунд | готово | перевірено на JDK 21: `nanos` кратні 1000; round-trip у `OrderRepositoryTest` |
| Outbox-таблиця `V2` і запис події в транзакції | готово | `OutboxRepositoryTest`, `OrderCreationTransactionTest` |
| Серіалізація `OrderCreated` у payload | готово | `OrderApiTest`: `payload->>'occurredAt'` парситься як `Instant` |
| Реле `OutboxPublisher` + `OutboxRelay` | готово | `OutboxPublisherTest`, `OutboxPublisherFailureTest` |
| Kafka-конфіг, `StringSerializer`, `acks: all` | готово | `OutboxPublisherTest` |
| Consumer і дедуплікація | ні, наступний крок | — |
| Redis в `core` | ні, після consumer'а | — |
| Dockerfile для `core` | ні, після consumer'а | — |

Закомічено: зріз `orders` (`a651f78`) і `OrderCreated` з мапінгом (`90a6881`). Незакомічені зараз: `V2__create_outbox.sql`, пакет `core.outbox`, `SchedulingConfiguration`, Kafka-залежності й конфіг, тестові `KafkaTestcontainersConfiguration`, `KafkaTestTopics`, `KafkaTestMessages` та чотири нові тестові класи. Наступний крок — коміт цього зрізу, потім consumer.

### Стан інфраструктури

- **Postgres 18.6**, том `corridor-pg` змонтований у `/var/lib/postgresql` (не в `/data`), порт `5432`, healthcheck через `pg_isready -U corridor -d corridor`.
- **Redis 8.12.0**, `--appendonly yes`, том `corridor-redis` у `/data`, порт `127.0.0.1:6379`.
- **Kafka 4.4.0** в KRaft, вузол 1, ролі `broker,controller`. Слухачі: `INTERNAL:29092`, `EXTERNAL:9092`, `CONTROLLER:9093`. Рекламуються `kafka:29092` для compose і `localhost:9092` для хоста. Том `corridor-kafka` у `/var/lib/kafka/data`, `KAFKA_LOG_DIRS` вказує туди ж. Порт `127.0.0.1:9092`.
- **kafka-ui v1.5.1**, порт `8090`, bootstrap `kafka:29092`, стартує після `condition: service_healthy`.
- Тема `corridor.orders` створена вручну, розширена до 3 партицій. `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`, тому нові теми створюють явно.
- Тести піднімають власний `apache/kafka:4.4.0` і створюють тему самі через `KafkaTestTopics` (`AdminClient`), не покладаючись на auto-create.
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
| `Clock` обрізає час до мікросекунд (`Clock.tick(..., 1 µs)`) | `timestamptz` у Postgres мікросекундний; інакше `createdAt` у памʼяті й у БД різняться на нано | порівняння після round-trip падає на рівному місці |
| `occurredAt` — `Instant`, на дроті ISO-8601 | UTC без зсуву й двозначності; Jackson 3 віддає рядок, а не epoch-число | `OffsetDateTime` тягне зсув у кожне повідомлення, `String` — ручний парсинг у кожного споживача |
| Подія в минулому часі (`OrderCreated`), без `status` | подія — факт, що вже стався; статус дублювався б у кожному повідомленні | `CreateOrder`-команда має інші гарантії й поки не потрібна |
| `libs/contract` без Spring, Jackson і домену | нуль залежностей — будь-яке протікання видно в `build.gradle.kts` одразу | інакше geo-route тягне домен замовлень або свою версію Jackson |
| Мапінг домен → контракт — `internal` extension у `core.order` | залежність односпрямована `core → contract`; домен і контракт не знають один про одного | метод у `Order` або фабрика в контракті розвертають залежність і тягнуть зайве в тести |
| Jackson 3-модуль `tools.jackson.module:jackson-module-kotlin` + `kotlin-reflect` | Boot 4 використовує Jackson 3, не Jackson 2 | Jackson 2-модуль не буде підхоплений |
| API-тест — повний `@SpringBootTest` + `@AutoConfigureMockMvc` | перевіряє контролер, сервіс, репозиторій, Jackson і Flyway разом | зріз не побачить повного ланцюжка |
| Репозиторний тест — `@JdbcTest` з `@AutoConfigureTestDatabase(NONE)` | швидкий зріз без вебшару й актуатора | без `Replace.NONE` Boot підмінить `DataSource` вбудованою базою |
| У `@JdbcTest` Flyway імпортується явно | зріз JDBC не включає Flyway-автоконфігурацію | без `FlywayAutoConfiguration` таблиці `orders` не буде |
| Testcontainers 2.0.5, `@TestConfiguration` + `@ServiceConnection` | контейнер описаний в одному місці, життєвий цикл керує Spring Boot | `@Container` у кожному тесті дублює конфігурацію; разом із біном буде два контейнери |
| Образи в тестах — `postgres:18.6` і `apache/kafka:4.4.0`, як у compose | локальна й тестова інфраструктура не розходяться за версією | `latest` робить тести непередбачуваними |
| Тести не використовують профіль `local` і `.env` | Testcontainers сам дає URL, користувача й пароль; тест повторюваний у CI | тести на `localhost:5432` залежать від стану машини й можуть псувати дані |
| `show-details: always` для health | локальна діагностика: видно компоненти й причину `DOWN` | у продакшені деталі health розкривають інфраструктуру; там їх ховають |
| `core` спершу через `bootRun` | швидкий цикл розробки; контейнеризація після подій | контейнер одразу вимагає Dockerfile і перезбірки на кожну зміну |
| Outbox замість прямого `send` з сервісу | подія і замовлення комітяться разом; публікація стає повторюваною дією, без dual write | прямий `send` у треді запиту губить події при падінні брокера або відкочує замовлення при падінні `send` |
| At-least-once: `send` → `published_at = now()` → commit | дублікат дешевший за втрату; дедуплікація буде на consumer'і | зворотний порядок дає at-most-once, тобто тихі втрати |
| `FOR UPDATE SKIP LOCKED`, `order by id`, пачка 50 | паралельні інстанси не блокують один одного; черга не вичитується в памʼять; `id bigint identity` дає стабільний порядок і локальний індекс | звичайний `FOR UPDATE` серіалізує реле, запит без `limit` тягне всю таблицю, `uuid` втрачає порядок вставки |
| `payload jsonb` і окремий `partition_key` | JSON валідований БД; ключ = `orderId`, тож події одного замовлення в одній партиції | ключ із `payload->>'orderId'` робить реле залежним від форми JSON |
| `StringSerializer`, payload — готовий JSON-рядок | outbox уже зберігає рядок; немає подвійної серіалізації й type headers | `JsonSerializer` загорнув би рядок у рядок або привʼязав споживача до класу |
| Реле під `@ConditionalOnProperty`, у тестах вимкнене | фоновий цикл у тестах — шум і флейки; `publishBatch()` тестується напряму | увімкнений планувальник у тестах стукає в `localhost:9092` кожні 500 мс |
| `@ServiceConnection` дає бін `KafkaConnectionDetails`, не property | тому bootstrap у тестах беруть з `KafkaContainer.bootstrapServers` | `@Value("\${spring.kafka.bootstrap-servers}")` у тесті не резолвиться |
| Тестові топіки створюють явно через `AdminClient` | у compose `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`, тести не мають жити на іншій конфігурації | auto-create дає 1 партицію замість 3 і хвилинні ретраї при помилці |

## Версії

Зафіксовані зараз: Kotlin `2.4.21`, Gradle `9.6.0` (wrapper), JDK `21`.

Spring Boot `4.2.0` керує: Spring Framework `7.1.0`, Tomcat `11.0.27`, HikariCP `7.1.0`, драйвер PostgreSQL `42.7.14`, Testcontainers `2.0.5`, JUnit Jupiter `6.1.3`, Flyway `12.11.0`, Jackson `3.1.8`, Spring Kafka `4.2.0`, kafka-clients `4.4.0`.

Інфраструктура: `postgres:18.6`, `redis:8.12.0`, `apache/kafka:4.4.0`, `kafbat/kafka-ui:v1.5.1`.

Заплановані, але ще не підключені: Spring Cloud `2026.0.0` (Gateway `5.1.0`), springdoc `3.2.1`, Redis-клієнт. JPA — лише якщо буде вирішено відмовитися від явного SQL.

Важливо для Boot 4: Jackson 3 має координату Kotlin-модуля `tools.jackson.module:jackson-module-kotlin`, а не `com.fasterxml.jackson.module`. Модуль уже підключений і використовується для REST.

## Питання, на якому зупинились

Вирішено: порядок — подія, outbox, Kafka, Redis; `occurredAt` — `Instant`; на дроті `UUID`, не `OrderId`; статусу в contract поки немає; продюсер пише через outbox, реле публікує at-least-once.

Відкрито:

1. `eventId` у контракті — перше питання кроку consumer'а: окреме поле події чи `id` рядка outbox. Без нього дедуплікація неможлива.
2. Дедуплікація: таблиця оброблених `eventId` у БД чи Redis (`SET NX EX`), і на який термін тримати ключ.
3. `eventType` або envelope: потрібні, коли в `corridor.orders` стане більше одного типу подій.
4. Retention outbox і отруйні рядки: коли чистити опубліковані, чи додавати `attempts` і DLQ.
5. Коли додавати поля замовлення (отримувач, адреса) і `CreateOrderRequest`: до consumer'а чи після?

## Наступні кроки, по порядку

1. Закомітити outbox, реле й Kafka-конфіг.
2. Consumer: `eventId` у контракті, `@KafkaListener`, дедуплікація по `eventId`.
3. Outbox retention і отруйні рядки: `attempts`, DLQ, чистка опублікованих.
4. Redis — для ідемпотентності `POST` або кешу.
5. Додати поля замовлення й `CreateOrderRequest` з валідацією.
6. Dockerfile для `core` і перехід на імена сервісів `postgres`, `redis`, `kafka`.
7. Далі за планом: geo-route, delivery-plugin, адмінка.

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

Перевірка outbox і реле (реле працює лише з профілем `local`):

```powershell
curl.exe -i -X POST http://localhost:8081/api/v1/orders
docker compose exec postgres psql -U corridor -d corridor -c "select id, topic, partition_key, payload, published_at from outbox;"
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic corridor.orders --from-beginning --max-messages 1
```

Очікування: рядок в outbox із заповненим `published_at` і те саме повідомлення в топіку. Якщо `published_at` порожній — реле вимкнене (`corridor.outbox.scheduler-enabled`) або Kafka недоступна; у лозі буде `outbox publish failed`.

Перевірка схеми:

```powershell
docker compose exec postgres psql -U corridor -d corridor -c "select id, status, created_at from orders;"
docker compose exec postgres psql -U corridor -d corridor -c "select version, description, success from flyway_schema_history;"
```

Тести:

```powershell
.\gradlew.bat :services:core:test
```

Тестам потрібен запущений Docker: Testcontainers сам піднімає `postgres:18.6` і `apache/kafka:4.4.0`, compose для тестів не потрібен.

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
- `libs/contract` містить лише те, що літає по дроту. Entity, репозиторії й сервіси — ні. Зараз там `OrderCreated`, і модуль не має жодної залежності.
- Час у контракті — `Instant` (UTC), на дроті ISO-8601; `Clock` у `core` обрізає до мікросекунд.
- Мапінг домен → контракт живе в `core`, не в контракті й не в домені.
- Команди Gradle на Windows — через `.\gradlew.bat`.
- `compose.yaml` живе в корені репозиторію; `.env` не комітиться, `.env.example` комітиться.
- Теми Kafka називаються `corridor.<агрегат>`, наприклад `corridor.orders`.
- Назовні — тільки через outbox: сервіс пише подію в транзакції з бізнес-даними, реле публікує. Прямий `KafkaTemplate.send` із сервісу не використовуємо.
- Реле вимкнене в тестах: `corridor.outbox.scheduler-enabled=false` у базовому YAML, `true` в local; тести кличуть `publishBatch()` напряму.
- Реле читає `FOR UPDATE SKIP LOCKED` малими пачками й ставить `published_at`; рядки не видаляє.
- Тестові топіки створюються явно (`KafkaTestTopics`), bootstrap беруть з `KafkaContainer`, а не з property.
- Postgres і Kafka для тестів — окремі `@TestConfiguration`, щоб `@JdbcTest` не піднімав брокера.
- Локальний конфіг — `application-local.yaml`; базовий `application.yaml` не містить ні `localhost`, ні секретів.
- Профіль `local` активують лише для ручного запуску; тести його не використовують.
- Тести з базою — через Testcontainers; образ має збігатися з `compose.yaml`.
- Секрети передаються через `.env` або змінні середовища; у YAML — тільки посилання на них.
- Схема БД — тільки через Flyway; застосовані міграції не редагують.
- `OrderId` живе в домені й обгортає `UUID`; на дріт іде `UUID`.
- JDBC-параметри: enum передаємо як `.name`, `timestamptz` — як `OffsetDateTime`.
- API-тести — повний `@SpringBootTest` + `@AutoConfigureMockMvc`; репозиторні — `@JdbcTest` з явним `FlywayAutoConfiguration`.
