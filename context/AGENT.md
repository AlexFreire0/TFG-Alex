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
