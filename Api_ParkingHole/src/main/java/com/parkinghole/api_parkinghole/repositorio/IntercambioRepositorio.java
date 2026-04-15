package com.parkinghole.api_parkinghole.repositorio;

import com.parkinghole.api_parkinghole.modelos.Intercambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntercambioRepositorio extends JpaRepository<Intercambio, Long> {

    List<Intercambio> findByEstadoIntercambio(String estado);
    boolean existsByIdCocheVendedorAndEstadoIntercambioIn(Long idCocheVendedor, List<String> estados);

    @Modifying
    @Transactional
    @Query(value = "UPDATE intercambio SET estado_intercambio = 'Caducado' " +
           "WHERE (momento_intercambio_previsto + (cortesia_minutos * interval '1 minute')) < CURRENT_TIMESTAMP " +
           "AND estado_intercambio = 'Esperando'", nativeQuery = true)
    int caducarPlazasPasadasDeTiempo();

    @Query(value = "SELECT * FROM intercambio i WHERE (i.momento_intercambio_previsto + (i.cortesia_minutos * interval '1 minute')) > CURRENT_TIMESTAMP " +
           "AND i.estado_intercambio = :estado " +
           "AND (:idUsuario IS NULL OR i.id_vendedor != :idUsuario)", nativeQuery = true)
    List<Intercambio> findDisponiblesFiltrados(@Param("estado") String estado, @Param("idUsuario") Long idUsuarioConsulta);

    // NUEVO: Para saber qué plazas ha publicado un usuario (Vendedor)
    List<Intercambio> findByIdVendedorOrderByCreatedAtDesc(Long idVendedor);

    // NUEVO: Para saber qué plazas ha reservado un usuario (Comprador)
    List<Intercambio> findByIdCompradorOrderByCreatedAtDesc(Long idComprador);
}