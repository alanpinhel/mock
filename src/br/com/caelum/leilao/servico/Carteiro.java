package br.com.caelum.leilao.servico;

import br.com.caelum.leilao.dominio.Leilao;

public interface Carteiro {
	void envia(Leilao leilao);
}