package com.parkinghole.api_parkinghole.controladores;

import com.parkinghole.api_parkinghole.modelos.Coche;

import com.parkinghole.api_parkinghole.modelos.Usuario;
import com.parkinghole.api_parkinghole.repositorio.CocheRepositorio;
import com.parkinghole.api_parkinghole.repositorio.IntercambioRepositorio;

import com.parkinghole.api_parkinghole.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coches")
public class CocheControlador {

    @Autowired
    private CocheRepositorio cocheRepository;

    @Autowired
    private UsuarioRepositorio usuarioRepository;

    @Autowired
    private IntercambioRepositorio intercambioRepository;

    // 1. OBTENER COCHES DE UN USUARIO (Para el Spinner/Desplegable)
    @GetMapping("/mis-coches")
    public ResponseEntity<?> obtenerMisCoches(@RequestParam Long uid) {
        try {
            if (uid == null) {
                return ResponseEntity.badRequest().body("El ID de usuario es obligatorio.");
            }
            List<Coche> lista = cocheRepository.findByDuenoUidAndActivoTrue(uid);
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al recuperar los vehículos: " + e.getMessage());
        }
    }

    // 2. OBTENER DETALLE DE UN COCHE (Para la pantalla de Radar/Uber)
    @GetMapping("/detalle/{id}")
    public ResponseEntity<Coche> obtenerDetalle(@PathVariable Long id) {
        // Corregido: usamos cocheRepository que es el nombre de la variable inyectada
        return cocheRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. REGISTRAR COCHE
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarCoche(@RequestBody Coche nuevoCoche) {
        try {
            if (nuevoCoche.getDueno() == null || nuevoCoche.getDueno().getUid() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Dueño no válido.");
            }

            nuevoCoche.setCid(null); // Forzamos INSERT

            Usuario duenoReal = usuarioRepository.findById(nuevoCoche.getDueno().getUid())
                    .orElse(null);

            if (duenoReal == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: Usuario no encontrado.");
            }

            long totalCoches = cocheRepository.countByDuenoUidAndActivoTrue(duenoReal.getUid());
            if (totalCoches >= 2) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Límite alcanzado: Ya tienes 2 vehículos registrados.");
            }

            nuevoCoche.setDueno(duenoReal);
            nuevoCoche.setActivo(true);

            Coche guardado = cocheRepository.save(nuevoCoche);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error crítico al registrar: " + e.getMessage());
        }
    }

    // 4. ELIMINAR COCHE (Borrado Lógico)
    @DeleteMapping("/eliminar/{cid}")
    public ResponseEntity<?> eliminarCoche(@PathVariable Long cid) {
        try {
            return cocheRepository.findById(cid).map(coche -> {

                boolean tieneIntercambiosActivos = intercambioRepository
                        .existsByIdCocheVendedorAndEstadoIntercambioIn(cid, List.of("Esperando", "Aceptado"));

                if (tieneIntercambiosActivos) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("No puedes eliminar el coche: tienes una oferta de plaza activa con él.");
                }

                cocheRepository.setActivoFalse(cid);
                return ResponseEntity.ok().body("Vehículo eliminado correctamente.");

            }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontró el coche."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al borrar.");
        }
    }
}