# 🕵️‍♂️ Auditoría de Arquitectura y Código: ParkingHole
**Fecha:** Marzo 2026 | **Rol:** Staff Software Engineer

Tras un análisis exhaustivo del código fuente del monorepo (Backend Spring Boot + Frontend Kotlin Android), aquí presento el informe estructurado del estado actual del proyecto, destacando vulnerabilidades, cuellos de botella y un roadmap claro para las siguientes implementaciones (Stripe).

---

## 🏗️ 1. Casos Límite (Edge Cases) en Negocio

Actualmente la lógica del `Intercambio` asume el "Happy Path" (el usuario reserva y ambos aparecen). Faltan validaciones vitales para el mundo real:

*   🔴 **Usuarios Fantasma (No-Show):** El campo `cortesia_minutos` se guarda en la DB pero *no hace nada*. Si un comprador reserva a las 15:00 con 5 min de cortesía, y son las 15:30, la plaza sigue bloqueada en estado `"Reservado"`, impidiendo al vendedor ganar dinero.
    *   *Solución:* Crear una tarea programada (`@Scheduled`) en Spring Boot que busque intercambios `"Reservado"` donde `momento_intercambio_previsto + cortesia_minutos < AHORA`. Si los encuentra, los revierte a `"Esperando"` y penaliza al comprador.
*   🔴 **Plazas Caducadas en el Mapa:** El endpoint `/api/intercambios/disponibles` y `MapsActivity.kt` cargan todas las plazas `"Esperando"`. Si alguien ofreció su plaza para ayer y nadie la compró, seguirá saliendo hoy en el mapa.
    *   *Solución:* Modificar la query del Repositorio para filtrar: `WHERE estado_intercambio = 'Esperando' AND momento_intercambio_previsto > CURRENT_TIMESTAMP`.
*   🟡 **Crash durante el Pago:** En `MapsActivity.kt`, si el móvil se apaga justo cuando dice `"PROCESANDO PAGO..."`, la pantalla de Stripe no se carga. Al no haber Webhooks (ver punto 5), el estado queda en el limbo.

---

## 🔐 2. Seguridad y Robustez (Crítico)

El backend de Spring Boot actualmente actúa como un CRUD abierto sin capa de seguridad real.

*   🔴 **Ausencia de Autorización (No Spring Security):** No existe configuración de Spring Security ni validación de JWT. Endpoints como `/api/usuarios/vertodos` devuelven la base de datos entera de usuarios a cualquiera que tenga la URL (incluyendo hashes de contraseñas).
    *   *Solución (Prioridad Alta):* Implementar JWT. El SessionManager de Android debe guardar un token firmado, enviarlo en el header `Authorization: Bearer <token>`, y Spring Boot debe rechazar peticiones sin él.
*   🔴 **Ataques IDOR (Insecure Direct Object Reference):**
    *   **Eliminar Coches:** En `CocheControlador.java` -> `DELETE /api/coches/eliminar/{cid}`. Cualquier persona puede enviar un HTTP DELETE con el ID del coche de otro usuario, y se borrará logicamente, ya que no verificas si el `DuenoUid` coincide con el que hace la petición.
    *   **Reservar Plazas:** En `IntercambioControlador.java`, recibes el `idComprador` como parámetro de la URL. Un atacante podría interceptar la llamada y poner el ID de otro comprador para gastarle el dinero a él.
    *   *Solución:* Nunca te fíes de los IDs enviados por el cliente. Extrae el ID del usuario del token JWT autenticado en el servidor.

---

## ⚡ 3. Rendimiento (Performance)

*   🟡 **Refresco Estático del Mapa:** `MapsActivity` carga los marcadores una única vez al abrir el fragmento (`obtenerIntercambiosDisponibles`). Si un usuario se queda mirando el mapa 10 minutos, no verá nuevas plazas ni desaparecerán las que ya se han comprado, a menos que salga y vuelva a entrar.
    *   *Recomendación:* Implementar **Server-Sent Events (SSE)** o **WebSockets** en Spring Boot. El mapa en Android debe suscribirse a un flujo continuo de datos. Cuando un coche libera o reserva una plaza, el marcador debe animarse en tiempo real en los dispositivos de tu alrededor, sin hacer F5.

---

## 🧹 4. Deuda Técnica y Refactorización

*   🟡 **Filtro de Arquitectura (MVVM):** Los archivos de Android (`MapsActivity.kt` y `OfrecerPlazaActivity.kt`) tienen más de 180-300 líneas de código porque mezclan manipulación de UI (`binding.tvGananciaVendedor.text = ...`) con lógica de red asíncrona (callbacks de `RetrofitClient`).
    *   *Solución:* Empezar a migrar hacia el patrón **MVVM** (Model-View-ViewModel). Los callbacks de Retrofit deben ir a un Repositorio, que pasa los datos a un ViewModel. La Activity solo debe observar `LiveData` o `StateFlow` y pintar la UI. Evitarás "Memory Leaks" si el usuario rota la pantalla mientras carga una llamada de red.

---

## 💳 5. ROADMAP: El Siguiente Gran Paso (Pagos con Stripe)

He analizado tu `PagoControlador.java` actual. Aunque dominas la sintaxis básica del SDK de Stripe (PaymentIntent y EphemeralKeys), hay un hueco de seguridad gravísimo que debes tapar antes de subir a producción.

**El Problema Crítico:** 
En `crearPaymentSheet`, estás leyendo `precioTotal = data.get("precioTotal")` directamente del móvil Android. ¡Un usuario podría hackear el APK, enviar `precioTotal: 0.10` a tu endpoint, y reservar la plaza de 50€ pagando solo 10 céntimos!

**Roadmap de Implementación Segura (Prioridad Extrema):**

1.  **Backend - Precio como Fuente de la Verdad:**
    *   El endpoint `/crear-payment-sheet` YA NO debe recibir el precio. Debe recibir el `id_intercambio`.
    *   Spring Boot busca ese ID en PostgreSQL, extrae el `precio_total_comprador` real de la base de datos, calcula la comisión y genera el PaymentIntent con **ese** precio inquebrantable.
2.  **Backend - Webhooks (La única forma real de confirmar el pago):**
    *   Actualmente confías en que Android te llame y te diga "Stripe me ha devuelto Completed". Nunca te fíes del cliente de Android.
    *   Crea un endpoint público `/stripe-webhook`. Cuando un pago se capture realmente en los servidores de Stripe, Stripe llamará en secreto a **tu backend** con el evento `payment_intent.succeeded`.
    *   Es en ese webhook donde cambias el estado del intercambio a `"Completado"` de forma 100% segura.
3.  **Frontend - Flujo Correcto:**
    *   Android lanza PaymentSheet.
    *   Si da error, avisa. Si da OK, no llamas directamente a completar nada importante; simplemente le dices a la UI "Redirigiendo...". Tu backend lo habrá completado instantáneamente por detrás mediante el webhook.

---
*Fin del Informe. ¿Deseas que empecemos a abordar alguno de los puntos críticos (como los Webhooks de Stripe o el cierre de seguridad de los endpoints) de forma inmediata?*
