package com.parkinghole.api_parkinghole.modelos;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor // Constructor vacío obligatorio para JPA
@AllArgsConstructor // Constructor con todos los campos
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long uid;

    @Column(unique = true)
    private String googleId;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false)
    private String correo;

    // Quitamos el nullable=false porque los usuarios de Google no tienen pass
    private String contrasenaHash;

    private String rol = "usuario";

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    private LocalDateTime ultimoAcceso;

    private boolean activo = true;

    // Campos para Stripe
    // Añade esto debajo de tus otros campos
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;
    // Este es para cuando el usuario VENDE (guarda su IBAN para cobrar)
    @Column(name = "stripe_connect_id")
    private String stripeConnectId;

    @Column(name = "metodo_pago_preferido")
    private String metodoPagoPreferido;

    @Column(name = "tarjeta_brand")
    private String tarjetaBrand;

    @Column(name = "tarjeta_ultimos_cuatro", length = 4)
    private String tarjetaUltimosCuatro;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // (Recuerda generar sus getters y setters si no usas Lombok)

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
        this.activo = true; // De paso, aseguras que el usuario empiece como activo
    }
}