package io.github.contractautomataproject.tictactoe;

import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.contractautomataproject.tictactoe.symbols.Circle;
import io.github.contractautomataproject.tictactoe.symbols.Cross;
import io.github.contractautomataproject.tictactoe.symbols.Symbol;

public class App 
{
	private static Random generator = new Random();
	
	public static void main( String[] args )
	{
		System.out.println( "Tic-tac-toe!" );

		Matrix m = new Matrix();

		m.printInformation();
		
		Symbol player;

		try (Scanner scan = new Scanner(System.in);)
		{

			System.out.println("Type ok if you want to start first, or type anything else otherwise.");

			if (scan.nextLine().equals("ok"))
				player = new Cross();
			else
				player = new Circle();

			while (!m.win()) {
				if (player instanceof Cross) {
					insert(player,m,scan);
					m.set(new Circle(), randomChoice(m));
				}
				else {
					m.set(new Cross(), randomChoice(m));
					System.out.println(m.toString());
					insert(player,m,scan);					
				}
				System.out.println(m.toString());
			}
			
			if (player.getClass().equals(m.whoHasWon().getClass()))
					System.out.println("Congratulations, you win!");
			else
				System.out.println("Game over!");
		}
	}

	private static void insert(Symbol symbol, Matrix m, Scanner scan) {

		Integer pos;
		do {
			System.out.println("Insert a valid position");
			pos = scan.nextInt();
		} while(pos<0 || pos>8 || (pos>=0 && pos<=8 && !m.isAvailable(pos)));
		m.set(symbol, pos);

	}
	
	private static Integer randomChoice(Matrix m) {
		List<Integer> choices = IntStream.range(0,9)
		.filter(i->m.isAvailable(i))
		.mapToObj(i->i)
		.collect(Collectors.toList());
		
		return choices.get(generator.nextInt(choices.size()));
	}

}
