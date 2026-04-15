package com.parkinghole.api_parkinghole.modelos;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "intercambio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Intercambio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_vendedor", nullable = false)
    private Long idVendedor;

    @Column(name = "id_comprador")
    private Long idComprador;

    @Column(name = "id_coche_vendedor", nullable = false)
    private Long idCocheVendedor;

    @Column(name = "id_coche_comprador")
    private Long idCocheComprador;

    // --- ECONOMÍA ---
    @Column(name = "precio_total_comprador", nullable = false)
    private Double precioTotalComprador;

    @Column(name = "comision_servicio", nullable = false)
    private Double comisionServicio;

    @Column(name = "ganancia_vendedor", nullable = false)
    private Double gananciaVendedor;

    // --- LOGÍSTICA ---
    @Column(name = "momento_intercambio_previsto", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // <--- ESTO ES LA CLAVE
    private LocalDateTime momentoIntercambioPrevisto;

    @Column(name = "cortesia_minutos")
    private Integer cortesiaMinutos = 5;

    @Column(name = "plaza_lat", nullable = false)
    private Double plazaLat;

    @Column(name = "plaza_long", nullable = false)
    private Double plazaLong;

    @Column(name = "plaza_direccion_texto")
    private String plazaDireccionTexto;

    @Column(name = "capacidad")
    private String capacidad;

    // --- ESTADOS ---
    @Column(name = "estado_intercambio")
    private String estadoIntercambio = "Esperando";

    @Column(name = "estado_resultado")
    private String estadoResultado;

    @Column(name = "codigo_verificacion", length = 4)
    private String codigoVerificacion;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "calificacion_al_vendedor")
    private Integer calificacionAlVendedor;

    @Column(name = "calificacion_al_comprador")
    private Integer calificacionAlComprador;

    @Column(name = "observaciones_del_comprador")
    private String observacionesDelComprador;

    @Column(name = "observaciones_del_vendedor")
    private String observacionesDelVendedor;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}