package io.github.contractautomata.tictactoe.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.github.contractautomata.tictactoe.symbols.Cross;
import io.github.contractautomata.tictactoe.symbols.Circle;
import io.github.contractautomata.tictactoe.symbols.Symbol;

public class Grid {
	private final List<List<String>> table;

	public Grid() {
		super();
		this.table = new ArrayList<>(3);
		this.table.add(new ArrayList<>(List.of(" ", " ", " ")));
		this.table.add(new ArrayList<>(List.of(" ", " ", " ")));
		this.table.add(new ArrayList<>(List.of(" ", " ", " ")));
	}

	public Grid(String fill) {
		this();
		for (Symbol s : List.of(new Cross(),new Circle())){
			for (int i=0;i<9;i++){
				if (fill.contains(s.getSymbol()+"_"+i))
					this.set(s,i);
			}
		}
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

	public String toStringLine(){
		return IntStream.range(0,9)
				.mapToObj(i-> (isAvailable(i))?"_"+i:table.get(i/3).get(i%3)+"_"+i)
				.collect(Collectors.joining(", "));
	}
	
	public void printInformation() {
		System.out.println("Each number corresponds to a position in the table : ");
		System.out.println(List.of("0","1","2") + System.lineSeparator() + 
				List.of("3","4","5") + System.lineSeparator() + 
				List.of("6","7","8"));
		System.out.println();
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
		.anyMatch(p->p.test(who));
	}
	
	public boolean win() {
		return win(Circle.circle) || win(Cross.cross);
	}

	public boolean tie() {
		return !win() && IntStream.range(0, 9)
				.noneMatch(i-> isAvailable(i));
	}
	
	public Symbol whoHasWon() {
		if (win(Circle.circle))
			return new Circle();
		else if (win(Cross.cross))
			return new Cross();
		else 
			return null;
	}

}
