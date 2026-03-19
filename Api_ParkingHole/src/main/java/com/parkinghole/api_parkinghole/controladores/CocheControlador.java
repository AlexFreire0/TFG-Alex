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

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/coches")
public class CocheControlador {

    @Autowired
    private CocheRepositorio cocheRepository;

    @Autowired
    private UsuarioRepositorio usuarioRepository;

    @Autowired
    private IntercambioRepositorio intercambioRepository;

    // Helper: Obtener usuario seguro desde el Token
    private Usuario getUsuarioAutenticado(Principal principal) throws Exception {
        if (principal == null || principal.getName() == null) {
            throw new Exception("No autorizado");
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(principal.getName());
        if (usuarioOpt.isEmpty()) {
            throw new Exception("Usuario no encontrado en base de datos");
        }
        return usuarioOpt.get();
    }

    // 1. OBTENER MIS COCHES (IDOR MITIGADO: Se ignora el request param, se saca del token)
    @GetMapping("/mis-coches")
    public ResponseEntity<?> obtenerMisCoches(Principal principal) {
        try {
            Usuario usuarioSeguro = getUsuarioAutenticado(principal);
            List<Coche> lista = cocheRepository.findByDuenoUidAndActivoTrue(usuarioSeguro.getUid());
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error de autenticación: " + e.getMessage());
        }
    }

    @GetMapping("/detalle/{id}")
    public ResponseEntity<Coche> obtenerDetalle(@PathVariable Long id) {
        return cocheRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. REGISTRAR COCHE (IDOR MITIGADO)
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarCoche(@RequestBody Coche nuevoCoche, Principal principal) {
        try {
            Usuario duenoReal = getUsuarioAutenticado(principal);

            nuevoCoche.setCid(null); // Forzamos INSERT

            long totalCoches = cocheRepository.countByDuenoUidAndActivoTrue(duenoReal.getUid());
            if (totalCoches >= 2) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Límite alcanzado: Ya tienes 2 vehículos registrados.");
            }

            // Ignoramos el dueño que viene en el JSON, forzamos el dueño del Token
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

    // 4. ELIMINAR COCHE (Borrado Lógico) (IDOR MITIGADO)
    @DeleteMapping("/eliminar/{cid}")
    public ResponseEntity<?> eliminarCoche(@PathVariable Long cid, Principal principal) {
        try {
            Usuario duenoReal = getUsuarioAutenticado(principal);

            return cocheRepository.findById(cid).map(coche -> {
                // Verificar que el coche realmente pertenece al usuario que hace la petición
                if (!coche.getDueno().getUid().equals(duenoReal.getUid())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("No tienes permiso para eliminar este coche.");
                }

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