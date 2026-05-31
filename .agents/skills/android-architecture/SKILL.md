---
name: android-architecture
description: Clean Architecture for Android apps — multi-module structure, convention plugins, dependency injection, and domain-driven design patterns following Google's official guidance.
---

# Android Architecture

## When to Use This Skill
- Designing the module structure of a new Android project
- Deciding between MVVM, MVI, or other presentation patterns
- Setting up multi-module Clean Architecture with convention plugins
- Implementing offline-first architecture with Room + DataStore
- Designing repository patterns and data layer abstractions

## Reference Architecture (from nowinandroid)

```
app/                          → Application entry point, DI, navigation
├── core/
│   ├── data/                 → Repository implementations, Room, Ktor
│   ├── domain/               → Use cases, domain models (pure Kotlin)
│   ├── ui/                   → Design system, reusable components
│   └── testing/              → Shared test utilities + fakes
├── feature/
│   ├── auth/                 → Feature: authentication
│   ├── home/                 → Feature: home screen
│   └── settings/             → Feature: settings
├── build-logic/convention/   → Convention plugins (shared Gradle config)
└── gradle/libs.versions.toml → Version catalog
```

## Module Dependency Rules

```
feature/* → core:domain → core:data (via interfaces)
core:domain → PURE KOTLIN (no Android dependencies)
core:data  → Room, Retrofit/Ktor, DataStore
app        → all features (navigation host)
```

## Architectural Principles

### 1. Single Source of Truth (SSOT)
- Room database is the single source of truth
- Network data always flows through Room (cache-then-network)
- UI observes local `Flow` only, never network directly

### 2. Unidirectional Data Flow (UDF)
```
UI (Composable) → Event → ViewModel → UseCase → Repository → Room
                                                                    ↓
UI ← StateFlow ← ViewModel ← Flow ← Repository ← Room ←──────────┘
```

### 3. Presentation Layer (MVVM / Light MVI)
```kotlin
// ViewState sealed hierarchy
sealed interface HomeUiState {
  data object Loading : HomeUiState
  data class Success(val items: List<Item>) : HomeUiState
  data class Error(val message: String) : HomeUiState
}

// ViewModel
class HomeViewModel @Inject constructor(
  private val getItemsUseCase: GetItemsUseCase
) : ViewModel() {
  private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      getItemsUseCase().collect { items ->
        _uiState.value = HomeUiState.Success(items)
      }
    }
  }
}
```

## Convention Plugins (build-logic)

| Plugin ID | Purpose |
|-----------|---------|
| `android.application` | App module setup |
| `android.library` | Shared library modules |
| `android.feature` | Feature module (Compose + Hilt) |
| `android.hilt` | Dependency injection |
| `android.room` | Room database |
| `android.test.unit` | Unit test deps |
| `android.test.instrumentation` | Instrumentation test deps |
| `android.lint` | Lint configuration |

## Dependency Injection (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
  @Provides @Singleton
  fun provideItemRepository(
    localDataSource: RoomDataSource,
    remoteDataSource: ApiDataSource
  ): ItemRepository = ItemRepositoryImpl(localDataSource, remoteDataSource)
}
```

## Offline-First Data Flow
```kotlin
class ItemRepositoryImpl(
  private val local: ItemDao,
  private val remote: ItemApi
) : ItemRepository {
  override fun observeItems(): Flow<List<Item>> =
    local.observeAll()         // 1. Read from Room first
      .map { it.map(::toDomain) }

  override suspend fun refresh() {
    val items = remote.fetch()   // 2. Fetch from network
    local.upsert(items)          // 3. Write to Room (triggers Flow)
  }
}
```
