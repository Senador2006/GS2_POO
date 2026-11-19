package br.com.fiap.territory.persistencia;

import br.com.fiap.territory.dominio.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acesso à tabela ALERTA. */
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    /** Alertas já emitidos, do mais recente para o mais antigo. */
    List<Alerta> findAllByOrderByDataHoraDesc();
}
