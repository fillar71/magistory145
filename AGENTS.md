# System Instructions: Magistory Project Workflow

You are working on **Magistory**, a professional Android video editor that integrates manual editing with an autonomous AI assistant (Agentic Workflow). 

This document defines the strict workflow and architectural guidelines you MUST follow when modifying or adding features to this project.

## 1. Architectural Blueprint
- **Pattern:** Clean Architecture with MVVM + Unidirectional Data Flow (UDF).
- **Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room DB (Offline-first), Hilt (DI), Retrofit, ExoPlayer (Media3), Firebase.
- **Package Structure:** Feature-driven within `data`, `domain`, `ui`, `di`, `security`, and `worker` packages.

## 2. Core Development Workflow

When implementing a new feature or modifying an existing one, strictly follow these steps sequentially:

### Step 1: Define Domain & Data Models
- Create or update **Room Entities** (`@Entity`) and **DAOs** in `data/local/`.
- Create or update **DTOs** (Moshi) and API interfaces in `data/remote/`.
- Define the core **Domain Models** in `domain/model/`.
- Ensure Data models (Entities/DTOs) are mapped to Domain Models inside the Repository implementations.

### Step 2: Repository & Use Cases
- Define Repository interfaces in `domain/repository/`.
- Implement the Repositories in `data/repository/`, handling offline-first logic (fetching from Room DB, syncing from Remote).
- Build **Use Cases** in `domain/usecase/` for complex or agentic business logic (e.g., `GenerateTimelineUseCase`, `ExportVideoUseCase`).

### Step 3: State Management (ViewModel - UDF Pattern)
- Define an immutable `data class ScreenUiState` for the screen.
- Define a `sealed class ScreenUiEvent` for side effects (e.g., Navigation, Error Snackbars).
- ViewModels MUST expose state as `StateFlow<ScreenUiState>` via `_uiState.asStateFlow()`.
- ViewModels MUST expose events as `Flow<ScreenUiEvent>` via `Channel.receiveAsFlow()`.
- **Rule:** Never expose `MutableStateFlow` or mutable variables to the UI.

### Step 4: UI Implementation (Jetpack Compose)
- Adhere strictly to **Material Design 3 (M3)** guidelines.
- Use `collectAsStateWithLifecycle()` to collect state safely in Compose.
- Keep Composables pure and stateless where possible; hoist state and event handling to screen-level Composables.
- Use `LaunchedEffect` to collect and handle `UiEvent` streams from the ViewModel.

### Step 5: Dependency Injection (Hilt)
- Register all new DAOs, Repositories, and UseCases in their respective Hilt modules (`di/DatabaseModule.kt`, `di/RepositoryModule.kt`, `di/NetworkModule.kt`).
- Annotate ViewModels with `@HiltViewModel` and inject dependencies via constructor.

## 3. Specialized System Rules

### A. Free Tier (BYOK) vs. Premium Tier Rules
- **Free Tier (Local BYOK):** AI requests MUST use the user's local API key (Bring Your Own Key). Use `ApiKeyManager` and `EncryptionHelper` (AES-256-GCM via Android Keystore) to decrypt the key *only* at runtime memory. Limit rendering export to 1080p.
- **Premium Tier (Edge Cloud):** AI requests MUST route via the `Cloudflare Workers` endpoint. Do NOT inject raw Enterprise API keys on the Android client. Enable 4K 60FPS exporting.

### B. Agentic AI & Timeline Generation Contract
- Always instruct the LLM to output JSON matching the `LlmTimelineDto` contract (Title, Description, and Tracks: VIDEO, AUDIO, TEXT).
- **Parsing Pipeline:** LLM JSON Response -> Moshi Deserialize -> Mapper to Domain Model -> Asset Resolver (Concurrent Pexels/Pixabay/Freesound API calls) -> Download to Cache -> Save to Room DB -> Render via ExoPlayer.

### C. Performance & Resource Management
- **Media Caching:** Download media assets to `Context.getExternalCacheDir()`. Enforce cache limits (1GB Free, 5GB Premium) and implement automated cleanup via `WorkManager`.
- **Database:** Ensure Room database uses Write-Ahead Logging (WAL) for stable concurrent timeline operations.
- **Video Rendering:** Maximize MediaCodec API for hardware acceleration. Export heavy operations MUST be offloaded to `WorkManager` (e.g., `ExportWorker`).

## 4. Agent Skill Integration Policy
When executing this workflow, you (the AI Agent) possess built-in skills (e.g., `room-database-integration`, `gemini-api`, `design-guidelines`). 
- **Rule:** You MUST seamlessly blend these skills into the Magistory Architecture. 
- Do NOT use the default folder structures suggested by the skills if they contradict the `Clean Architecture` rules in Section 1 and 2. Adapt the skill's code patterns to fit `domain/`, `data/`, and `ui/` layers appropriately.