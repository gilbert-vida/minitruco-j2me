package br.inf.chester.minitruco.servidor;

/*
 * Copyright � 2006 Carlos Duarte do Nascimento (Chester)
 * cd@pobox.com
 * 
 * Este programa � um software livre; voc� pode redistribui-lo e/ou 
 * modifica-lo dentro dos termos da Licen�a P�blica Geral GNU como 
 * publicada pela Funda��o do Software Livre (FSF); na vers�o 2 da 
 * Licen�a, ou (na sua opni�o) qualquer vers�o.
 *
 * Este programa � distribuido na esperan�a que possa ser util, 
 * mas SEM NENHUMA GARANTIA; sem uma garantia implicita de ADEQUA��O
 * a qualquer MERCADO ou APLICA��O EM PARTICULAR. Veja a Licen�a
 * P�blica Geral GNU para maiores detalhes.
 *
 * Voc� deve ter recebido uma c�pia da Licen�a P�blica Geral GNU
 * junto com este programa, se n�o, escreva para a Funda��o do Software
 * Livre(FSF) Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import br.inf.chester.minitruco.cliente.EstrategiaWillian;
import br.inf.chester.minitruco.cliente.Jogador;
import br.inf.chester.minitruco.cliente.JogadorCPU;
import br.inf.chester.minitruco.cliente.Jogo;
import br.inf.chester.minitruco.cliente.JogoLocal;

/**
 * Representa uma sala, onde ocorre um jogo
 * 
 * @author Chester
 * 
 */
public class Sala {

	/**
	 * Jogadores presentes na sala
	 */
	private Jogador[] jogadores = new Jogador[4];

	/**
	 * Timestamp de entrada de cada jogador (usada para determinar o gerente)
	 */

	private Date[] timestamps = new Date[4];

	/**
	 * Adiciona um jogador na sala
	 * 
	 * @param j
	 *            Jogador a adicionar
	 * @return true se tudo correr bem, false se a sala estiver lotada ou o
	 *         jogador j� estiver em outra sala
	 */
	public boolean adiciona(JogadorConectado j) {
		// Se o jogador j� est� numa sala, n�o permite
		if (j.getSala() != null) {
			return false;
		}
		// Procura um lugarzinho na sala. Se achar, adiciona
		for (int i = 0; i <= 3; i++) {
			if (jogadores[i] == null) {
				// Link sala->jogador
				jogadores[i] = j;
				timestamps[i] = new Date();
				// Link jogador->sala
				j.numSalaAtual = this.getNumSala();
				return true;
			}
		}
		return false;
	}

	/**
	 * Recupera o gerente da sala, i.e., o <code>JogadorRemoto</code> mais
	 * antigo nela
	 * 
	 * @return Jogador mais antigo, ou null se a sala n�o tiver jogadores
	 *         remotos
	 */
	public Jogador getGerente() {
		int posGerente = -1;
		for (int i = 0; i <= 3; i++) {
			if (jogadores[i] instanceof JogadorConectado) {
				if (posGerente == -1
						|| timestamps[i].before(timestamps[posGerente])) {
					posGerente = i;
				}
			}
		}
		if (posGerente != -1) {
			return jogadores[posGerente];
		} else {
			return null;
		}
	}

	/**
	 * Conta quantas pessoas tem na sala
	 * 
	 * @return N�mero de Pessoas
	 */
	public int getNumPessoas() {
		int numPessoas = 0;
		for (int i = 0; i <= 3; i++) {
			if (jogadores[i] != null) {
				numPessoas++;
			}
		}
		return numPessoas;
	}

	/**
	 * Remove um jogador da sala.
	 * <p>
	 * Se houver um jogo em andamento, interrompe o mesmo.
	 * 
	 * @param j
	 *            Jogador a remover
	 * @return true se removeu, false se ele n�o estava l�
	 */
	public boolean remove(JogadorConectado j) {
		for (int i = 0; i <= 3; i++) {
			if (jogadores[i] == j) {
				// Finaliza jogo em andamento, se houver.
				if (jogo != null) {
					jogo.abortaJogo(j);
					jogo = null;
				}
				// Desfaz link sala->jogador
				jogadores[i] = null;
				// Desfaz link jogador->sala
				j.numSalaAtual = 0;
				return true;
			}
		}
		return false;
	}

	/**
	 * Jogo que est� rodando nessa sala (se houver)
	 */
	private JogoLocal jogo = null;

