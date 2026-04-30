package com.parkinghole.api_parkinghole.servicios;

import com.parkinghole.api_parkinghole.modelos.Usuario;
import com.parkinghole.api_parkinghole.repositorio.UsuarioRepositorio;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.LoginLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.LoginLinkCreateOnAccountParams;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public String crearAccountLink(Usuario usuario) throws Exception {
        String accountId = usuario.getStripeConnectId();

        try {
            // 1. Si no tiene cuenta Express conectada, se la creamos
            if (accountId == null || accountId.isEmpty()) {
                AccountCreateParams accountParams = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setEmail(usuario.getCorreo())
                        .setCountry("ES")
                        .setCapabilities(
                                AccountCreateParams.Capabilities.builder()
                                        .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder().setRequested(true).build())
                                        .setTransfers(AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                                        .build()
                        )
                        .build();

                Account account = Account.create(accountParams);
                accountId = account.getId();
                
                // Guardamos el ID de Stripe en el usuario
                usuario.setStripeConnectId(accountId);
                usuarioRepositorio.save(usuario);
            }
        } catch (Exception e) {
            System.err.println("STRIPE ERROR CREANDO CUENTA: " + e.getMessage());
            throw e;
        }

        try {
            // 2. Generamos el enlace para el onboarding
            AccountLinkCreateParams.Type type = AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING;
            if (accountId != null && !accountId.isEmpty()) {
                Account retrievedAccount = Account.retrieve(accountId);
                if (retrievedAccount.getDetailsSubmitted() != null && retrievedAccount.getDetailsSubmitted()) {
                    type = AccountLinkCreateParams.Type.ACCOUNT_UPDATE;
                }
            }

            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl("https://tfg-alex-732367725602.europe-west1.run.app/api/pagos/stripe-reintentar")
                    .setReturnUrl("https://tfg-alex-732367725602.europe-west1.run.app/api/pagos/stripe-exito")
                    .setType(type)
                    .build();

            AccountLink accountLink = AccountLink.create(linkParams);
            return accountLink.getUrl();
        } catch (Exception e) {
            System.err.println("STRIPE ERROR: " + e.getMessage());
            throw e;
        }
    }

    public String generarLoginLink(String stripeAccountId) throws Exception {
        try {
            System.out.println("Generando link para la cuenta: " + stripeAccountId);
            LoginLink link = LoginLink.createOnAccount(
                stripeAccountId,
                LoginLinkCreateOnAccountParams.builder().build(),
                null
            );
            return link.getUrl();
        } catch (com.stripe.exception.StripeException e) {
            System.err.println("ERROR STRIPE LOGIN: " + e.getMessage());
            throw e;
        }
    }
}
