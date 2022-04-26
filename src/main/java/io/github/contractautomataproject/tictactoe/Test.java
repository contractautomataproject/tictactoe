package io.github.contractautomataproject.tictactoe;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.label.action.OfferAction;
import io.github.contractautomata.catlib.automaton.state.BasicState;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.catlib.operations.MpcSynthesisOperator;
import io.github.contractautomata.catlib.operations.RelabelingOperator;
import io.github.contractautomataproject.tictactoe.grid.Grid;
import io.github.contractautomataproject.tictactoe.symbols.Cross;
import io.github.contractautomataproject.tictactoe.symbols.Symbol;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Test {
    private final static String dir = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" +
            File.separator+"resources"+File.separator;

    public static void main(String[] args) throws IOException {
        AutDataConverter<CALabel> adc = new AutDataConverter<>(CALabel::new);

        Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> aut = choosePlayerAndStrategySynthesis(new Cross(),adc.importMSCA(dir+"plant.data"));

        System.out.println(aut.getBasicStates());

    }


    private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> choosePlayerAndStrategySynthesis(
            Symbol player, Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> aut)  {

        //removing final states  and turning the transitions of the opponent to uncontrollable
        RelabelingOperator<String,CALabel> ro = new RelabelingOperator<>(CALabel::new, s->s, BasicState::isInitial, s->false);

        Set<ModalTransition<String, Action, State<String>, CALabel>> transitions = ro.apply(aut).parallelStream()
                .map(t->new ModalTransition<>(t.getSource(),t.getLabel(),t.getTarget(),
                        t.getLabel().getAction().getLabel().contains(player.getSymbol())?
                                ModalTransition.Modality.PERMITTED
                                : ModalTransition.Modality.URGENT
                ))
                .collect(Collectors.toSet());

        //add transitions to a new winning state when player wins
        State<String> win = new State<>(IntStream.range(0,12)
                .mapToObj(i->new BasicState<>("Success",false,true))
                .collect(Collectors.toList()));

        transitions.addAll(transitions.parallelStream()
                .flatMap(t-> Stream.of(t.getSource(),t.getTarget()))
                .filter(s->{
                    Grid m = new Grid(s.toString());
                    return m.win(player.getSymbol()) || m.tie();
                })//only states where the player wins or ties
                .map(s->new ModalTransition<>(s,
                        new CALabel(12,0,new OfferAction("success")),
                        win,
                        ModalTransition.Modality.PERMITTED))
                .collect(Collectors.toSet()));

        Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> plant = new Automaton<>(transitions);

        System.out.println("Synthesising the strategy for player "+player.getSymbol());

        //return the strategy for player
        MpcSynthesisOperator<String> mso = new MpcSynthesisOperator<>(l->true);
        return mso.apply(plant);
    }
}
