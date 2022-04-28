package io.github.contractautomata.tictactoe;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
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

public class App {

	private static State<String> currentState;
	private static final Random generator = new Random();
	private static Symbol opponent;
	private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategyX=null;
	private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategyO=null;


	public static void main(String[] args) throws IOException {
		System.out.println("Tic-tac-toe!");
		System.out.println("Warning: winning against the computer is impossible!");

		try (Scanner scan = new Scanner(System.in)) {
			while (true) {
				System.out.println("Type ok if you want to start first, or type anything else otherwise.");

				Symbol player;
				if (scan.nextLine().equals("ok")) {
					player = new Cross();
					opponent = new Circle();
				} else {
					player = new Circle();
					opponent = new Cross();
				}

				System.out.println("Loading the guided opponent...");
				Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategy;
				if (opponent instanceof Circle) {
					if (strategyO == null)
						strategyO = loadFile();
					strategy = strategyO;
				} else {
					if (strategyX == null)
						strategyX = loadFile();
					strategy = strategyX;
				}

				new Grid().printInformation();
				currentState = strategy.getInitial();
				while(currentState!=null){
					Set<ModalTransition<String,Action,State<String>,CALabel>> forwardStar = strategy.getForwardStar(currentState);
					if (check(forwardStar)) {
						currentState=null;
					}
					else {
						Symbol turn = (currentState.getState().get(9).getState().equals("TurnCross")) ?
								new Cross() : new Circle();

						if (player.getClass().equals(turn.getClass()))
							currentState = insertPlayer(scan,forwardStar);
						else
							currentState = insertOpponent(forwardStar);

						System.out.println(new Grid(currentState.toString()));
						System.out.println();
					}
				}

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
	private static State<String> insertPlayer(Scanner scan,Set<ModalTransition<String,Action,State<String>,CALabel>> forwardStar) {
		ModalTransition<String,Action,State<String>,CALabel> value;
		do{
			System.out.println("Insert a valid position (a number from 0 to 8 and the corresponding position must be free)");
			int pos = scan.nextInt();
			value=forwardStar.stream()
					.filter(t->t.getLabel().getAction().getLabel().contains(pos+""))
					.findAny().orElse(null);

		} while (value==null);
		return value.getTarget();

	}

	/**
	 * pick a move of the opponent, using a strategy if guided or randomly otherwise
	 */
	private static State<String> insertOpponent(Set<ModalTransition<String,Action,State<String>,CALabel>> forwardStar) {
		//if a transition has a winning target state it is picked, otherwise one of the available choices is picked randomly
		return forwardStar.stream()
				.filter(t->new Grid(t.getTarget().toString()).win(opponent.getSymbol()))
				.findAny()
				.orElse(new ArrayList<>(forwardStar).get(generator.nextInt(forwardStar.size())))
				.getTarget();
	}

	private static boolean check(Set<ModalTransition<String,Action,State<String>,CALabel>> forwardStar) {
		if (forwardStar.stream()
				.anyMatch(t->t.getLabel().getAction().getLabel().contains("tie"))) {
			System.out.println("Draw!");
			return true;
		}
		else if (forwardStar.stream()
				.anyMatch(t->t.getLabel().getAction().getLabel().contains("win"))) {
			System.out.println("You lose!");
			return true;
		}
		return false;
	}

}
