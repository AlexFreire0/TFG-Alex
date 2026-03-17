package com.parkinghole.api_parkinghole.controladores;

import com.parkinghole.api_parkinghole.modelos.Intercambio;
import com.parkinghole.api_parkinghole.repositorio.IntercambioRepositorio;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/intercambios")
public class IntercambioControlador {

    @Autowired
    private IntercambioRepositorio repository;

    @GetMapping("/disponibles")
    public List<Intercambio> obtenerDisponibles() {
        return repository.findByEstadoIntercambio("Esperando");
    }

    @PostMapping("/ofrecer")
    public ResponseEntity<?> ofrecerPlaza(@RequestBody Intercambio nuevoIntercambio) {
        try {
            nuevoIntercambio.setEstadoIntercambio("Esperando");
            if (nuevoIntercambio.getCreatedAt() == null) {
                nuevoIntercambio.setCreatedAt(LocalDateTime.now());
            }
            Intercambio guardado = repository.save(nuevoIntercambio);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: El coche seleccionado ya tiene una oferta activa.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/reservar/{id}")
    public ResponseEntity<?> reservarPlaza(
            @PathVariable Long id,
            @RequestParam Long idComprador,
            @RequestParam Long idCocheComprador,
            @RequestParam String paymentIntentId // <--- NUEVO: Recibimos el ID del pago retenido
    ) {
        return repository.findById(id).map(intercambio -> {
            if (!"Esperando".equals(intercambio.getEstadoIntercambio())) {
                return ResponseEntity.badRequest().body("Esta plaza ya no está disponible");
            }

            // Generamos PIN
            String pinGenerado = String.format("%04d", new java.util.Random().nextInt(10000));
            intercambio.setCodigoVerificacion(pinGenerado);

            // Guardamos datos de reserva
            intercambio.setIdComprador(idComprador);
            intercambio.setIdCocheComprador(idCocheComprador);
            intercambio.setPaymentIntentId(paymentIntentId); // <--- NUEVO: Vinculamos el pago
            intercambio.setEstadoIntercambio("Reservado");

            repository.save(intercambio);
            return ResponseEntity.ok(intercambio);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mis-ofrecidas/{idVendedor}")
    public ResponseEntity<List<Intercambio>> obtenerMisPlazasOfrecidas(@PathVariable Long idVendedor) {
        List<Intercambio> misPlazas = repository.findByIdVendedorOrderByCreatedAtDesc(idVendedor);
        return ResponseEntity.ok(misPlazas);
    }

    @GetMapping("/mis-reservas/{idComprador}")
    public ResponseEntity<List<Intercambio>> obtenerMisReservas(@PathVariable Long idComprador) {
        List<Intercambio> misReservas = repository.findByIdCompradorOrderByCreatedAtDesc(idComprador);
        return ResponseEntity.ok(misReservas);
    }

    @PostMapping("/completar/{id}")
    public ResponseEntity<?> completarIntercambio(
            @PathVariable Long id,
            @RequestParam String pinIngresado) {
        return repository.findById(id).map(intercambio -> {
            // 1. Validar PIN
            if (intercambio.getCodigoVerificacion() == null ||
                    !intercambio.getCodigoVerificacion().equals(pinIngresado)) {
                return ResponseEntity.badRequest().body("PIN incorrecto.");
            }

            // 2. --- CAPTURAR EL PAGO EN STRIPE ---
            try {
                if (intercambio.getPaymentIntentId() != null) {
                    // Recuperamos el pago de Stripe y lo capturamos (liberamos el dinero)
                    PaymentIntent intent = PaymentIntent.retrieve(intercambio.getPaymentIntentId());
                    intent.capture();
                }

                // 3. Si el pago se capturó bien (o no había pago), completamos
                intercambio.setEstadoIntercambio("Completado");
                repository.save(intercambio);

                return ResponseEntity.ok(intercambio);

            } catch (Exception e) {
                // Si falla la captura de Stripe (ej. tarjeta caducada en esos minutos),
                // avisamos
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al procesar el cobro final: " + e.getMessage());
            }

        }).orElse(ResponseEntity.notFound().build());
    }
}