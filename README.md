# ParkingHole Project Context

Welcome to the **ParkingHole** project workspace. This README serves as the primary entry point for AI agents to understand the architecture, technologies, and directory structure of this application.

## 1. Overview
ParkingHole is a platform (composed of an Android mobile app and a Spring Boot backend API) that facilitates the exchange of parking spots between users. It allows users (sellers) who are leaving a parking spot to connect with users (buyers) who are looking for one. The platform manages vehicles (coches), user sessions, and payments for these exchanges.

## 2. Project Structure
The workspace is divided into three main components:

### A. Backend (`d:\MiTFG\Api_ParkingHole`)
- **Framework:** Spring Boot (v4.0.2 starter parent), Java 17.
- **Dependencies:** Spring WebMVC, Spring Data JPA, PostgreSQL Driver, Lombok, Stripe-Java (31.4.0).
- **Functionality:** RESTful API serving the mobile clients, handling business logic, database transactions, and Stripe payment intents/payouts.

### B. Frontend (`d:\MiTFG\TFG_app`)
- **Platform:** Android App built with Kotlin.
- **Minimum SDK:** 24, **Target SDK:** 36.
- **Key Libraries:**
  - **Firebase & Google Auth:** Firebase Analytics, Firebase Auth, Play Services Auth (for Google Sign-In).
  - **Networking:** Retrofit2, Gson.
  - **Maps & Location:** Google Play Services Maps, Location.
  - **UI:** ViewBinding, Material Components.
  - **Payments:** Stripe Android (22.5.0).
- **Functionality:** User interface for Google Login, viewing/managing cars (Coches), making/receiving payments (Stripe Onboarding), and finding/offering parking spots on a map.

### C. Database (`d:\MiTFG\SQL` and `d:\MiTFG\context\schema.sql`)
- **Engine:** PostgreSQL.
- **Core Entities:**
  - `usuario`: Stores user details, Google IDs, Stripe IDs (customer & connect), and preferences.
  - `coche`: Stores vehicle details (brand, model, license plate, color, image). Linked to `usuario`.
  - `intercambio`: The core transaction entity. Links a seller, a buyer, their respective cars, pricing/commission, location (`plaza_lat`, `plaza_long`), and states.
- **Logic:** Includes triggers for `updated_at` timestamps and geospatial indices for finding available spots.

## 3. How to Navigate
- For overall operational guidelines, past issues, and strict coding practices, read `AGENT.md` in this directory.
- When tasked with feature development, always trace the data flow from the DB Schema -> Java Entity / Controller -> Kotlin Data Class / UI.
- Use the provided `schema.sql` in this directory for a quick reference of the current database structure.
