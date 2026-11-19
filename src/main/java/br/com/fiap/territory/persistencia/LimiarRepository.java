package br.com.fiap.territory.persistencia;

import br.com.fiap.territory.dominio.LimiarSalvo;
import br.com.fiap.territory.dominio.TipoSensor;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela LIMIAR (limiares alterados pelo menu). A chave é o tipo de sensor. */
public interface LimiarRepository extends JpaRepository<LimiarSalvo, TipoSensor> {
}
