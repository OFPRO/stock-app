---
name: android-compose-ui
description: Jetpack Compose UI patterns — state management, Navigation 3, Material 3 theming, adaptive layouts, animations, and previews following Material Design 3 guidelines.
---

# Jetpack Compose UI

## When to Use This Skill
- Building new screens with Jetpack Compose and Material 3
- Implementing Navigation 3 with type-safe routes
- Designing adaptive layouts for phones, tablets, and foldables
- Managing UI state with StateFlow, State, and snapshot system
- Creating custom themes with Material 3 dynamic color

## Material 3 Theming

```kotlin
// Theme.kt
private val DarkColorScheme = darkColorScheme(
  primary = Purple80,
  secondary = PurpleGrey80,
  tertiary = Pink80
)
private val LightColorScheme = lightColorScheme(
  primary = Purple40,
  secondary = PurpleGrey40,
  tertiary = Pink40
)

@Composable
fun AppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context)
      else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

## Navigation 3 (Type-Safe)

```kotlin
// Define routes as serializable sealed interface
@Serializable
sealed interface Route {
  @Serializable data object Home : Route
  @Serializable data class Detail(val id: Int) : Route
  @Serializable data object Settings : Route
}

// NavHost composable
@Composable
fun AppNavHost(navController: NavHostController) {
  NavHost(navController, startDestination = Route.Home) {
    composable<Route.Home> { HomeScreen(onItemClick = { id ->
      navController.navigate(Route.Detail(id))
    })}
    composable<Route.Detail> { DetailScreen() }
    composable<Route.Settings> { SettingsScreen() }
  }
}
```

## State Management Patterns

### ViewModel + StateFlow
```kotlin
class ItemListViewModel @Inject constructor(
  private val useCase: GetItemsUseCase
) : ViewModel() {

  @Stable
  data class UiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
  )

  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      useCase().collect { result ->
        _uiState.update { it.copy(items = result, isLoading = false) }
      }
    }
  }
}

// Screen collects from ViewModel
@Composable
fun ItemListScreen(viewModel: ItemListViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  // Render
}
```

### Compose Previews
```kotlin
@ThemePreviews
@Composable
fun ItemListPreview() {
  AppTheme {
    ItemListScreen(
      uiState = ItemListViewModel.UiState(
        items = listOf(Item(1, "Preview Item", "Description"))
      )
    )
  }
}
```

## Adaptive Layouts
```kotlin
@Composable
fun ItemDetailPane(itemId: Int) {
  val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
  when {
    windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT -> {
      CompactLayout(itemId)   // Phone: single column
    }
    windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM -> {
      MediumLayout(itemId)    // Foldable: list-detail
    }
    else -> {
      ExpandedLayout(itemId)  // Tablet: master-detail
    }
  }
}
```

## Key Compose Conventions

- Use `@Stable` / `@Immutable` annotations on UI state classes
- Use `collectAsStateWithLifecycle()` in screens (never `collectAsState()` alone)
- Use `derivedStateOf` for computed state (not `remember { ... }` with keys)
- LazyColumn keys must be stable and unique (`item.key { it.id }`)
- Extract reusable composable components without ViewModel dependencies
- Use `snapshotFlow { state.value }` to observe state changes from coroutines
