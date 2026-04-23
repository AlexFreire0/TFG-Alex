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
- **Producción de Notificaciones (FCM) y Seguridad:**
  - *Issue:* Limpieza de endpoints de pruebas y estructuración de notificaciones UI.
  - *Solution:* Los endpoints de prueba nunca se exponen sin filtro JWT en `SecurityConfig`. En Spring Boot, `NotificationService` usa SLF4J en lugar de `System.out` y los envíos combinan `.setNotification()` más `.putData("title", title).putData("body", body)` en el mismo `Message` para asegurar procesamiento de payload en activo y pasivo.
  - *Rule for future development:* En Android, el `smallIcon` del notification builder debe ser obligatoriamente una silueta monocroma (blanco/transparente). Navegación por `PendingIntent` siempre requiere `.FLAG_IMMUTABLE`.
- **Apariencia Gráfica de Pasarelas (Stripe PaymentSheet):**
  - *Issue:* Mantener consistencia visual de marca (ParkingHole) en pantallas de terceros sin romper accesibilidad.
  - *Solution:* Instanciar `PaymentSheet.Appearance` con `PaymentSheet.Colors` (leyendo `#1976D2` primary local), `Shapes` (`cornerRadiusDp=12f`) y `Typography` para la fuente custom en res. El `Appearance` se inyecta en el objeto `PaymentSheet.Configuration` y afecta al modal emergente sin CSS.
- **Deep Linking desde FCM (Android):**
  - *Issue:* Redirigir al usuario al pulsar una notificación push, preservando el ID cuando usamos una SplashScreen Activity intermedia.
  - *Solution:* Extraer payloads de `remoteMessage.data` en el `FirebaseMessagingService` e inyectarlos como Extras en el `PendingIntent`. En la `MainActivity` (Splash), interceptarlos en `onCreate` y `onNewIntent`. Crucialmente, re-inyectarlos (`intent.extras?.let { intentDestino.putExtras(it) }`) al Intent que dispara la Activity real (e.g. `InicioActivity`) para evitar que se pierdan en la transición limpia del `finish()`.
- **Aperturas de UI y Cierre de Intercambio (Reservas):**
  - *Issue:* Crear un destino final dinámico sincronizado tras seguir un Deep Link de FCM.
  - *Solution:* Construir `DetalleReservaActivity.kt` interceptando el extra `"id_intercambio"`. Utiliza Retrofit (`lifecycleScope.launch(Dispatchers.IO)`) llamando a un endpoint `@GET("api/intercambios/{id}")` para pintar el estado real de BBDD en lugar de fiarse de cachés perdidos.
  - *Rule for future development:* Para transicionar el `Intercambio` a `Completado`, se requiere que el usuario (vendedor/comprador) confirme con el `PIN` de seguridad en UI. Ese PIN se empaqueta en el click listener enviando POST a `/completar/{id}`, lo que libera el PaymentIntent retenido en el backend.
- **Android Resource Linking Failed (XML):**
  - *Issue:* Usar `?attr/colorBackground` o `?attr/textColorSecondary` provoca rotura total de Gradle si el Theme base no declara explícitamente este `attr`.
  - *Rule for future development:* Utilizar siempre `#Hexadecimal` directo (o `@color/...`) al maquetar vistas modulares para proyectos TFG/críticos sin depender de diccionarios de Tema dudosos.
- **Spring Boot Compilation & Phantom Errors:**
  - *Issue:* "GoogleCredentials cannot be resolved" tras haber editado el `pom.xml` o arrancar `Api_ParkingHole`.
  - *Solution:* Suele ser un falso negativo de la caché de build. Las dependencias (`firebase-admin:9.2.0`) y los imports (`GoogleCredentials`) sí residen en el código fuente. Se soluciona forzando "Reload Project" de Maven en la raíz y purgado de Invalidades en el IDE.
- **Wizard UI Pattern in Android:**
  - *Issue:* Formularios largos y selección de mapa en la misma pantalla aturden y provocan altas tasas de rebote.
  - *Solution:* Implementar Wizard de 2 Fases bajo un único ConstraintLayout. Ocultar/Mostrar Views (`View.GONE`, `View.VISIBLE`) es mucho más rápido y estable que inyectar Múltiples Fragmentos para TFG, interceptando retrocesos con `onBackPressedDispatcher.addCallback()`. El Marker estático inyectado a la Cámara en vez del Marker dinámico mejora la UX geoespacial.
