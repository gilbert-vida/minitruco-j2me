package br.inf.chester.minitruco.cliente;

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

/**
 * Faz o log do jogo, para fins de debug.
 * <p>
 * Foi separada da classe original, para que o servidor n�o tivesse depend�ncias
 * de J2ME
 * @author chester
 *
 */
public class Logger {

	/**
	 * M�todo usado para debug (permite acompanhar o jogo no console)
	 * 
	 * @param string
	 *            Mensagem informativa
	 */
	public static void debug(String string) {
		// Descomentar para acompanhar o jogo no console
		//System.out.println(string);
		Thread.yield(); // Bugfix para Nokia 6600 e ME4SE
	}

}