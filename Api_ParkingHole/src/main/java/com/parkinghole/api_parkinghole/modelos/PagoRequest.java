package com.parkinghole.api_parkinghole.modelos;

import lombok.Data;

@Data
public class PagoRequest {
    private Long idIntercambio;
    private Long idUsuario;
    private Long idVendedor;
}
