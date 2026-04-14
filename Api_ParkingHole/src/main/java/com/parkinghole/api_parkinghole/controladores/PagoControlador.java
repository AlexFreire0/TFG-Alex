package com.parkinghole.api_parkinghole.controladores;

import com.parkinghole.api_parkinghole.modelos.Intercambio;
import com.parkinghole.api_parkinghole.modelos.PagoRequest;
import com.parkinghole.api_parkinghole.modelos.PagoResponse;
import com.parkinghole.api_parkinghole.modelos.Usuario;
import com.parkinghole.api_parkinghole.repositorio.IntercambioRepositorio;
import com.parkinghole.api_parkinghole.repositorio.UsuarioRepositorio;
import com.parkinghole.api_parkinghole.servicios.NotificationService;
import com.stripe.Stripe;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.EphemeralKeyCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pagos")
public class PagoControlador {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret; // ¡Añadir a application.properties!

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    
    @Autowired
    private IntercambioRepositorio intercambioRepositorio;

    @Autowired
    private NotificationService notificationService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @PostMapping("/crear-payment-sheet")
    public ResponseEntity<?> crearPaymentSheet(@RequestBody PagoRequest request) {
        try {
            Long idIntercambio = request.getIdIntercambio();
            Long idComprador = request.getIdUsuario();
            Long idVendedor = request.getIdVendedor();

            // 1. Buscar entidades reales
            Optional<Intercambio> optIntercambio = intercambioRepositorio.findById(idIntercambio);
            if (!optIntercambio.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: El intercambio solicitado no existe.");
            }
            Intercambio intercambio = optIntercambio.get();

            if (!"Esperando".equals(intercambio.getEstadoIntercambio())) {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Esta plaza ya no está disponible para reserva.");
            }

            Usuario comprador = usuarioRepositorio.findById(idComprador)
                    .orElseThrow(() -> new Exception("Comprador no encontrado"));
            Usuario vendedor = usuarioRepositorio.findById(idVendedor)
                    .orElseThrow(() -> new Exception("Vendedor no encontrado"));

            // 2. --- LÓGICA DE RENTABILIDAD SEGURA ---
            // Leemos el precio REAL de la base de datos, ¡inmune a hackeos del APK!
            Double precioVendedorReal = intercambio.getPrecioTotalComprador();
            if (precioVendedorReal == null || precioVendedorReal <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: El precio del intercambio es cero o inválido.");
            }
            Double comisionApp = precioVendedorReal * 0.15;
            Double precioFinalComprador = precioVendedorReal + comisionApp + 0.35;

            Long montoTotalCentimos = Math.round(precioFinalComprador * 100);
            Long comisionTotalCentimos = Math.round((comisionApp + 0.35) * 100);

            // 3. Gestionar Customer (Comprador)
            String stripeCustomerId = comprador.getStripeCustomerId();
            if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setEmail(comprador.getCorreo())
                        .setName(comprador.getNombre())
                        .build();
                Customer customer = Customer.create(customerParams);
                stripeCustomerId = customer.getId();
                comprador.setStripeCustomerId(stripeCustomerId);
                usuarioRepositorio.save(comprador);
            }

            // 4. Crear Ephemeral Key
            EphemeralKeyCreateParams ephemeralKeyParams = EphemeralKeyCreateParams.builder()
                    .setStripeVersion("2023-10-16")
                    .setCustomer(stripeCustomerId)
                    .build();
            EphemeralKey ephemeralKey = EphemeralKey.create(ephemeralKeyParams);

            // 5. Configurar el Pago
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(montoTotalCentimos)
                    .setCurrency("eur")
                    .setCustomer(stripeCustomerId)
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL) // Retención
                    .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
                    .putMetadata("id_intercambio", String.valueOf(idIntercambio)) // <-- VITAL PARA EL WEBHOOK
                    .putMetadata("vendedor_email", vendedor.getCorreo()) // <-- NUEVO PARA NOTIFICACIÓN PUSH
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );

            if (vendedor.getStripeConnectId() != null && !vendedor.getStripeConnectId().isEmpty()) {
                paramsBuilder.setApplicationFeeAmount(comisionTotalCentimos)
                        .setTransferData(
                                PaymentIntentCreateParams.TransferData.builder()
                                        .setDestination(vendedor.getStripeConnectId())
                                        .build()
                        );
            }

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());

            return ResponseEntity.ok(new PagoResponse(
                    paymentIntent.getClientSecret(),
                    ephemeralKey.getSecret(),
                    stripeCustomerId
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // --- NUEVO: WEBHOOK EXTREMADAMENTE SEGURO ---
    @PostMapping("/webhook")
    public ResponseEntity<String> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            // Verifica que el evento viene realmente de Stripe y no de un hacker
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            System.err.println("Error verificando webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        // Manejar el evento
        if ("payment_intent.succeeded".equals(event.getType()) || "payment_intent.amount_capturable_updated".equals(event.getType())) {
            
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            
            if (stripeObject instanceof PaymentIntent) {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                System.out.println("Pago Retenido con éxito: " + paymentIntent.getId());

                // 1. Extraemos el ID del intercambio de la Metadata que pusimos antes
                String intercambioIdStr = paymentIntent.getMetadata().get("id_intercambio");

                if (intercambioIdStr != null) {
                    try {
                        Long idIntercambio = Long.parseLong(intercambioIdStr);
                        
                        // 2. Buscamos en la DB y actualizamos a "Reservado" de forma 100% segura
                        Optional<Intercambio> optIntercambio = intercambioRepositorio.findById(idIntercambio);
                        if (optIntercambio.isPresent()) {
                            Intercambio intercambio = optIntercambio.get();
                            
                            // Solo si estaba Esperando, así evitamos dobleces de actualización
                            if ("Esperando".equals(intercambio.getEstadoIntercambio())) {
                                String pinGenerado = String.format("%04d", new java.util.Random().nextInt(10000));
                                intercambio.setCodigoVerificacion(pinGenerado);
                                intercambio.setPaymentIntentId(paymentIntent.getId());
                                intercambio.setEstadoIntercambio("Reservado");
                                intercambioRepositorio.save(intercambio);
                                System.out.println("Intercambio " + idIntercambio + " marcado como Reservado vía Webhook.");

                                // --- NUEVO: ENVIAR NOTIFICACIÓN PUSH AL VENDEDOR ---
                                String vendedorEmail = paymentIntent.getMetadata().get("vendedor_email");
                                if (vendedorEmail != null) {
                                    Optional<Usuario> vendedorOpt = usuarioRepositorio.findByCorreo(vendedorEmail);
                                    if (vendedorOpt.isPresent() && vendedorOpt.get().getFcmToken() != null) {
                                        String title = "¡Plaza Reservada! \uD83D\uDCB8";
                                        String bodyMessage = "Alguien ha pagado por tu plaza. Comprueba los detalles en la app.";
                                        notificationService.sendPushNotification(vendedorOpt.get().getFcmToken(), title, bodyMessage);
                                        System.out.println("Push notification enviada al vendedor " + vendedorEmail);
                                    }
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Webhook: ID de intercambio malformado.");
                    }
                }
            }
        }
        
        return ResponseEntity.ok("Received");
    }

    @PostMapping("/crear-cuenta-vendedor/{idUsuario}")
    public ResponseEntity<?> crearCuentaVendedor(@PathVariable Long idUsuario) {
        try {
            Usuario usuario = usuarioRepositorio.findById(idUsuario)
                    .orElseThrow(() -> new Exception("Usuario no encontrado"));

            String accountId = usuario.getStripeConnectId();

            if (accountId == null || accountId.isEmpty()) {
                AccountCreateParams accountParams = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setEmail(usuario.getCorreo())
                        .setCapabilities(
                                AccountCreateParams.Capabilities.builder()
                                        .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder().setRequested(true).build())
                                        .setTransfers(AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                                        .build()
                        )
                        .build();

                Account account = Account.create(accountParams);
                accountId = account.getId();
                usuario.setStripeConnectId(accountId);
                usuarioRepositorio.save(usuario);
            }

            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl("https://tudominio.com/reintentar")
                    .setReturnUrl("https://tudominio.com/exito")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink accountLink = AccountLink.create(linkParams);

            Map<String, String> response = new HashMap<>();
            response.put("url", accountLink.getUrl());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/capturar-pago/{paymentIntentId}")
    public ResponseEntity<?> capturarPago(@PathVariable String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent capturedIntent = paymentIntent.capture();
            return ResponseEntity.ok("Pago capturado: " + capturedIntent.getStatus());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}