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
import java.util.Optional;
import java.security.Principal;
import com.parkinghole.api_parkinghole.modelos.Usuario;
import com.parkinghole.api_parkinghole.repositorio.UsuarioRepositorio;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/intercambios")
public class IntercambioControlador {

    private static final Logger logger = LoggerFactory.getLogger(IntercambioControlador.class);

    @Autowired
    private IntercambioRepositorio repository;

    @Autowired
    private UsuarioRepositorio usuarioRepository;

    private Usuario getUsuarioAutenticado(Principal principal) throws Exception {
        if (principal == null || principal.getName() == null) {
            throw new Exception("No autorizado");
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(principal.getName());
        if (usuarioOpt.isEmpty()) {
            throw new Exception("Usuario no encontrado en base de datos");
        }
        return usuarioOpt.get();
    }

    public static class CalificacionRequest {
        public Integer estrellas;
        public String observaciones;
    }

    @GetMapping("/disponibles")
    public List<Intercambio> obtenerDisponibles(@RequestParam(required = false) Long idUsuarioConsulta) {
        repository.caducarPlazasPasadasDeTiempo();
        return repository.findDisponiblesFiltrados("Esperando", idUsuarioConsulta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Intercambio> obtenerIntercambioPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/ofrecer")
    public ResponseEntity<?> ofrecerPlaza(@RequestBody Intercambio nuevoIntercambio) {
        try {
            // --- LÓGICA DE TRANSPARENCIA DE COMISIONES ---
            Double ganancia = nuevoIntercambio.getGananciaVendedor();
            if (ganancia == null || ganancia < 0) ganancia = 0.0;
            
            Double comision = (ganancia * 0.15) + 0.35;
            Double precioTotal = ganancia + comision;

            // Redondeo a 2 decimales para evitar problemas de coma flotante
            comision = Math.round(comision * 100.0) / 100.0;
            precioTotal = Math.round(precioTotal * 100.0) / 100.0;

            nuevoIntercambio.setGananciaVendedor(ganancia);
            nuevoIntercambio.setComisionServicio(comision);
            nuevoIntercambio.setPrecioTotalComprador(precioTotal);
            // ---------------------------------------------

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

    @Transactional
    @PostMapping("/reservar/{id}")
    public ResponseEntity<?> reservarPlaza(
            @PathVariable Long id,
            @RequestParam Long idComprador,
            @RequestParam Long idCocheComprador,
            @RequestParam String paymentIntentId // <--- NUEVO: Recibimos el ID del pago retenido
    ) {
        logger.info("Recibida petición de reserva. ID Plaza: {}, PaymentIntent: {}", id, paymentIntentId);
        try {
            return repository.findById(id).map(intercambio -> {
                if (!"Esperando".equals(intercambio.getEstadoIntercambio())) {
                    logger.warn("Plaza {} ya no está disponible (Estado: {}). Abortando y cancelando Stripe.", id, intercambio.getEstadoIntercambio());
                    cancelarPagoStripe(paymentIntentId);
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
                logger.info("Reserva completada con éxito en BD para la plaza {}.", id);
                return ResponseEntity.ok(intercambio);
            }).orElseGet(() -> {
                logger.warn("Plaza {} no encontrada en BD. Cancelando pago de Stripe.", id);
                cancelarPagoStripe(paymentIntentId);
                return ResponseEntity.notFound().build();
            });
        } catch (Exception e) {
            logger.error("Error crítico durante la reserva de la plaza {}. Intentando cancelar pago.", id, e);
            cancelarPagoStripe(paymentIntentId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar en base de datos. Retención cancelada automáticamente.");
        }
    }

    private void cancelarPagoStripe(String paymentIntentId) {
        try {
            String cleanId = paymentIntentId.contains("_secret_") ? paymentIntentId.split("_secret_")[0] : paymentIntentId;
            logger.info("Solicitando cancelación de retención a Stripe para PaymentIntent: {}", cleanId);
            PaymentIntent intent = PaymentIntent.retrieve(cleanId);
            if (!"canceled".equals(intent.getStatus())) {
                intent.cancel();
                logger.info("Pago {} cancelado en Stripe exitosamente.", cleanId);
            }
        } catch (Exception ex) {
            logger.error("ERROR CRÍTICO AL CANCELAR STRIPE (Posible fuga de retención): {}", ex.getMessage(), ex);
        }
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
            logger.info("Verificando PIN para intercambio {}. PaymentIntentId en DB: {}, PIN Esperado: {}, PIN Recibido: {}", 
                        id, intercambio.getPaymentIntentId(), intercambio.getCodigoVerificacion(), pinIngresado);

            // 1. Validar PIN (invertir la comprobación para evitar NPE si getCodigoVerificacion() es null)
            if (pinIngresado == null || !pinIngresado.equals(intercambio.getCodigoVerificacion())) {
                logger.warn("Validación fallida: PIN incorrecto o nulo para intercambio {}", id);
                return ResponseEntity.badRequest().body("PIN incorrecto.");
            }

            // 2. --- CAPTURAR EL PAGO EN STRIPE ---
            try {
                if (intercambio.getPaymentIntentId() != null && !intercambio.getPaymentIntentId().isEmpty()) {
                    String rawId = intercambio.getPaymentIntentId();
                    String cleanId = rawId.contains("_secret_") ? rawId.split("_secret_")[0] : rawId;
                    
                    logger.info("Iniciando capture en Stripe para paymentIntentId limpio: {}", cleanId);
                    PaymentIntent intent = PaymentIntent.retrieve(cleanId);
                    intent.capture();
                    logger.info("Capture de Stripe exitoso para intercambio {}", id);
                } else {
                    logger.warn("Intercambio {} no tiene paymentIntentId. Marcando como completado sin cobro de Stripe.", id);
                }

                // 3. Si el pago se capturó bien (o no había pago), completamos
                intercambio.setEstadoIntercambio("Completado");
                repository.save(intercambio);

                return ResponseEntity.ok(intercambio);

            } catch (Exception e) {
                logger.error("Excepción en pasarela de pago al capturar Stripe para intercambio {}", id, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error en pasarela de pago: No se pudo verificar el cobro con el banco.");
            }

        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/calificar")
    public ResponseEntity<?> calificarIntercambio(
            @PathVariable Long id,
            @RequestBody CalificacionRequest request,
            Principal principal) {
        try {
            Usuario usuarioSeguro = getUsuarioAutenticado(principal);
            Long uid = usuarioSeguro.getUid();

            return repository.findById(id).map(intercambio -> {
                if (uid.equals(intercambio.getIdVendedor())) {
                    intercambio.setCalificacionAlComprador(request.estrellas);
                    intercambio.setObservacionesDelVendedor(request.observaciones);
                } else if (uid.equals(intercambio.getIdComprador())) {
                    intercambio.setCalificacionAlVendedor(request.estrellas);
                    intercambio.setObservacionesDelComprador(request.observaciones);
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("No tienes permisos para calificar este intercambio.");
                }

                repository.save(intercambio);
                return ResponseEntity.ok(intercambio);
            }).orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error de autenticación: " + e.getMessage());
        }
    }
}