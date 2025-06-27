
## Как это работает подробнее

### 1. Room (слой хранения данных)

1. **Entity (`Note.kt`)**

   ```kotlin
   @Entity(tableName = "notes")
   data class Note(
     @PrimaryKey(autoGenerate = true) val id: Int = 0,
     val text: String,
     val created: Long = System.currentTimeMillis()
   )
   ```

   * **Что происходит при компиляции:** Room-processor генерирует SQL-код для создания таблицы `notes` со столбцами `id`, `text` и `created`.
   * **Для чего:** `Note` представляет одну запись в базе; `id` нужен для уникальности, `created` — для сортировки.

2. **DAO (`NoteDao.kt`)**

   ```kotlin
   @Dao
   interface NoteDao {
     @Query("SELECT * FROM notes ORDER BY created DESC")
     fun getAll(): Flow<List<Note>>

     @Insert
     suspend fun insert(note: Note)

     @Delete
     suspend fun delete(note: Note)
   }
   ```

   * **`getAll()`** возвращает **Flow\<List<Note>>** — асинхронный поток, который при каждом изменении таблицы автоматически выдаёт обновлённый список.
   * **`insert()`** и **`delete()`** — сгенерированные Room-методы, аннотированные как `suspend`, чтобы выполняться в корутине без блокировки UI.
   * **Для чего:** DAO служит «мостиком» между Kotlin-кодом и SQL-запросами, обрабатывая преобразование объектов `Note` в строки таблицы и обратно.

3. **Database (`NoteDatabase.kt`)**

   ```kotlin
   @Database(entities = [Note::class], version = 1, exportSchema = false)
   abstract class NoteDatabase : RoomDatabase() {
     abstract fun noteDao(): NoteDao

     companion object {
       @Volatile private var INSTANCE: NoteDatabase? = null

       fun getDatabase(context: Context): NoteDatabase =
         INSTANCE ?: synchronized(this) {
           Room.databaseBuilder(
             context.applicationContext,
             NoteDatabase::class.java,
             "notes.db"
           )
           .allowMainThreadQueries()
           .build().also { INSTANCE = it }
         }
     }
   }
   ```

   * **Синглтон-паттерн:** `@Volatile` и `synchronized` гарантируют, что база создаётся только один раз из разных потоков.
   * **`context.applicationContext`:** используем глобальный контекст приложения, чтобы база жила столько же, сколько приложение, и не утекал контекст Activity.
   * **`allowMainThreadQueries()`:** разрешает операции в главном потоке (только для учебного примера).
   * **Расположение файла:** физически база находится в `/data/data/com.example.based/databases/notes.db`.

### 2. ViewModel (слой логики и связи с UI)

```kotlin
class NoteViewModel(application: Application) : AndroidViewModel(application) {
  // Получаем applicationContext
  private val context: Context = getApplication<Application>().applicationContext

  // Ссылка на DAO
  private val dao: NoteDao = NoteDatabase.getDatabase(context).noteDao()

  // Преобразуем Flow в LiveData для подписки из Compose
  val notes: LiveData<List<Note>> = dao.getAll().asLiveData()

  // Добавляет новую заметку
  fun add(text: String) = viewModelScope.launch {
    if (text.isNotBlank()) {
      val newNote = Note(text = text.trim())
      dao.insert(newNote)
    }
  }

  // Удаляет заметку
  fun delete(note: Note) = viewModelScope.launch {
    dao.delete(note)
  }
}
```

* **`AndroidViewModel(application)`** даёт доступ к `application` через `getApplication()`.
* **`viewModelScope`** — корутинный скоуп, отменяется при уничтожении ViewModel, предотвращая утечки.
* **`dao.getAll().asLiveData()`** автоматически собирает Flow в бекграунде и постит изменения во `LiveData`.
* **Методы `add`/`delete`** запускают соответствующие операции в корутине, не блокируя UI.

### 3. UI (Jetpack Compose, слой отображения)

```kotlin
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      BasedTheme {
        // Получаем ViewModel из ViewModelStoreOwner
        val vm: NoteViewModel = viewModel()
        // Подписываемся на LiveData; при получении null или до первого значения — пустой список
        val notes: List<Note> by vm.notes.observeAsState(emptyList())

        // Локальный стейт для ввода текста
        var input by remember { mutableStateOf("") }

        Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
              .padding(16.dp)
          ) {
            TextField(
              value = input,
              onValueChange = { input = it },
              placeholder = { Text("Новая заметка") },
              modifier = Modifier.fillMaxWidth(),
              keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = {
                vm.add(input)
                input = ""
              })
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
              onClick = {
                vm.add(input)
                input = ""
              },
              enabled = input.isNotBlank(),
              modifier = Modifier.align(Alignment.End)
            ) {
              Text("Добавить")
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
              items(notes, key = { it.id }) { note ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(note.text, modifier = Modifier.weight(1f))
                  IconButton(onClick = { vm.delete(note) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

* **`viewModel()`**: Compose-хелпер, который использует `ViewModelProvider` под капотом, привязывая ViewModel к Activity.
* **`observeAsState()`**: создаёт Compose `State`, автоматически триггеря recomposition при изменении данных.
* **`remember`**: запоминает значение поля ввода между recomposition.
* **Recomposition**: когда `notes` меняются (после insert/delete), Compose автоматически перерисовывает только изменённые участки UI.

