# Product Requirement Document (PRD): Magistory

**Document Status:** Draft
**Target Platform:** Android (Native)
**Author:** Senior Product Manager & System Architect

---

## Executive Summary & Product Goals

Magistory adalah aplikasi editor video profesional untuk Android yang memadukan kontrol editing manual berstandar industri dengan asisten AI otonom (Agentic Workflow).

### Product Goals

1. Mengurangi waktu pembuatan draf video awal hingga 80%.
2. Menyediakan infrastruktur hybrid-costing yang memisahkan beban operasional antara pengguna gratis (mandiri) dan pengguna berbayar (terkelola).
3. Menjamin stabilitas dan performa rendering 100% di sisi klien (offline-first rendering).

---

## Screen List & Prioritas Development

### MVP (Phase 1 — Free Tier)

| # | Screen | Deskripsi |
|---|--------|-----------|
| 1 | **SplashScreen** | Logo Magistory + loading animation |
| 2 | **OnboardingScreen** | Pengenalan fitur utama (3 slides, hanya first launch) |
| 3 | **LoginScreen** | Google Sign-In (SSO) |
| 4 | **HomeScreen** | Daftar project video + tombol buat project baru |
| 5 | **NewProjectScreen** | Input prompt AI / pilih mode (AI-assisted atau manual) |
| 6 | **AIProcessingScreen** | Loading screen saat AI generate timeline |
| 7 | **TimelineEditorScreen** | Editor utama — timeline, preview, tracks |
| 8 | **ExportScreen** | Pilihan resolusi, format, progress export |
| 9 | **SettingsScreen** | API Key management (BYOK), account info |

### Phase 2 (Premium Tier)

| # | Screen | Deskripsi |
|---|--------|-----------|
| 10 | **SubscriptionScreen** | Halaman langganan via Google Play Billing |
| 11 | **AIRevisionScreen** | Conversational AI revision (chat-like UI) |

---

## App Flow / Navigation

```
Splash → Onboarding (first launch only)
       → Login (if not authenticated)
       → Home

Home → New Project → AI Processing → Timeline Editor
Home → Existing Project → Timeline Editor
Timeline Editor → Export
Home → Settings (API Keys, Account)

[Premium] Home → Subscription
[Premium] Timeline Editor → AI Revision (conversational)
```

### Navigation Rules

- **Onboarding** hanya muncul saat first launch (`DataStore` flag).
- **Login** hanya muncul jika user belum terautentikasi (Firebase Auth state).
- **Back** dari Timeline Editor kembali ke Home (project auto-saved).
- **Settings** accessible dari Home via icon di top bar.

---

## BAGIAN 1: PENGGUNA GRATIS (FREE TIER - BYOK)

Bagian ini mendefinisikan fitur, persona, dan batasan untuk pengguna yang menggunakan aplikasi tanpa biaya langganan, dengan mengandalkan sistem Bring Your Own Key (BYOK) untuk fitur AI.

### 1.1 User Persona: "The Tech-Savvy Hobbyist"

- **Profil:** Mahasiswa atau tech-enthusiast yang suka bereksperimen dengan AI dan video.
- **Pain Point:** Ingin fitur AI canggih tapi tidak ingin membayar biaya langganan bulanan yang mahal.
- **Kebutuhan:** Kebebasan memasukkan API Key LLM sendiri (BYOK), kontrol penuh atas privasi, dan fungsi editing dasar yang solid (hingga 1080p).

### 1.2 User Stories & Acceptance Criteria

| ID | User Story | Acceptance Criteria |
|----|-----------|---------------------|
| US-F1 | Sebagai pengguna gratis, saya ingin login menggunakan Google. | - Hanya menampilkan tombol Google Sign-In (SSO).<br>- Akun tersinkronisasi dengan Firebase Authentication. |
| US-F2 | Sebagai pengguna gratis, saya ingin memasukkan API Key LLM saya sendiri. | - Terdapat menu "API Keys" di Settings.<br>- Key dienkripsi dan disimpan hanya di device lokal.<br>- Notifikasi error spesifik jika kuota API Key bawaan pengguna habis. |
| US-F3 | Sebagai pengguna gratis, saya ingin AI membuat draf video dengan instruksi teks. | - AI menghasilkan naskah, mencari video & musik bebas royalti, dan menyusun timeline.<br>- Menggunakan kuota API dari Key pengguna sendiri. |
| US-F4 | Sebagai pengguna gratis, saya ingin mengekspor video saya. | - Ekspor dibatasi hingga resolusi maksimal 1080p. |

