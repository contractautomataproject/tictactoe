package io.github.contractautomataproject.tictactoe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.automaton.transition.Transition;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomataproject.tictactoe.grid.Grid;
import io.github.contractautomataproject.tictactoe.symbols.Circle;
import io.github.contractautomataproject.tictactoe.symbols.Cross;
import io.github.contractautomataproject.tictactoe.symbols.Symbol;

public class App {
	private final static String dir = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" +
			File.separator+"resources"+File.separator;

	private static Random generator = new Random();
	private static Grid m;
	private static Symbol player;
	private static Symbol opponent;
	private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategyX=null;
	private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategyO=null;
	private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategy;


	public static void main(String[] args) throws IOException {
//		m = new Grid("O_2 X_6 X_7 O_8");
//		AutDataConverter<CALabel> aadc = new AutDataConverter<>(CALabel::new);
//		opponent = new Cross();
//		strategy = aadc.importMSCA(dir + "strategy"+opponent.getSymbol()+".data");
//		insertOpponent(true);


		boolean guided = false;
		System.out.println("Tic-tac-toe!");
		try (Scanner scan = new Scanner(System.in);) {
			System.out.println("Type ok if you want your opponent to be guided, anything else otherwise.");
			System.out.println("Warning: winning against the guided opponent is impossible!");
			if (scan.nextLine().equals("ok"))
				guided = true;

			while (true) {
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
					AutDataConverter<CALabel> adc = new AutDataConverter<>(CALabel::new);
					if (opponent instanceof Circle) {
						if (strategyO == null)
							strategyO = adc.importMSCA(dir + "strategy" + opponent.getSymbol() + ".data");
						strategy = strategyO;
					} else {
						if (strategyX == null)
							strategyX = adc.importMSCA(dir + "strategy" + opponent.getSymbol() + ".data");
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


				System.out.println("Type anything to start a new game, type quit to terminate. ");

				if (scan.next().equals("quit"))
					return;
			}
		}
	}

	private static void insertPlayer(Scanner scan) {
		Integer pos;
		do {
			System.out.println("Insert a valid position");
			pos = scan.nextInt();
		} while (pos < 0 || pos > 8 || (pos >= 0 && pos <= 8 && !m.isAvailable(pos)));
		m.set(player, pos);

	}

	private static void insertOpponent(boolean guided) {
		if (guided) {
			String conf = m.toStringLine();
			List<ModalTransition<String,Action,State<String>,CALabel>> choices = new ArrayList<>(strategy.getForwardStar(strategy.getTransition().parallelStream()
					.filter(t->t.getSource().toString().contains(", "+conf))
					.map(Transition::getSource)
					.findAny().orElseThrow(UnsupportedOperationException::new)));

			int move = generator.nextInt(choices.size());
			//System.out.println("Selected move "+move+" over "+choices.size());
			m.set(opponent, Integer.parseInt(choices.get(move).getLabel().getAction().getLabel().split("_")[1]));
		}
		else {
			List<Integer> choices = IntStream.range(0, 9)
					.filter(i -> m.isAvailable(i))
					.mapToObj(i -> i)
					.collect(Collectors.toList());
			m.set(opponent, choices.get(generator.nextInt(choices.size())));
		}
	}

}
