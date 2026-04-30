package com.parkinghole.api_parkinghole.servicios;

import com.parkinghole.api_parkinghole.modelos.Usuario;
import com.parkinghole.api_parkinghole.repositorio.UsuarioRepositorio;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
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

        // 2. Generamos el enlace para el onboarding
        AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setRefreshUrl("https://parkinghole.com/reintentar")
                .setReturnUrl("https://parkinghole.com/exito")
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

        AccountLink accountLink = AccountLink.create(linkParams);
        return accountLink.getUrl();
    }
}
