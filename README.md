# Приложение "Based"

Простой блок‑нот на Android с использованием Room, ViewModel и Jetpack Compose.

## Описание

Приложение позволяет:

* Добавлять текстовые заметки.
* Просматривать список заметок.
* Удалять ненужные заметки.
* Сохранять заметки локально в базе данных между запусками.

## Структура проекта

```
com.example.based
├── data
│   ├── Note.kt         # Entity: модель заметки
│   ├── NoteDao.kt      # DAO: методы работы с базой
│   └── NoteDatabase.kt # RoomDatabase: инициализация базы
├── NoteViewModel.kt    # AndroidViewModel: логика добавления/удаления и источник данных
├── MainActivity.kt     # UI: Compose-экран с полем ввода, кнопкой и списком
└── ui/theme            # Тема и стили Material3 для Compose
```

## Как это работает

1. **Room (база данных)**

   * Файл `notes.db` хранится в `/data/data/com.example.based/databases/notes.db`.
   * Класс `NoteDatabase` создаёт или открывает базу данных, используя аннотацию `@Database`.
   * Интерфейс `NoteDao` содержит методы `getAll()` (получение всех заметок), `insert()` (добавление новой) и `delete()` (удаление заметки).
   * Все операции с базой происходят через этот DAO.

2. **ViewModel (NoteViewModel)**

   * `NoteViewModel` наследуется от `AndroidViewModel`, чтобы получить доступ к `applicationContext`.
   * Получает ссылку на DAO из `NoteDatabase.getDatabase()`.
   * Использует Kotlin Flow, преобразуемый в `LiveData`, чтобы UI мог автоматически получать изменения.
   * Методы `add(text: String)` и `delete(note: Note)` используют `viewModelScope.launch` для запуска операций с базой в фоне.

3. **UI (Jetpack Compose)**

   * В `MainActivity` через `setContent` задаётся интерфейс с помощью Jetpack Compose.
   * ViewModel подключается через `viewModel()` и подписывается на данные: `observeAsState()` следит за списком заметок.
   * Компоненты `TextField`, `Button`, `LazyColumn`, `Row`, `Text`, `IconButton` формируют интерфейс:

     * Ввод текста новой заметки
     * Кнопка "Добавить" — вызывает `vm.add(input)`
     * Список заметок, каждая с кнопкой удаления — `vm.delete(note)`
   * Обновление UI происходит автоматически при изменении данных.

## Запуск проекта

1. Клонировать репозиторий:

   ```bash 
    git clone [https://github.com/hetasshi/based.git](https://github.com/hetasshi/based.git)
   ```
2. Открыть проект в Android Studio.
3. При необходимости указать путь к Android SDK (в `local.properties`).
4. Нажать **Run ▶** для сборки и запуска.

## Проверка работоспособности

- После запуска введите текст и нажмите «Добавить» — заметка появится в списке.
- Закройте приложение и откройте снова — заметки останутся сохранёнными.
- Нажатие на иконку корзины удаляет нужную заметку.

## Примечания

- В `NoteDatabase` используется `allowMainThreadQueries()` — это допустимо только в моем проекте. В продакшн-приложениях всё должно происходить в фоновом потоке.
- Все зависимости подключены через Gradle Wrapper (`gradlew`), поэтому сборка проекта возможна на любой машине.
- `.gitignore` исключает временные файлы, кэш Android Studio и др.

---

Артём