### 1.3 Functional & Non-Functional Requirements

- **Multi-Model Switcher:** Pengguna gratis dapat memilih LLM (Gemini, Nemotron, Claude, ChatGPT) asalkan mereka memiliki API Key yang sesuai.
- **Security (BYOK):** API Key milik pengguna harus dienkripsi menggunakan AES-256-GCM dan dikelola oleh Android Keystore System. Key tidak boleh dikirim ke server Magistory.
- **Local Caching:** Aset diunduh ke `Context.getExternalCacheDir()` dengan auto-purge via WorkManager (batas 2GB) agar memori HP tidak penuh.

---

## BAGIAN 2: PENGGUNA BERBAYAR (PREMIUM TIER)

Bagian ini mendefinisikan pengalaman premium yang terkelola penuh (managed service), di mana pengguna mendapatkan akses AI instan tanpa repot mengatur kunci API, beserta fitur editing tingkat lanjut.

### 2.1 User Persona: "The Efficient Creator"

- **Profil:** Content creator TikTok/Reels/YouTube Shorts yang memproduksi konten setiap hari.
- **Pain Point:** Menghabiskan waktu berjam-jam mencari stock footage b-roll dan mencocokkan beat musik.
- **Kebutuhan:** Otomatisasi AI yang instan (Zero-setup), tanpa repot mengatur API Key, render 4K yang cepat.

### 2.2 User Stories & Acceptance Criteria

| ID | User Story | Acceptance Criteria |
|----|-----------|---------------------|
| US-P1 | Sebagai pengguna premium, saya ingin berlangganan agar tidak perlu repot mencari API Key. | - Integrasi Google Play Billing API berhasil.<br>- Status premium divalidasi via Firebase. |
| US-P2 | Sebagai pengguna premium, saya ingin AI langsung memproses video saya. | - API request dirutekan melalui Cloudflare Workers milik Magistory.<br>- Tidak ada menu permintaan API Key. |
| US-P3 | Sebagai pengguna premium, saya ingin mengekspor video dengan kualitas tertinggi. | - Opsi ekspor 4K (Ultra HD) terbuka dan dapat digunakan.<br>- Dukungan hingga 60 FPS. |
| US-P4 | Sebagai pengguna premium, saya ingin meminta AI merevisi draf tanpa merusak proyek. | - Agentic AI membaca current state timeline.<br>- UI menampilkan prompt klarifikasi jika instruksi ambigu (Conversational Logic). |

### 2.3 Functional & Non-Functional Requirements

- **Agentic Pipeline via Edge:** Request AI dikirim ke server edge Magistory untuk diproses menggunakan Enterprise API Key.
- **Performance Engine:** Memanfaatkan MediaCodec API untuk hardware acceleration secara maksimal, memastikan ekspor 4K 60FPS berjalan lancar di sisi klien tanpa server latensi.
- **Telemetry:** Memantau ketat Crash-Free Sessions (>99.5%) terutama untuk pengguna premium yang melakukan rendering 4K, menggunakan Firebase Crashlytics.

---

## Data Models

### Project

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| id | Long | No | Auto-generated PK |
| title | String | No | Nama project |
| createdAt | Long | No | Epoch millis |
| updatedAt | Long | No | Epoch millis |
| thumbnailPath | String | Yes | Local file path |
| resolution | String | No | "1080p" atau "4K" |
| promptText | String | Yes | Prompt AI yang digunakan |

### TimelineTrack

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| id | Long | No | Auto-generated PK |
| projectId | Long | No | FK to Project |
| type | String | No | "VIDEO", "AUDIO", "TEXT" |
| orderIndex | Int | No | Urutan track di timeline |

### TimelineClip

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| id | Long | No | Auto-generated PK |
| trackId | Long | No | FK to TimelineTrack |
| sourceUri | String | No | Local path atau URL |
| startTimeMs | Long | No | Posisi di timeline |
| endTimeMs | Long | No | Posisi akhir di timeline |
| trimStartMs | Long | No | Trim awal di source |
| trimEndMs | Long | No | Trim akhir di source |
| label | String | Yes | Teks overlay (untuk type TEXT) |

### ApiKeyEntry

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| id | Long | No | Auto-generated PK |
| providerName | String | No | "gemini", "openai", "claude", "nemotron" |
| encryptedKey | ByteArray | No | AES-256-GCM encrypted |
| iv | ByteArray | No | Initialization Vector |
| isActive | Boolean | No | Key yang sedang digunakan |
