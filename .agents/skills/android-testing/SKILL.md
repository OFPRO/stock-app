---
name: android-testing
description: Android testing strategy — unit tests, Compose UI tests, Flow/Turbine tests, MockK, Robolectric, and instrumented tests following a layered testing approach.
---

# Android Testing

## When to Use This Skill
- Writing unit tests for ViewModels, UseCases, and Repositories
- Writing Compose UI tests for screens and components
- Testing Flows, StateFlows, and coroutines with Turbine
- Setting up test infrastructure (TestDispatcher, fakes, fixtures)
- Running instrumented tests and snapshot tests

## Testing Pyramid for Android

```
     ╱  E2E / Espresso ╲       ← Few, slow, high confidence
    ╱  Compose UI Tests  ╲     ← Medium, medium speed
   ╱   ViewModel Tests    ╲    ← Many, fast
  ╱    UseCase Tests       ╲
 ╱     Repository Tests     ╲
╱       Domain Logic         ╲  ← Most, instant
```

## Unit Test Infrastructure

```kotlin
// TestDispatcherRule
class TestDispatcherRule(
  private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
  override fun starting(description: Description) {
    Dispatchers.setMain(testDispatcher)
  }
  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}

// Base ViewModel test class
abstract class ViewModelTest {
  @get:Rule val dispatcher = TestDispatcherRule()
  protected val testScope = TestScope(dispatcher.testDispatcher)
}
```

## ViewModel Tests

```kotlin
class HomeViewModelTest {
  @get:Rule val dispatcher = TestDispatcherRule()

  private val repository = FakeItemRepository()
  private val viewModel = HomeViewModel(GetItemsUseCase(repository))

  @Test
  fun `initial state is loading`() = runTest {
    assertThat(viewModel.uiState.value).isInstanceOf(UiState.Loading::class.java)
  }

  @Test
  fun `loads items successfully`() = runTest {
    repository.setItems(listOf(Item(1, "Test")))

    // Turbine: test Flow emissions
    viewModel.uiState.test {
      val loading = awaitItem()
      assertThat(loading).isInstanceOf(UiState.Loading::class.java)

      val success = awaitItem()
      assertThat(success).isInstanceOf(UiState.Success::class.java)
      assertThat((success as UiState.Success).items).hasSize(1)
    }
  }
}
```

## Repository Tests (with Fakes)

```kotlin
class FakeItemRepository : ItemRepository {
  private val items = MutableStateFlow<List<Item>>(emptyList())

  fun setItems(list: List<Item>) { items.value = list }

  override fun observeAll(): Flow<List<Item>> = items.asStateFlow()
  override suspend fun refresh() { /* no-op for tests */ }
}

class ItemRepositoryTest {
  private val dao = FakeItemDao()
  private val api = FakeItemApi()
  private val repo = ItemRepositoryImpl(dao, api)

  @Test
  fun `observeAll returns cached data`() = runTest {
    dao.upsert(listOf(Item(1, "Test")))
    val result = repo.observeAll().first()
    assertThat(result).hasSize(1)
  }
}
```

## Compose UI Tests

```kotlin
@Test
fun `home screen shows items after loading`() {
  val viewModel = HomeViewModel(FakeItemRepository().apply {
    setItems(listOf(Item(1, "Test")))
  })

  composeTestRule.setContent { AppTheme { HomeScreen(viewModel) } }

  composeTestRule.onNodeWithText("Test").assertIsDisplayed()
  composeTestRule.onNodeWithContentDescription("Item image").assertExists()
}

@Test
fun `clicking item navigates to detail`() {
  var navigatedId: Int? = null
  composeTestRule.setContent {
    HomeScreen(onItemClick = { id -> navigatedId = id })
  }
  composeTestRule.onNodeWithTag("item_1").performClick()
  assertThat(navigatedId).isEqualTo(1)
}
```

## Flow Testing with Turbine

```kotlin
@Test
fun `repository emits updates after refresh`() = runTest {
  val repo = ItemRepositoryImpl(FakeItemDao(), FakeItemApi())

  repo.observeAll().test {
    assertThat(awaitItem()).isEmpty()  // Initial
    repo.refresh()
    assertThat(awaitItem()).isNotEmpty()  // After refresh
    ensureAllEventsConsumed()
  }
}
```

## Test Coverage with Kover

```kotlin
// build-logic convention plugin
plugins { id("org.jetbrains.kotlinx.kover") }
kover {
  reports {
    filters { excludes { classes("*.di.*", "*.hilt_*") } }
    verify { rule { minBound(80) } }
  }
}
```

## Tooling Quick Reference

| Tool | Usage | Command |
|------|-------|---------|
| JUnit 5 | Unit test runner | `@Test` annotation |
| Turbine | Flow assertions | `.test { awaitItem() }` |
| MockK | Mocking library | `mockk(), coEvery { }` |
| Robolectric | Local device tests | `@Config(manifest = Config.NONE)` |
| Compose Test | UI component testing | `composeTestRule.onNodeWithText()` |
| Roborazzi | Screenshot testing | `captureRoboImage()` |
| Kover | Code coverage | `./gradlew koverHtmlReport` |
