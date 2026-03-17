package com.parkinghole.api_parkinghole.controladores;

import com.parkinghole.api_parkinghole.modelos.PagoResponse;
import com.parkinghole.api_parkinghole.modelos.Usuario;
import com.parkinghole.api_parkinghole.repositorio.UsuarioRepositorio;
import com.stripe.Stripe;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.model.PaymentIntent;
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

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @PostMapping("/crear-payment-sheet")
    public ResponseEntity<?> crearPaymentSheet(@RequestBody Map<String, Object> data) {
        try {
            // 1. Recibir datos base
            Double precioVendedor = Double.valueOf(data.get("precioTotal").toString());
            Long idComprador = Long.valueOf(data.get("idUsuario").toString());
            Long idVendedor = Long.valueOf(data.get("idVendedor").toString());

            // 2. Buscar usuarios
            Usuario comprador = usuarioRepositorio.findById(idComprador)
                    .orElseThrow(() -> new Exception("Comprador no encontrado"));
            Usuario vendedor = usuarioRepositorio.findById(idVendedor)
                    .orElseThrow(() -> new Exception("Vendedor no encontrado"));

            // 3. --- LÓGICA DE RENTABILIDAD (SUMAR COMISIÓN) ---
            // Queremos que el vendedor reciba su precio íntegro.
            // Sumamos tu 15% + 0.35€ fijos para cubrir la comisión de Stripe.
            Double comisionApp = precioVendedor * 0.15;
            Double precioFinalComprador = precioVendedor + comisionApp + 0.35;

            Long montoTotalCentimos = Math.round(precioFinalComprador * 100);
            Long comisionTotalCentimos = Math.round((comisionApp + 0.35) * 100);

            // 4. Gestionar Customer (Comprador)
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

            // 5. Crear Ephemeral Key
            EphemeralKeyCreateParams ephemeralKeyParams = EphemeralKeyCreateParams.builder()
                    .setStripeVersion("2023-10-16")
                    .setCustomer(stripeCustomerId)
                    .build();
            EphemeralKey ephemeralKey = EphemeralKey.create(ephemeralKeyParams);

            // 6. Configurar el Pago con Captura Manual (Escrow)
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(montoTotalCentimos)
                    .setCurrency("eur")
                    .setCustomer(stripeCustomerId)
                    // RETENCIÓN: El dinero se bloquea pero no se cobra aún
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );

            // Configurar reparto si el vendedor tiene cuenta Connect
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

            // Enlace para el Onboarding (Webview en Android)
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

    // Método para capturar el dinero cuando el PIN sea correcto
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