	/**
	 * Regra de baralho para jogos iniciados nesta sala
	 */
	boolean baralhoLimpo = false;

	/**
	 * Regra de manilha para jogos iniciados nesta sala
	 */
	boolean manilhaVelha = false;

	/**
	 * Recupera o jogo que est� rolando na sala (para dar comandos, etc.)
	 */
	public Jogo getJogo() {
		return jogo;
	}

	/**
	 * Salas de jogo
	 */
	private static List<Sala> salas;

	/**
	 * Envia uma notifica��o para todos os jogadores na sala
	 * 
	 * @param mensagem
	 *            linha de texto a ser enviada
	 */
	public void notificaJogadores(String mensagem) {
		for (int i = 0; i <= 3; i++) {
			if (jogadores[i] instanceof JogadorConectado) {
				((JogadorConectado) jogadores[i]).println(mensagem);
			}
		}
	}

	/**
	 * Verifica se a mesa est� completa, i.e., se a sala tem 4 jogadores
	 * dispostos a jogar, e se j� n�o tem um jogo rolando.
	 * <p>
	 * Se isto acontecer, inicia a partida.
	 */
	public void verificaMesaCompleta() {
		// Se estamos em jogo, desencana
		if (jogo != null) {
			return;
		}
		// Todos os remotos conectados t�m que querer jogar
		for (int i = 0; i <= 3; i++) {
			if ((jogadores[i] instanceof JogadorConectado)
					&& !((JogadorConectado) jogadores[i]).querJogar) {
				return;
			}
		}
		// Tem que ter pelo menos dois remotos na sala
		int numRemotos = 0;
		for (int i = 0; i <= 3; i++) {
			if (jogadores[i] instanceof JogadorConectado) {
				numRemotos++;
			}
		}
		if (numRemotos < 2) {
			return;
		}
		// Completa as posi��es vazias com bots
		int n = 1;
		for (int i = 0; i <= 3; i++) {
			if (jogadores[i] == null) {
				jogadores[i] = new JogadorCPU(new EstrategiaWillian());
				jogadores[i].setNome("[ROBO_" + (n++) + "]");
			}
		}
		// Cria o jogo com as regras selecionadas, adiciona os jogadores na
		// ordem e inicia
		jogo = new JogoLocal(baralhoLimpo, manilhaVelha);
		for (int i = 0; i <= 3; i++) {
			jogo.adiciona(jogadores[i]);
			if (jogadores[i] instanceof JogadorConectado) {
				((JogadorConectado) jogadores[i]).jogando = true;
			}
		}
		Thread t = new Thread(jogo);
		t.start();
	}

	/**
	 * Inicializa as salas de jogo a disponibilizar
	 * 
	 * @param numSalas
	 *            Quantidade de salas no servidor
	 */
	public static void inicializaSalas(int numSalas) {
		salas = new ArrayList<Sala>(numSalas);
		for (int i = 0; i < numSalas; i++) {
			salas.add(i, new Sala());
		}
	}

	/**
	 * Recupera uma sala de jogo.
	 * 
	 * @param numSala
	 *            Numero da sala (de 1 at� <code>getQtdeSalas(</code>)
	 * @return sala correspondente ao n�mero. Se as salas n�o tiverem sido
	 *         inicializadas, ou se o n�mero for inv�lido, retorna
	 *         <code>null</code>
	 */
	public static Sala getSala(int numSala) {
		if (salas == null || numSala < 1 || numSala > salas.size()) {
			return null;
		} else {
			return salas.get(numSala - 1);
		}
	}

	/**
	 * @return quantidade de salas dispon�veis no servidor
	 */
	public static int getQtdeSalas() {
		return (salas == null ? 0 : salas.size());
	}

	/**
	 * Recupera o n�mero da sala
	 * 
	 * @return N�mero de 1 a <code>getQtdeSalas()</code>
	 */
	public int getNumSala() {
		return salas.indexOf(this) + 1;
	}

	/**
	 * Retorna a posi��o do jogador na sala
	 * 
	 * @param j
	 *            Jogador consultado
	 * @return posi��o de 1 a 4, ou 0 se o jogador n�o est� na sala
	 */
	public int getPosicao(Jogador j) {
		for (int i = 0; i <= 3; i++) {
			if (jogadores[i] == j) {
				return i + 1;
			}
		}
		return 0;
	}

