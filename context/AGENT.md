# AGENT global context & Guidelines

This file is the single source of truth for an AI Agent operating within the ParkingHole workspace. It contains necessary skills, established good practices, and a history of past issues and solutions to ensure consistency and prevent regressions.

## 1. Required Skills & Knowledge
- **Android Development:** Kotlin, Android SDK, ViewBinding, SharedPreferences, Intents (flag clearing), BottomSheetDialogFragments, Material Design (Theming & Dark Mode).
- **Backend Development:** Java 17, Spring Boot, Spring Data JPA, REST APIs, JSON Serialization (Gson/Jackson).
- **External Integrations:** Firebase Auth, Google Sign-In SDK, Google Maps API, Stripe API (Java + Android components).
- **Database:** PostgreSQL schema design, relationships, and triggers.

## 2. Strict Good Practices (Must Follow)
1. **Implementation Plans:** BEFORE making any code changes, always generate an `implementation_plan.md` outlining the exact files and lines to change, and wait for USER approval if necessary.
2. **End-to-End Sync:** When a database schema changes (e.g., adding a `color` to a car or a Stripe ID), you MUST update:
   - The PostgreSQL SQL Schema.
   - The Spring Boot Java Entity (`@Column`, `@Entity`).
   - The Android Kotlin Data Class (adding `@SerializedName` if necessary).
   - The Android UI components (Adapters, XML) to reflect the new data.
3. **Session Management:** Avoid hardcoding user IDs. Always use the central `SessionManager.kt` referencing `SharedPreferences` to fetch the current logged-in user ID for API calls.
4. **UI Reactivity over Backend Complexity:** If a purely visual constraint applies (e.g., maximum 2 cars per user), handle the visual warnings (like hiding Floating Action Buttons and showing MaterialCardViews) in the Android Fragments/XML, without necessarily cluttering the backend with UI-specific logic.
5. **Always Update Context (Internal & Public):** 
   - INTERNAL: If you introduce major architectural changes, new libraries, or fix a significant recurring bug, UPDATE this `AGENT.md` file to reflect the new knowledge and prevent future errors.
   - PUBLIC: Ensure the root `README.md` is kept up-to-date with any new features, tech stack modifications, or implementation details. The `README.md` serves as the public face of the project and must reflect the current state of the application.

## 3. Historical Issues & Solutions (Lessons Learned)
- **Google Auth Flows & Logout:** 
  - *Issue:* Logging out left the cached Google session active, skipping the login screen on the next attempt.
  - *Solution:* Ensure the native Google session (`GoogleSignInClient.signOut()`) is cleared BEFORE clearing the local session `SharedPreferences`. In `LoginActivity`, ensure local session is saved ONLY after the backend confirms the login response.
  - *Navigation:* Always use `Intent.FLAG_ACTIVITY_NEW_TASK` or `Intent.FLAG_ACTIVITY_CLEAR_TASK` when redirecting to `LoginActivity` to prevent navigating back to closed sessions.
- **Backend-Frontend Serialization Desync:** 
  - *Issue:* The Android app showed only 1 car when the backend had 3 for that user.
  - *Solution:* Ensure JSON serialization lists are correctly built in the Spring Boot controller, and that the Kotlin Data Class accurately mirrors the emitted JSON structure without swallowing list elements via improper parsing.
- **Responsive Layouts & Dark Mode:**
  - *Issue:* Buttons and text were invisible in Dark Mode on devices like Poco X7 Pro.
  - *Solution:* Never hardcode black/white colors (`#000000`). Always use theme attributes (`?attr/colorOnPrimary`, `?attr/colorSurface`) in XML layouts so they dynamically adapt. Ensure responsive sizing using `ConstraintLayout` rather than fixed `dp` for critical views.
- **Stripe Onboarding Navigation:**
  - *Issue:* Flow required for setting up Stripe Seller accounts.
  - *Solution:* Use an async Retrofit call with a loading state to fetch the onboarding URL, then launch an external browser intent to complete Stripe onboarding securely.
- **Stripe Payment Security (Price Tampering):**
  - *Issue:* The Android client was dictating the `precioTotal` to the Spring Boot backend, allowing maliciously modified APKs to purchase parking spots for pennies.
  - *Solution:* Refactored `PagoControlador` to only accept `idIntercambio`. The backend now fetches the real price from PostgreSQL (`precio_total_comprador`) and calculates fees server-side.
  - *Verification:* Implemented a secure Stripe Webhook (`/api/pagos/webhook`) that listens for `payment_intent.succeeded` and verifies the `Stripe-Signature` before marking an `Intercambio` as `"Reservado"`. Never trust the client for payment confirmations.
