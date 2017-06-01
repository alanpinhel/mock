package br.com.caelum.leilao.servico;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Arrays;

import org.junit.Test;
import org.mockito.InOrder;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;

public class EncerradorDeLeilaoTest {

	@Test
	public void encerraLeiloesQueComecaramHaMaisDeUmaSemana() {

		Calendar antiga = Calendar.getInstance();
		antiga.set(2017, 01, 01);

		Leilao leilao1 = new CriadorDeLeilao().para("Macbook Air").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Macbook Pro").naData(antiga).constroi();
		List<Leilao> leiloes = asList(leilao1, leilao2);

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);

		when(dao.correntes()).thenReturn(leiloes);

		Carteiro carteiro = mock(Carteiro.class);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();

		assertEquals(2, encerrador.getTotalEncerrados());
		assertTrue(leilao1.isEncerrado());
		assertTrue(leilao2.isEncerrado());
	}

	@Test
	public void naoDeveEncerrarLeiloesQueComecaramOntem() {

		Calendar ontem = Calendar.getInstance();
		ontem.add(Calendar.DAY_OF_MONTH, -1);

		Leilao leilao = new CriadorDeLeilao().para("Macbook Air").naData(ontem).constroi();

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);

		when(dao.correntes()).thenReturn(asList(leilao));

		Carteiro carteiro = mock(Carteiro.class);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
		assertFalse(leilao.isEncerrado());

		verify(dao, never()).atualiza(leilao);
		verify(carteiro, never()).envia(leilao);
	}

	@Test
	public void naoDeveEncerrarLeiloesCasoNaoHajaNenhum() {

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);

		when(dao.correntes()).thenReturn(new ArrayList<Leilao>());

		Carteiro carteiro = mock(Carteiro.class);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
	}

	@Test
	public void deveAtualizarLeiloesEncerrados() {

		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao = new CriadorDeLeilao().para("Macbook Air").naData(antiga).constroi();

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);

		when(dao.correntes()).thenReturn(asList(leilao));

		Carteiro carteiro = mock(Carteiro.class);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();

		InOrder inOrder = inOrder(dao, carteiro);

		inOrder.verify(dao, times(1)).atualiza(leilao);
		inOrder.verify(carteiro, times(1)).envia(leilao);
	}

	@Test
	public void deveContinuarAExecucaoMesmoQuandoDaoFalha() {
		
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao1);

		Carteiro carteiroFalso = mock(Carteiro.class);
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);

		encerrador.encerra();

		verify(daoFalso).atualiza(leilao2);
		verify(carteiroFalso).envia(leilao2);
	}
	
	@Test
	public void deveContinuarAExecucaoMesmoQuandoEnvioDeEmailFalhar() {
		
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		Carteiro carteiro = mock(Carteiro.class);
		doThrow(new RuntimeException()).when(carteiro).envia(leilao1);
		
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);

		encerrador.encerra();

		verify(dao).atualiza(leilao2);
		verify(carteiro).envia(leilao2);
	}
	
	@Test
	public void naoDeveInvocarNenhumaVezOCarteiro() {
		
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

		RepositorioDeLeiloes dao = mock(RepositorioDeLeiloes.class);
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));		
		doThrow(new RuntimeException()).when(dao).atualiza(any(Leilao.class));
		
		Carteiro carteiro = mock(Carteiro.class);
		
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(dao, carteiro);
		encerrador.encerra();
		
		verify(carteiro, never()).envia(any(Leilao.class));
	}
}