	/**
	 * Recupera o jogador em uma determinada posi��o
	 * 
	 * @param i
	 *            posi��o do jogador (de 1 a 4)
	 * @return objeto que representa o jogador, ou null se a posi��o for
	 *         inv�lida ou n�o estiver ocupada
	 */
	public Jogador getJogador(int i) {
		if (i >= 1 && i <= 4)
			return jogadores[i - 1];
		else
			return null;
	}

	/**
	 * Desvincula o jogo da sala, eliminado eventuais bots
	 * 
	 */
	public synchronized void liberaJogo() {
		for (int i = 0; i <= 3; i++) {
			if (jogadores[i] instanceof JogadorCPU) {
				jogadores[i] = null;
			}
		}
		this.jogo = null;
	}

	/**
	 * Recupera a string de informa��o da sala.
	 * 
	 * @return String no formato "I sala nome1|nome2|nome3|nome4 vontade posicao
	 *         regras"
	 */
	public String getInfo() {
		StringBuilder sb = new StringBuilder();
		// I numsala
		sb.append("I " + getNumSala());
		// Nomes dos jogadores, separados por pipe (posi��es vazias s�o strings
		// vazias)
		for (int i = 0; i <= 3; i++) {
			sb.append(i == 0 ? ' ' : '|');
			sb.append(jogadores[i] == null ? "" : jogadores[i].getNome());
		}
		sb.append(' ');
		// Status de "quer jogar" dos jogadores (posi��es vazias s�o T,
		// indicando que ser�o preenchidas por rob�s caso o jogo inicie)
		for (int i = 0; i <= 3; i++) {
			if (jogadores[i] instanceof JogadorConectado) {
				if (((JogadorConectado) jogadores[i]).querJogar) {
					sb.append('T');
				} else {
					sb.append('F');
				}
			} else {
				sb.append('T');
			}
		}
		sb.append(' ');
		// Posi��o do gerente
		sb.append(getPosicao(getGerente()));
		sb.append(' ');
		// Regras
		sb.append((baralhoLimpo ? 'T' : 'F'));
		sb.append((manilhaVelha ? 'T' : 'F'));
		return sb.toString();
	}

	/**
	 * Troca o parceiro do gerente da sala (fazendo um rod�zio de todo mundo
	 * menos o gerente)
	 * 
	 */
	public void trocaParceiroDoGerente() {

		Jogador gerente = getGerente();

		// Cria uma lista das posi��es a trocar, duplicando a primeira no final
		List<Integer> posicoes = new ArrayList<Integer>();
		int posGerente = 0;
		for (int i = 1; i <= 4; i++) {
			if (!gerente.equals(this.getJogador(i))) {
				posicoes.add(i);
			} else {
				posGerente = i;
			}
		}
		posicoes.add(posicoes.get(0));

		// Cria novos arrays de jogadores/timestamps, rotacionando as posi��es
		// com base na lista acima (jogando o pr�ximo da lista no atual)
		Jogador[] novosJogadores = new Jogador[4];
		Date[] novosTimestamps = new Date[4];
		for (int i = 0; i <= 2; i++) {
			novosJogadores[posicoes.get(i) - 1] = getJogador(posicoes
					.get(i + 1));
			novosTimestamps[posicoes.get(i) - 1] = timestamps[posicoes
					.get(i + 1) - 1];
		}

		// Complementa a lista com o gerente e troca a lista atual por essa
		novosJogadores[posGerente - 1] = gerente;
		novosTimestamps[posGerente - 1] = timestamps[posGerente - 1];
		jogadores = novosJogadores;
		timestamps = novosTimestamps;

	}

	public void inverteAdversariosDoGerente() {

		// Acha o gerente
		Jogador gerente = getGerente();
		int posGerente = 0;
		for (int i = 0; i <= 3; i++) {
			if (!gerente.equals(jogadores[i])) {
				posGerente = i;
			}
		}
		// Acha as posi��es dos advers�rios
		int posAdv1 = posGerente + 1;
		int posAdv2 = posGerente + 3;
		if (posAdv1 > 4)
			posAdv1 -= 4;
		if (posAdv2 > 4)
			posAdv2 -= 4;

		// Troca jogadores e timestamps
		posAdv1--;
		posAdv2--;

		Jogador tempJogador = jogadores[posAdv1];
		jogadores[posAdv1] = jogadores[posAdv2];
		jogadores[posAdv2] = tempJogador;

		Date tempTimestamp = timestamps[posAdv1];
		timestamps[posAdv1] = timestamps[posAdv2];
		timestamps[posAdv2] = tempTimestamp;

	}

}
