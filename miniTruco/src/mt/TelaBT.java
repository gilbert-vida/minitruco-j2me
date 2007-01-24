package mt;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextField;

/**
 * Decorator para o servidor e o cliente BlueTooth - adiciona form de apelido,
 * c�digo para exibi��o dos jogadores conectados, constantes e outros elementos
 * comnus aos dois lados.
 * <p>
 * Ap�s a exibi��o do form de apelido, o processo apropriado (busca de clientes
 * ou de servidores) � iniciado atrav�s do m�todo run()
 * 
 * @author Chester
 * 
 */
public abstract class TelaBT extends Canvas implements CommandListener,
		Runnable {

	protected static final Command okApelidoCommand = new Command("Ok",
			Command.SCREEN, 1);

	protected static final Command voltarCommand = new Command("Voltar",
			Command.STOP, 999);

	protected static final Command iniciarJogoCommand = new Command("Iniciar",
			Command.SCREEN, 1);
	
	protected static final char ENTER = '\n';

	/**
	 * Fonte para a mensagem de "Aguarde"
	 */
	private static final Font fonteAguarde = Font.getFont(
			Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);

	/**
	 * Fonte para os nomes dos jogadores "normais"
	 */
	private static final Font fonteNomes = Font.getFont(Font.FACE_PROPORTIONAL,
			Font.STYLE_PLAIN, Font.SIZE_SMALL);

	/**
	 * Fonte para o quadro de informa��es da sala
	 */
	private static final Font fonteInfo = Font.getFont(Font.FACE_PROPORTIONAL,
			Font.STYLE_ITALIC, Font.SIZE_SMALL);

	/**
	 * Identificador �nico Bluetooth do "servi�o miniTruco"
	 */
	public static final UUID UUID_BT = new UUID(
			"3B175368ABB411DBA508C2B155D89593", false);
	         

	protected static final String[] APELIDOS_CPU = { "CPU1", "CPU2", "CPU3" };

	/**
	 * Refer�ncia ao jogo em execu��o
	 */
	protected MiniTruco midlet;

	/**
	 * Refer�ncia � tela do jogo em execu��o
	 */
	protected Display display;

	/**
	 * Apelido do jogador (obtido do "friendly name" do celular, mas edit�vel
	 * quando ele pede pra jogar via bluetooth)
	 */
	String apelido;

	/**
	 * Campo texto do apleido do jogador
	 */
	TextField txtApelido;

	/**
	 * Permite acessar as capacidades bluetooth do celular
	 */
	LocalDevice localDevice;

	/**
	 * Apelidos dos jogadores conectados nos "slots" de 0 a 3
	 */
	protected String[] apelidos = new String[4];

	/**
	 * Regras (string de 2 caracteres T/F, indicando baralho limpo e manilha
	 * velha, nesta ordem) para o jogo a iniciar
	 */
	public String regras;

	/**
	 * Mensagem exibida quando ainda n�o temos mesa (ou null para exibir a mesa)
	 */
	private String msgDisplay = "";

	/**
	 * Define o que ser� mostrado na tela (pode ser uma mensagem ou os jogadores
	 * conectados) e a atualiza.
	 * 
	 * @param msg
	 *            Mensagem a ser exibida. Se for <code>null</code>, exibe a
	 *            mesa com os jogadores posicionados.
	 */
	protected void setStatusDisplay(String msg) {
		msgDisplay = msg;
		repaint();
		serviceRepaints();
		Thread.yield();
	}

	public TelaBT(MiniTruco midlet) {

		// Guarda o display da MIDlet (vamos precisar dele pra mostrar forms e
		// alerts) e uma refer�ncia a ela, parando a anima��o.
		this.display = Display.getDisplay(midlet);
		this.midlet = midlet;

		// Recupera o dispositivo local (que � o ponto de entrada para as
		// comunica��es bluetooth
		try {
			localDevice = LocalDevice.getLocalDevice();
		} catch (BluetoothStateException e) {
			Logger.debug(e.toString());
			midlet.alerta("Erro Bluetooth", e.getMessage());
			return;
		} catch (RuntimeException re) {
			// Esse catch � um pouco abrangente, mas � a primeira chamada a
			// classes bt, assim, se for dar algum erro bizarro, � aqui
			Logger.debug("erro runtime bt");
			Logger.debug(re.toString());
			midlet.alerta("Erro Runtime", re.getMessage());
			return;
		}

		mostraFormApelido();

	}

	public void mostraFormApelido() {
		String sugestao = apelido;
		if (sugestao == null) {
			sugestao = localDevice.getFriendlyName();
		}
		Form formApelido = new Form("Apelido");
		txtApelido = new TextField("Informe seu apelido", sugestao, 15,
				TextField.ANY);
		formApelido.append(txtApelido);
		formApelido.setCommandListener(this);
		formApelido.addCommand(okApelidoCommand);
		formApelido.addCommand(voltarCommand);
		display.setCurrent(formApelido);
	}

	public void commandAction(Command cmd, Displayable arg1) {
		if (cmd.equals(okApelidoCommand)) {
			// Confirma o apelido e come�a a procurar servidores ou aguardar
			// clientes (quem decide � a subclasse, no seu m�todo run())
			apelido = txtApelido.getString();
			this.addCommand(voltarCommand);
			display.setCurrent(this);
			(new Thread(this)).start();
		} else if (cmd.equals(voltarCommand)) {
			// Sinaliza a finaliza��o para a thread e volta ao menu
			encerraSessaoBT();
			midlet.telaBT = null;
			midlet.novaMesa(false);
			Display.getDisplay(midlet).setCurrent(midlet.mesa);
		}
	}

	/**
	 * Encerra a sess�o (cliente ou servidor), liberando quaisquer recursos que
	 * estejam em uso.
	 */
	public abstract void encerraSessaoBT();

	/**
	 * Exibe um alerta e aguarda o "ok"
	 * 
	 * @param titulo
	 *            T�tulo da janela
	 * @param texto
	 *            Texto do alerta
	 */
	public void alerta(String titulo, String texto) {
		alerta(titulo, texto, false);
	}

	/**
	 * Exibe um alerta
	 * 
	 * @param titulo
	 *            T�tulo da janela
	 * @param texto
	 *            Texto do alerta
	 * @param bloqueia
	 *            true para bloquear at� o usu�rio dar o "ok", false para exibir
	 *            e continuar rodando
	 */
	public void alerta(String titulo, String texto, boolean bloqueia) {
		Alert a = new Alert(titulo);
		a.setString(texto);
		a.setType(AlertType.INFO);
		a.setTimeout(Alert.FOREVER);
		display.setCurrent(a);
		if (bloqueia) {
			do {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// Nevermind, apenas aguardando...
				}
			} while (display.getCurrent().equals(a));
		}
	}

	/**
	 * Mostra os jogadores conectados
	 */
	protected void paint(Graphics g) {

		g.setColor(0x0000FF00);
		g.fillRect(0, 0, getWidth(), getHeight());

		if (msgDisplay == null) {

			// Nomes dos jogadores

			int topoNomes = fonteInfo.getHeight();
			int alturaNomes = getHeight() - topoNomes;

			for (int i = 0; i <= 3; i++) {

				String nome = apelidos[i];
				if (nome != null && !"".equals(nome)) {

					// Decide onde escrever
					int pos = getPosicaoMesa(i + 1);

					// Escreve
					g.setColor(0x00000000);
					g.setFont(fonteNomes);
					switch (pos) {
					case 1:
						g.drawString(nome, getWidth() / 2, getHeight(),
								Graphics.HCENTER | Graphics.BOTTOM);
						break;
					case 2:
						g.drawString(nome, getWidth() - 1, topoNomes
								+ alturaNomes / 2, Graphics.RIGHT
								| Graphics.TOP);
						break;
					case 3:
						g.drawString(nome, getWidth() / 2, topoNomes,
								Graphics.HCENTER | Graphics.TOP);
						break;
					case 4:
						g.drawString(nome, 0, topoNomes + alturaNomes / 2,
								Graphics.LEFT | Graphics.BOTTOM);
						break;
					}
				}
			}

			// Info da sala (acima dos nomes)

			g.setColor(0x00C0C0C0);
			g.fillRect(0, 0, getWidth(), topoNomes);

			// String linha1 = "SALA " + numSala;
			String linha2 = (regras.charAt(0) == 'T' ? "b.limpo / "
					: "b.sujo /")
					+ (regras.charAt(1) == 'T' ? "m.velha " : "m.nova");

			g.setColor(0x00000000);
			g.setFont(fonteInfo);
			// g.drawString(linha1, 1, 0, Graphics.TOP | Graphics.LEFT);
			g.drawString(linha2, getWidth() - 2, 0, Graphics.TOP
					| Graphics.RIGHT);

		} else {
			// Se n�o tiver nada pra mostrar, manda a mensagem de aguarde
			g.setColor(0x0000FF00);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(0x00FF0000);
			g.setFont(fonteAguarde);
			g.drawString(msgDisplay, getWidth() / 2, getHeight() / 2,
					Graphics.HCENTER | Graphics.BASELINE);
		}

	}

	/**
	 * Recupera a posi��o na mesa para o jogador conectado na posi��o i.
	 * <p>
	 * Este m�todo permite que cliente e servidor compartilhem o c�digo de
	 * desenho da tela (pois o que muda de um para outro � o "ponto de vista"
	 * mesmo - ambos t�m que ter o jogador humano na posi��o 1)
	 * 
	 * @param i
	 *            posi��o (1 a 4) na conex�o (no cliente � a posi��o na lista
	 *            recebida; no servidor � a posi��o de jogo mesmo)
	 * @return posi��o em que este jogador deve ser desenhado na mesa (e
	 *         adicionado no jogo), na mesma conven��o da classe Mesa
	 *         (1=inferior, 2=direita, 3=superior, 4=esquerda)
	 */
	public abstract int getPosicaoMesa(int i);

	/**
	 * Divide uma string com base em um separador (como o <code>split()</code>)
	 * da classe <code>String</code> do J2SE.
	 * <p>
	 * Ele efetua a opera��o em dois passos, mas esta abordagem tem a vantagem
	 * de n�o alocar nenhum objeto al�m das strings n�o-nulas do array.
	 */
	public static String[] split(String original, char separador) {
		// Fase 1: Contagem dos tokens (para dimensionar o array)
		int tamanho = original.length();
		int qtdeTokens = 1;
		for (int i = 0; i < tamanho; i++) {
			if (original.charAt(i) == separador) {
				qtdeTokens++;
			}
		}
		// Fase 2: Montagem do array
		String[] result = new String[qtdeTokens];
		int numTokenAtual = 0, inicioTokenAtual = 0;
		for (int i = 0; i <= tamanho; i++) {
			if ((i == tamanho) || (original.charAt(i) == separador)) {
				result[numTokenAtual] = original.substring(inicioTokenAtual, i);
				inicioTokenAtual = i + 1;
				numTokenAtual++;
			}
		}
		return result;

	}

}