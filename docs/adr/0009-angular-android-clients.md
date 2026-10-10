# ADR 0009: Angular and Android Clients

**Status:** proposed
**Owner:** dinesh-sh886
**Date:** 2026-10-10

## Context

The platform serves two client types: a web dashboard for gym staff/admins and a native mobile app for members. Both consume the same backend APIs.

## Decision

### Web: Angular 18+

- **Framework**: Angular 18 (standalone components, signals, SSR optional)
- **Language**: TypeScript 5.x, strict mode
- **Build**: Vite (via Angular CLI), npm workspaces
- **State**: Signals + RxJS for async; NgRx only if complexity warrants
- **UI**: Angular Material or Tailwind CSS (decide per component)
- **API Client**: Generated from OpenAPI (ADR 0006) → `web/src/app/api/`
- **Auth**: HttpInterceptor attaches JWT; silent refresh via refresh token
- **Structure**:
  ```
  web/
  ├── src/app/
  │   ├── core/          # Auth, guards, interceptors, services
  │   ├── features/      # Feature modules (booking, membership, etc.)
  │   ├── shared/        # UI components, pipes, directives
  │   └── api/           # Generated OpenAPI client (not committed)
  ├── angular.json
  ├── package.json
  └── tsconfig.json
  ```

### Mobile: Native Android (Kotlin)

- **Language**: Kotlin 2.0+, target SDK 34, min SDK 24
- **Architecture**: Clean Architecture + MVVM + Flow
- **DI**: Hilt (Dagger)
- **Networking**: Retrofit + OkHttp + Kotlinx Serialization (JSON) / Protobuf (future)
- **API Client**: Generated from OpenAPI (ADR 0006) → `mobile/app/src/main/java/com/gix/gym/api/`
- **Database**: Room (offline cache, sync via repository)
- **Auth**: EncryptedSharedPreferences for tokens; BiometricPrompt for unlock
- **Structure**:
  ```
  mobile/
  ├── app/
  │   ├── src/main/java/com/gix/gym/
  │   │   ├── data/      # Repository, DAO, API, DTO
  │   │   ├── domain/    # Use cases, entities, repository interfaces
  │   │   ├── presentation/ # ViewModel, UI (Compose)
  │   │   └── di/        # Hilt modules
  │   └── build.gradle.kts
  ├── gradle/
  ├── build.gradle.kts
  └── settings.gradle.kts
  ```

### Shared Contracts

- OpenAPI specs in `contracts/openapi/` are the **single source of truth**
- Both clients generate code at build time (CI + local)
- Breaking API changes detected in CI before client builds

### Local Development

- **Web**: `npm run dev` (Vite dev server, proxy to `localhost:8080`)
- **Mobile**: Android Studio → Run on emulator/device; `10.0.2.2` maps to host `localhost`
- **Backend**: Services run on distinct ports (gateway: 8080, or direct service ports)

## Consequences

### Positive
- Native Android: best performance, platform integration, offline support
- Angular: mature ecosystem, enterprise adoption, strong typing
- Shared contracts eliminate API drift
- Independent deployment pipelines per client

### Negative
- Two codebases to maintain (mitigated by shared contracts + generated clients)
- Android requires macOS/Linux/Windows with Android Studio (not just WSL)
- Generated code may need manual adaptation for complex UX flows
- Initial setup overhead for both platforms

## Verification

- `cd web && npm ci && npm run build` succeeds
- `cd mobile && ./gradlew assembleDebug` succeeds
- Generated API clients compile without errors
- E2E test: Web app → login → list bookings (via running backend)
- E2E test: Mobile app → login → list bookings (via running backend)

## References

- [Angular 18 Release](https://blog.angular.io/angular-18-is-here/)
- [Android Developer Guide](https://developer.android.com/guide)
- [OpenAPI Generator](https://openapi-generator.tech/)
- ADR 0006 (REST & OpenAPI), ADR 0007 (Auth)