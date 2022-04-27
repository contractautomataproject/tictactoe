package io.github.contractautomata.tictactoe;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.automaton.transition.Transition;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.tictactoe.grid.Grid;
import io.github.contractautomata.tictactoe.symbols.Circle;
import io.github.contractautomata.tictactoe.symbols.Cross;
import io.github.contractautomata.tictactoe.symbols.Symbol;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class App {

	private static final Random generator = new Random();
	private static Grid m;
	private static Symbol player;
	private static Symbol opponent;
	private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategyX=null;
	private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategyO=null;
	private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategy;


	public static void main(String[] args) throws IOException {
		System.out.println("Tic-tac-toe!");
		try (Scanner scan = new Scanner(System.in)) {
			while (true) {
				System.out.println("Type ok if you want your opponent to be guided, anything else otherwise.");
				System.out.println("Warning: winning against the guided opponent is impossible!");
				boolean guided=(scan.nextLine().equals("ok"));

				if (!guided)
					System.out.println("You have selected an opponent performing random moves.");

				m = new Grid();
				System.out.println("Type ok if you want to start first, or type anything else otherwise.");
				if (scan.nextLine().equals("ok")) {
					player = new Cross();
					opponent = new Circle();
				} else {
					player = new Circle();
					opponent = new Cross();
				}

				if (guided) {
					System.out.println("Loading the guided opponent...");
					if (opponent instanceof Circle) {
						if (strategyO == null)
							strategyO = loadFile();
						strategy = strategyO;
					} else {
						if (strategyX == null)
							strategyX = loadFile();
						strategy = strategyX;
					}
				}

				m.printInformation();

				Symbol turn = new Cross();
				while (!m.win() && !m.tie()) {
					if (player.getClass().equals(turn.getClass()))
						insertPlayer(scan);
					else
						insertOpponent(guided);

					if (turn instanceof Cross)
						turn = new Circle();
					else
						turn = new Cross();

					System.out.println(m.toString());
					System.out.println();
				}

				if (m.whoHasWon() == null)
					System.out.println("Draw!");
				else if (player.getClass().equals(m.whoHasWon().getClass()))
					System.out.println("Congratulations, you win!");
				else
					System.out.println("You lose!");

				scan.nextLine();

				System.out.println("Type anything to start a new game, type quit to terminate. ");


				if (scan.nextLine().equals("quit"))
					return;
			}
		}
	}

	/**
	 * used to load a strategy (as package resource)
	 */
	private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> loadFile() throws IOException {
		AutDataConverter<CALabel> adc = new AutDataConverter<>(CALabel::new);
		InputStream in = App.class.getClassLoader().getResourceAsStream("strategy" + opponent.getSymbol() + ".data");
		File f = new File("strategy" + opponent.getSymbol() + ".data");
		FileUtils.copyInputStreamToFile(in, f);
		Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>>  aut = adc.importMSCA("strategy" + opponent.getSymbol() + ".data");
		f.delete();
		return aut;
	}

	/**
	 * read the user input  and update the configuration of the game
	 */
	private static void insertPlayer(Scanner scan) {
		int pos;
		do {
			System.out.println("Insert a valid position");
			pos = scan.nextInt();
		} while (pos < 0 || pos > 8 || (pos >= 0 && pos <= 8 && !m.isAvailable(pos)));
		m.set(player, pos);

	}

	/**
	 * pick a move of the opponent, using a strategy if guided or randomly otherwise
	 */
	private static void insertOpponent(boolean guided) {
		if (guided) {
			//select outgoing transitions from the current configuration of the same
			String conf = m.toStringLine();
			List<ModalTransition<String,Action,State<String>,CALabel>> choices = new ArrayList<>(strategy.getForwardStar(strategy.getTransition().parallelStream()
					.filter(t->t.getSource().toString().contains("["+conf))
					.map(Transition::getSource)
					.findAny().orElseThrow(UnsupportedOperationException::new)));

			//if a transition has a winning target state it is picked, otherwise one of the available choices is picked randomly
			choices.stream()
					.filter(t->new Grid(t.getTarget().toString()).win(opponent.getSymbol()))
					.findAny().ifPresentOrElse(
							t->m.set(opponent, Integer.parseInt(t.getLabel().getAction().getLabel().split("_")[1])),
							()->{
								int move = generator.nextInt(choices.size());
								m.set(opponent, Integer.parseInt(choices.get(move).getLabel().getAction().getLabel().split("_")[1]));
							}
							);

		}
		else {
			//without guide a valid move is chosen randomly
			List<Integer> choices = IntStream.range(0, 9)
					.filter(i -> m.isAvailable(i))
					.boxed()
					.collect(Collectors.toList());
			m.set(opponent, choices.get(generator.nextInt(choices.size())));
		}
	}

}
