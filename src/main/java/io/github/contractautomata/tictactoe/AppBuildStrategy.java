package io.github.contractautomata.tictactoe;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.Label;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.label.action.OfferAction;
import io.github.contractautomata.catlib.automaton.label.action.RequestAction;
import io.github.contractautomata.catlib.automaton.state.BasicState;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.catlib.operations.MSCACompositionFunction;
import io.github.contractautomata.catlib.operations.MpcSynthesisOperator;
import io.github.contractautomata.catlib.operations.RelabelingOperator;
import io.github.contractautomata.catlib.requirements.StrongAgreement;
import io.github.contractautomata.tictactoe.grid.Grid;
import io.github.contractautomata.tictactoe.symbols.Circle;
import io.github.contractautomata.tictactoe.symbols.Cross;
import io.github.contractautomata.tictactoe.symbols.Symbol;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This app is used to build the automata for the strategies for player X and O,
 * which will be used by the guided opponent during the game.
 *
 * The various automata are build inside this class.
 *
 * @author Davide Basile
 */
public class AppBuildStrategy {
    private final List<Action> actionsCross;
    private final List<Action> actionsCircle;
    private final Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> automaton;
    private final int size;

    /**
     * The constructor instantiates the possible actions of the game and invoke the method buildAutomaton, 
     * which will build a strategy for the player passed as argument
     *
     * @param player the player to which the strategy is to be synthesised
     */
    public AppBuildStrategy(Symbol player)  {
        this.size=9;

        //all the actions for the cross, e.g., X_0, ... X_8
        actionsCross = IntStream.range(0, size)
                .mapToObj(i -> new RequestAction("X_" + i))
                .collect(Collectors.toList());


        //all the actions for the circle, e.g., O_0, ... O_8
        actionsCircle = IntStream.range(0, size)
                .mapToObj(i -> new RequestAction("O_" + i))
                .collect(Collectors.toList());

        automaton =  buildAutomaton(player);

    }

    /**
     * Main method to synthesise and store the strategies.
     * The strategies are stored into two separate files.
     */
    public static void main(String[] args) throws IOException {
        AutDataConverter<CALabel> adc = new AutDataConverter<>(CALabel::new);

        System.out.println("Building the strategy for player X, this may take some minute...");

        Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategyX =
                new AppBuildStrategy(new Cross()).getAutomaton();

        adc.exportMSCA("strategy"+new Cross().getSymbol()+".data", strategyX);
        System.out.println("The strategy for player X has "+strategyX.getNumStates()+ " total states and "+strategyX.getTransition().size()+" total transitions.");


        System.out.println("Building the strategy for player O, this may take some minute...");

        Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> strategyO =
                new AppBuildStrategy(new Circle()).getAutomaton();

        adc.exportMSCA( "strategy"+new Circle().getSymbol()+".data", strategyO);
        System.out.println("The strategy for player O has "+strategyO.getNumStates()+ " total states and "+strategyO.getTransition().size()+" total transitions.");
    }

    public Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> getAutomaton() {
        return automaton;
    }

    /**
     * Build the strategy for the player passed as parameter
     */
    private Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> buildAutomaton(Symbol player)  {
        Symbol opponent = (player instanceof Circle)?new Cross():new Circle();

        //creating a list of automata, one for each position, offering to write either X or O in that position
        List<Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>>> aut =
                IntStream.range(0,size).mapToObj(i->{
                    State<String> cs_can = new State<>(List.of(new BasicState<>("_" + i, true, true)));
                    State<String> cs_cross = new State<>(List.of(new BasicState<>(Cross.cross + "_" + i, false, true)));
                    State<String> cs_circle = new State<>(List.of(new BasicState<>(Circle.circle + "_" + i, false, true)));

                    return new Automaton<>(Map.of(Cross.cross, cs_cross, Circle.circle, cs_circle).entrySet().stream()
                            .map(e-> new ModalTransition<>(cs_can,
                                    new CALabel(1,0,new OfferAction(e.getKey() + "_" + i)),
                                    e.getValue(), ModalTransition.Modality.PERMITTED))
                            .collect(Collectors.toSet()));

                }).collect(Collectors.toList());

        //creating an automaton requiring turns between X and O
        State<String> cs_cross = new State<>(List.of(new BasicState<>("TurnCross", true, true)));
        State<String> cs_circle = new State<>(List.of(new BasicState<>("TurnCircle", false, true)));
        aut.add(new Automaton<>(Stream.concat(
                //cross turn
                actionsCross.stream()
                        .map(ac->new ModalTransition<>(cs_cross,new CALabel(1,0,ac),cs_circle, ModalTransition.Modality.PERMITTED)),
                //circle turn
                actionsCircle.stream()
                        .map(ac->new ModalTransition<>(cs_circle,new CALabel(1,0,ac),cs_cross, ModalTransition.Modality.PERMITTED))
        ) .collect(Collectors.toSet())));

        System.out.println("...computing the composition...");

        MSCACompositionFunction<String> mcf = new MSCACompositionFunction<>(aut, new StrongAgreement().negate());
        Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> comp = mcf.apply(Integer.MAX_VALUE);

        //reset all final states to false
        RelabelingOperator<String,CALabel> ro = new RelabelingOperator<>(CALabel::new,x->x,BasicState::isInitial,x->false);

        Set<ModalTransition<String, Action, State<String>, CALabel>> transitions =
                ro.apply(comp).parallelStream()
                //removing transitions from states where someone wins or there is a draw
                .filter(t->{
                    Grid m = new Grid(t.getSource().toString());
                    return !(m.win()||m.tie());
                })
                //turning the opponent transitions to uncontrollable
                .map(t->new ModalTransition<>(t.getSource(),t.getLabel(),t.getTarget(),
                        t.getLabel().getAction().getLabel().startsWith(opponent.getSymbol())?
                                ModalTransition.Modality.URGENT: ModalTransition.Modality.PERMITTED
                ))
                .collect(Collectors.toSet());

        //creating the unique final state
        State<String> win = new State<>(IntStream.range(0,10)
                .mapToObj(i->new BasicState<>("Success",false,true))
                .collect(Collectors.toList()));


        //add transitions to win from states where player wins or ties
        transitions.addAll(transitions.parallelStream()
                .flatMap(t->Stream.of(t.getSource(),t.getTarget()))
                .filter(s->{
                    Grid m = new Grid(s.toString());
                    return m.win(player.getSymbol()) || m.tie();
                })
                .map(s->new ModalTransition<>(s,
                        new CALabel(10,0,new OfferAction("success")),
                        win,
                        ModalTransition.Modality.PERMITTED))
                .collect(Collectors.toSet()));

        System.out.println("...computing the synthesis... ");
        MpcSynthesisOperator<String> mso = new MpcSynthesisOperator<>(l->true);
        return mso.apply(new Automaton<>(transitions));

    }

}
