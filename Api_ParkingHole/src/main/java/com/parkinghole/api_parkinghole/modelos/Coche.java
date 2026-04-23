package com.parkinghole.api_parkinghole.modelos;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "coche")
@Data
public class Coche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cid;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uid_dueno", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Usuario dueno;

    private String marca;
    private String modelo;

    @Column(unique = true)
    private String matricula;

    private String color;
    private String imagenUrl;
    private boolean activo = true;

    private String capacidad;
}