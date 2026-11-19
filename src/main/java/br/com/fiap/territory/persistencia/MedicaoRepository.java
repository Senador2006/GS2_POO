package br.com.fiap.territory.persistencia;

import br.com.fiap.territory.dominio.Medicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acesso à tabela MEDICAO. O Spring Data gera o SQL a partir do nome do método
 * (sempre com parâmetros, nunca por concatenação de String).
 */
public interface MedicaoRepository extends JpaRepository<Medicao, Long> {

    /** Histórico de um sensor em ordem cronológica (mais antiga primeiro). */
    List<Medicao> findByCodigoSensorOrderByDataHoraAscIdAsc(String codigoSensor);

    long countByCodigoSensor(String codigoSensor);
}