- **RecyclerView ID Mappings (ID = 0 Bug):**
  - *Issue:* Las listas de entidades muestran `0` como identificador cuando el Intent intenta serializar objetos complejos.
  - *Solution:* Reasignar los nombres de ID estrictamente en el mapeo en vez de presuponer `id`, y utilizar `reserva.id.toString()` en el `Intent.putExtra()`.
- **Parallel Dispatching in Collections (P2P Android):**
  - *Issue:* Múltiples peticiones HTTP a endpoints separados (`compras` vs `ventas`) causaban carga asíncrona desordenada en pantallas de historial.
  - *Solution:* Emplear `lifecycleScope.launch(Dispatchers.Main)` e inyectar tareas de red en variables `async(Dispatchers.IO) { call.execute() }`. Finalizar iteración haciendo barrera en `.await()`, concatenar colecciones en tiempo lineal (`listA + listB`) y aplicar `sortedByDescending` antes del binding.
- **Native Context MapViews & Memory Leaks:**
  - *Issue:* Incrustar un `MapView` puro sin fragmento rompe el ciclo de vida de la API de Google Play Services provocando fugas de memoria y pantallazos blancos.
  - *Solution:* Activar delegación explícita overriding manualmente en la Actividad madre: `mapView.onCreate()`, `onStart()`, `onResume()`, `onPause()`, `onStop()`, `onDestroy()`, `onSaveInstanceState()` y `onLowMemory()`.
- **Producción y Despliegue (Google Cloud Run):**
  - *Issue:* Migración del servidor de pruebas local a producción.
  - *Solution:* `RetrofitClient` debe apuntar siempre a la URL proporcionada por Cloud Run (`https://tfg-alex-732367725602.europe-west1.run.app/`) asegurando que termine con `/`. Cualquier testing futuro debe validarse contra este endpoint.
- **Stripe Payment Sheet UI Focus Bugs:**
  - *Issue:* Al llamar a `paymentSheet.present()`, los campos de texto no reaccionan al clic y no activan la aparición del teclado virtual.
  - *Solution:* Añadir `android:windowSoftInputMode="adjustResize"` a la etiqueta de la `<activity>` correspondiente en el `AndroidManifest.xml` (por ejemplo, `MapsActivity`), permitiendo así que el WebView del PaymentSheet redimensione la ventana y asigne el foco correctamente. Requiere asimismo dependencias actualizadas en `activity-ktx` y `fragment-ktx`.
- **End-to-End Data Modeling vs Decorators:**
  - *Issue:* Frontend UI inputs for "Capacity" were discarded as strings because the entity didn't natively represent them, breaking server mapping integrity.
  - *Solution:* Formalize missing keys (`capacidad`) comprehensively tracking them through SQL schemas (`VARCHAR`), Java Spring Boot entities (`@Column`), and Kotlin data classes cleanly mapping real Dropdown outputs instead.
- **PostgreSQL Native Timestamps vs JPQL & Auto-Cleanup:**
  - *Issue:* Calculating expiration logic summing an `Integer` courtesy grace period `+ interval '1 minute'` commonly fails via JPQL due to dialectal boundaries and parsing strictness. Cron Jobs add structural overhead.
  - *Solution:* Convert the Spring Data JPA `@Query` to `nativeQuery = true` executing raw Postgres operations mapping `interval` sums organically against `CURRENT_TIMESTAMP`. Additionally, run `@Modifying` update directives updating "Caducado" rows silently inside Controllers dynamically right before executing GET selects, granting autonomous sweeping traits without Cron daemons.
- **Model Synchronization & Hibernate 500 Errors (Phantom Columns):**
  - *Issue:* Adding fields like `updatedAt` to Spring Boot entities (`Coche.java`) that do not exist in the Supabase PostgreSQL database causes `DataIntegrityViolationException` / Error 500 on standard operations like `.save()` or `.findById()`.
  - *Solution:* Always ensure database schema changes (`ALTER TABLE`) are executed in the database *before* or *simultaneously* with adding `@Column` or attributes to Java entities. Removed `updatedAt` from `Coche` and explicitly added `capacidad` to SQL, Java, and Kotlin to map parking spot types to vehicles correctly.
