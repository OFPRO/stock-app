# Architecture — StockPro Android

## Principes

- **Clean Architecture** multi-module (Now in Android style)
- **Unidirectional Data Flow (UDF)** : UI → Event → ViewModel → UseCase → Repository
- **Single Source of Truth** : Room database (offline-first, cache-then-network)
- **MVVM** avec `StateFlow` et `sealed interface UiState`

## Module Dependency Graph

```
app  →  feature/*  →  core:domain  →  core:data (via interfaces)
                       core:domain  →  PURE KOTLIN (zero Android deps)
                       core:data    →  Room, Retrofit, DataStore
                       core:ui      →  Compose design system
```

## Structure complète des modules

```
App2/
├── app/                        → Hilt, NavHost, entry point
├── core/
│   ├── data/
│   │   ├── remote/             → Retrofit services + DTOs
│   │   ├── local/              → Room DAOs + entities
│   │   └── repository/         → Repository implementations
│   ├── domain/
│   │   ├── model/              → Domain models (data classes)
│   │   └── repository/         → Repository interfaces
│   └── ui/
│       ├── theme/              → Material 3 theme, tokens
│       └── components/         → Design system composables
├── feature/
│   ├── dashboard/              → KPIs + charts
│   ├── products/               → CRUD + pricing tiers
│   ├── customers/              → CRUD
│   ├── suppliers/              → CRUD
│   ├── warehouses/             → CRUD
│   ├── locations/              → CRUD
│   ├── movements/              → Stock movement history + create
│   ├── orders/                 → Purchase orders
│   ├── invoices/               → Sales invoices
│   ├── notifications/          → Alert center
│   ├── reorder-rules/          → Reordering rules + replenishment
│   ├── pos/                    → Caisse, session, cart, payment
│   ├── scanner/                → Barcode scanner
│   ├── auth/                   → PIN login
│   └── settings/               → About, PIN change, debug
└── build-logic/convention/     → Convention plugins
```

## Convention Plugins

| Plugin | Applique |
|--------|----------|
| `android.library` | Android library module (compileSdk, minSdk, Java 21, kotlin) |
| `android.feature` | Feature module (Compose + Hilt + KSP + ktx + lifecycle) |
| `android.hilt` | Hilt + KSP + javax inject |
| `android.room` | Room + KSP |
| `android.test.unit` | JUnit5 + Turbine + MockK |
| `android.lint` | Detekt + Lint config |

## Data Flow (offline-first)

```
View (Composable)
  → Event (sealed class)
    → ViewModel (StateFlow)
      → UseCase (optional, pure logic)
        → Repository (interface)
          → RepositoryImpl
            ├── local: Room DAO (observeAll → Flow)
            └── remote: Retrofit Service (suspend)
```

1. UI observe `Room.observeAll()` → Flow → StateFlow
2. ViewModel lance `refresh()` à l'init
3. Repository appelle `remote.fetch()` → upsert dans Room
4. Le Flow Room se met à jour automatiquement

## ViewModel Pattern

```kotlin
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val data: DashboardData) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val kpiRepository: KpiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            kpiRepository.observeDashboard()
                .catch { _uiState.value = DashboardUiState.Error(it.message ?: "Erreur") }
                .collect { data -> _uiState.value = DashboardUiState.Success(data) }
        }
    }
}
```

## Retrofit API Service

```kotlin
interface StockProApi {
    // Products
    @GET("api/products") suspend fun getProducts(
        @Query("warehouse_id") warehouseId: Int? = null
    ): List<ProductDto>

    // Dashboard
    @GET("api/kpis/dashboard") suspend fun getDashboard(): DashboardKpiDto

    // POS
    @GET("api/products/for-sale") suspend fun getProductsForSale(
        @Query("customer_id") customerId: Int? = null
    ): List<ForSaleProductDto>

    // ... 90+ endpoints
}
```

## Mapping DTO → Domain → Entity

```
DTO (Retrofit) → Domain model (pure Kotlin) → Room Entity
```

Les DTOs reflètent la réponse JSON du backend Flask.
Les domain models sont des data classes sans framework.
Les Room entities sont les tables locales.

## Thème

- Brand: `#1E3A6F` (bleu)
- Accent: `#F5A623` (orange)
- Material 3 dynamic color scheme
- Support dark/light mode
- Typo système (Roboto par défaut sur Android)
