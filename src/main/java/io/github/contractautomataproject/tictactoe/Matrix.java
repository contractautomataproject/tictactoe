package io.github.contractautomataproject.tictactoe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.github.contractautomataproject.tictactoe.symbols.Circle;
import io.github.contractautomataproject.tictactoe.symbols.Cross;
import io.github.contractautomataproject.tictactoe.symbols.Symbol;

public class Matrix {
	private final List<List<String>> table;

	public Matrix() {
		super();
		this.table = new ArrayList<>(3);
		this.table.add(new ArrayList<>(List.of(" ", " ", " ")));
		this.table.add(new ArrayList<>(List.of(" ", " ", " ")));
		this.table.add(new ArrayList<>(List.of(" ", " ", " ")));
	}
	
	public void set(Symbol s, Integer pos) {
		table.get(pos / 3).set(pos % 3, s.getSymbol());
	}
	
	public boolean isAvailable(Integer pos) {
		return table.get(pos/3).get(pos%3).equals(" ");
	}
	
	public String toString() {
		return table.get(0) + System.lineSeparator() + table.get(1) + System.lineSeparator() + table.get(2);
	}
	
	public void printInformation() {
		System.out.println("Each number corresponds to a position in the table : ");
		System.out.println(List.of("0","1","2") + System.lineSeparator() + 
				List.of("3","4","5") + System.lineSeparator() + 
				List.of("6","7","8"));
	}
	
	public boolean win(String who) {
		Predicate<String> rows = sym -> table.stream()
										.anyMatch(row->row.stream().allMatch(s->s.equals(sym)));
		
		Predicate<String> columns = sym -> IntStream.range(0, 3)
				.mapToObj(i->IntStream.range(0, 3)
						.mapToObj(j->table.get(j).get(i)))
				.anyMatch(col->col.allMatch(s->s.equals(sym)));
		
		Predicate<String> diagonals = sym -> IntStream.range(0, 3)
				.allMatch(i->table.get(i).get(i).equals(sym)) ||
				 IntStream.range(0, 3)
				 .allMatch(i->table.get(i).get(2-i).equals(sym));
		
		return Stream.of(rows,columns,diagonals)
		.filter(p->p.test(who))
		.findAny().isPresent();
	}
	
	public boolean win() {
		return win(Circle.circle) || win(Cross.cross);
	}
	
	public Symbol whoHasWon() {
		if (win(Circle.circle))
			return new Circle();
		else if (win(Cross.cross))
			return new Cross();
		else 
			throw new UnsupportedOperationException();
	}

}