- **IDOR Vulnerabilities & JWT Authentication:**
  - *Issue:* Controllers trusted `uid` parameters from the client, allowing IDOR (Insecure Direct Object Reference) attacks and lacking 120-day persistent sessions.
  - *Solution:* Implemented Spring Security with `JwtRequestFilter`. The Android app stores the token in `SessionManager` and attaches it globally via `AuthInterceptor` configured in a custom `MyApplication` class. `MainActivity` routes users based on local decoding of the JWT `exp` claim.
  - *Rule for future development:* NEVER trust IDs passed in the request body or path for sensitive actions (Creation, Deletion, Modification). Extract the authenticated user's email via `Principal.getName()` mapped by Spring Security.
- **OkHttp Interceptor & Kotlin Interoperability:**
  - *Issue:* When writing OkHttp `Interceptor`s in Kotlin, accessing `request.url.encodedPath` can cause compilation errors ("cannot access url: it is package-private in Request") depending on the OkHttp version (specifically 3.x vs 4.x).
  - *Solution:* Always use the explicit getter methods `request.url()` and `url.encodedPath()` instead of property access syntax to ensure backwards compatibility with OkHttp Java classes.
  - *Rule for future development:* In `AuthInterceptor`, ensure public endpoints like `webhook` are explicitly excluded from receiving the `Authorization` header to prevent breaking third-party integrations (like Stripe).
- **Kotlin Null Safety with Gson/Retrofit & Login Token NPE:**
  - *Issue:* Crash during Google Login because `authResponse.token` in `LoginActivity.kt` was `null` and bypassed Kotlin's `String` non-nullable strictness due to Gson deserialization of a missing or differently mapped JSON response field.
  - *Solution:* Always use `@SerializedName` in Kotlin Data Classes mapped to backend DTOs. When parsing critical security data like a JWT, declare the type as nullable `String?` and validate `!token.isNullOrEmpty()` before saving to `SessionManager`. 
  - *Rule for future development:* Do not rely entirely on Kotlin's null-safety for JSON deserialization via frameworks like Gson without explicit mapping. Map exact keys properly and safely handle potential nulls via defensive programming before interacting with data stores like `SharedPreferences`.
- **Silent Login & JWT Decoder on Android:**
  - *Issue:* Despite having a 120-day token, the application constantly forced the user to see `LoginActivity` on boot.
  - *Solution:* Implemented `isTokenExpired()` in `SessionManager`. It extracts the payload by splitting the string by `.`, decoding the `android.util.Base64`, parsing as JSON, and comparing `exp * 1000` with `System.currentTimeMillis()`. In `LoginActivity.onCreate()`, this is checked *before* `setContentView()`. If valid, it immediately routes to `MapsActivity` con `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`. If expired, it calls `SessionManager.cerrarSesion()` to wipe local data and proceeds to render the login view.
  - *Rule for future development:* When adding persistent sessions via JWT, always implement a `Silent Login` verification layer early in the boot sequence (e.g., Application class or initial Activity) to bypass auth flow securely without making a redundant network request.
- **Firebase Cloud Messaging Integration Architecture:**
  - *Issue:* Delivering event-driven notifications to Android users reliably.
  - *Solution:* Android devices request a token from the FCM SDK inside the landing Activity (`InicioActivity`) and transmit it to Spring Boot via an `@PUT` endpoint securely behind the JWT interceptor. Spring Boot stores it in `Usuario.fcmToken`. With `firebase-admin` loaded via the JSON service account, the server dispatches pushes to those exact tokens globally. Notifications are drawn automatically by extending `FirebaseMessagingService`.
  - *Rule for future development:* When handling FCM Tokens, don't trust static device IDs. Device tokens rotate; therefore, the Android app must upload its token on every app boot or `onNewToken` callback. The backend must tie those tokens robustly via `Principal` context to prevent users from spoofing token assignments. Additionally, for Android 13+ (API 33), explicitly implement `ActivityResultContracts.RequestPermission()` for `android.permission.POST_NOTIFICATIONS` so that the channel isn't muted from the start.
- **Stripe Webhook and Real-time Notifications:**
  - *Issue:* How to notify a seller reliably when a payment is processed externally by Stripe.
  - *Solution:* When creating the `PaymentIntent` via `PaymentIntentCreateParams`, inject the seller's email into `.putMetadata("vendedor_email", ...)`. Then, intercept the `payment_intent.succeeded` event in the Webhook, extract `paymentIntent.getMetadata().get("vendedor_email")`, map it to the database `Usuario`, and finally trigger `notificationService.sendPushNotification(...)` with their `fcmToken`. This guarantees pushes are only sent if Stripe validates the funds.
