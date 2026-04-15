package com.parkinghole.api_parkinghole.repositorio;

import com.parkinghole.api_parkinghole.modelos.Intercambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntercambioRepositorio extends JpaRepository<Intercambio, Long> {

    List<Intercambio> findByEstadoIntercambio(String estado);
    boolean existsByIdCocheVendedorAndEstadoIntercambioIn(Long idCocheVendedor, List<String> estados);

    @Query("SELECT i FROM Intercambio i WHERE i.estadoIntercambio = :estado " +
           "AND (:idUsuario IS NULL OR i.idVendedor != :idUsuario) " +
           "AND i.momentoIntercambioPrevisto > CURRENT_TIMESTAMP")
    List<Intercambio> findDisponiblesFiltrados(@Param("estado") String estado, @Param("idUsuario") Long idUsuarioConsulta);

    // NUEVO: Para saber qué plazas ha publicado un usuario (Vendedor)
    List<Intercambio> findByIdVendedorOrderByCreatedAtDesc(Long idVendedor);

    // NUEVO: Para saber qué plazas ha reservado un usuario (Comprador)
    List<Intercambio> findByIdCompradorOrderByCreatedAtDesc(Long idComprador);
}