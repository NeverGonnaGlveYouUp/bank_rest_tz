# Bank Cards  REST Тестовое задание

Сервис для управления банковскими картами на REST с поддержкой RSQL-фильтрации, JWT-авторизации и шифрования данных.
* По-хорошему для шифрования стоит использовать Transparent Data Encryption (TDE) в PostgreSQL Professional, но в проекте используется pgcrypto ТОЛЬКО на поле номера карты, пароль юзера зашифрован через BCryptPasswordEncoder.
* Если падают тесты в контроллере убедитесь что в компиляторе есть ключ

      -parameters

### 🚀 Запуск в Docker
 1. Создайте файл .env в корневой папке проекта и заполните переменные окружения:

        POSTGRES_USER=admin
        POSTGRES_PASSWORD=secret
        POSTGRES_DB=bankrest
        POSTGRES_HOST=postgres
        JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
        BD_SECRET=my-super-secret-encryption-key-32
        CARD_BIN=400000
 
 2. Соберите JAR-файл:

        mvn clean package -DskipTests

 3. Запустите контейнеры:

        docker-compose up -d

### 💻 Запуск в IDE (IntelliJ IDEA / Eclipse)
 1. В вашей IDE добавьте переменные окружения из application.yml.
 2. Запустите класс BankCardsApplication. Liquibase автоматически создаст структуру таблиц и роли при старте.


### 📖 API Документация (Swagger)
 - После запуска приложение доступно по адресу: http://localhost:8080
 - Swagger UI: http://localhost:8080/swagger-ui/index.html#

### 🔍 Примеры использования RSQL (Фильтрация c пагинацией)
 1. Эндпоинты вроде GET /api/admin/user/findAllByRsql поддерживают сложные запросы поиска сортировки и пагинации.
 2. Параметры GET RSQL ендпоинтов содержат:
    - search: RSQL запрос для фильтрации, примеры:
        
            username=='stringstring'             // поиск по юзернейму
            username=like='str'                  // поиск с like по юзернейму
            cards.status=='ACTIVE'               // поиск по вложным сущностям
            username==st*;roles.name==ROLE_ADMIN // Имя начинается на 'st' И имеет роль 'ROLE_ADMIN'

    - sort: принимает имя поля и направление (asc или desc). Можно указывать несколько полей через запятую:

            username,asc                 // По имени пользователя (А-Я)
            roles.name,asc,username,asc  // Сначала по роли, затем по имени
            id,asc                       // По ID (по умолчанию)

    - page и size: параметры пагинации 