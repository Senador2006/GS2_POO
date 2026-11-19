package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.EstrategiaPrevisao;
import br.com.fiap.territory.dominio.PrevisaoComparacao;
import br.com.fiap.territory.dominio.PrevisaoMediaMovel;
import br.com.fiap.territory.dominio.TipoEstrategia;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * FACTORY: único lugar que sabe dar "new" nas estratégias de previsão.
 *
 * Não há "if (tipo == ...)" nem instanceof: cada TipoEstrategia está registrado em um
 * mapa com a "receita" (Supplier) que cria o objeto. Para incluir uma estratégia nova
 * basta uma linha no construtor; o serviço e o menu não mudam.
 */
public class FabricaPrevisao {

    private final Map<TipoEstrategia, Supplier<EstrategiaPrevisao>> receitas = new EnumMap<>(TipoEstrategia.class);
    private final ConfiguracaoSistema configuracao;

    public FabricaPrevisao(ConfiguracaoSistema configuracao) {
        this.configuracao = Objects.requireNonNull(configuracao, "configuracao");
        receitas.put(TipoEstrategia.MEDIA_MOVEL, () -> new PrevisaoMediaMovel(configuracao.getJanelaMediaMovel()));
        receitas.put(TipoEstrategia.COMPARACAO, () -> new PrevisaoComparacao(configuracao.getJanelaComparacao()));
    }

    public EstrategiaPrevisao criar(TipoEstrategia tipo) {
        Supplier<EstrategiaPrevisao> receita = receitas.get(Objects.requireNonNull(tipo, "tipo"));
        if (receita == null) {
            throw new IllegalArgumentException("nenhuma estratégia registrada para " + tipo);
        }
        return receita.get();
    }

    /** Estratégia definida como padrão no ConfiguracaoSistema. */
    public EstrategiaPrevisao criarPadrao() {
        return criar(configuracao.getEstrategiaPadrao());
    }

    /** Uma instância de cada estratégia registrada (usado para comparar as previsões no menu). */
    public List<EstrategiaPrevisao> criarTodas() {
        return Arrays.stream(TipoEstrategia.values()).map(this::criar).toList();
    }
}
