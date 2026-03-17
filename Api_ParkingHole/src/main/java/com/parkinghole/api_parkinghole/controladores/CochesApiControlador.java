package com.parkinghole.api_parkinghole.controladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api_ext/coches")
public class CochesApiControlador {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "https://vpic.nhtsa.dot.gov/api/vehicles/";

    // 1️⃣ Listar todas las marcas
    @GetMapping("/marcas")
    public Object listarMarcas() {
        String url = BASE_URL + "getallmakes?format=json";
        return restTemplate.getForObject(url, Object.class);
    }

    // 2️⃣ Listar todos los modelos de una marca
    @GetMapping("/modelos/{marca}")
    public Object listarModelosPorMarca(@PathVariable String marca) {
        String url = BASE_URL + "GetModelsForMake/" + marca + "?format=json";
        return restTemplate.getForObject(url, Object.class);
    }
}