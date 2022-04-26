package io.github.contractautomataproject.tictactoe;

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
import io.github.contractautomataproject.tictactoe.grid.Grid;
import io.github.contractautomataproject.tictactoe.symbols.Circle;
import io.github.contractautomataproject.tictactoe.symbols.Cross;
import io.github.contractautomataproject.tictactoe.symbols.Symbol;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
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

    private final static String dir = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" +
            File.separator+"resources"+File.separator;

    private final List<Action> actionsCross;
    private final List<Action> actionsCircle;
    private final Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> automaton;
    private final int size;

    public AppBuildStrategy(int size)  {
        this.size=size;

        //all the actions for the cross, e.g., X_0, ... X_8
        actionsCross = IntStream.range(0, size)
                .mapToObj(i -> new RequestAction("X_" + i))
                .collect(Collectors.toList());


        //all the actions for the circle, e.g., O_0, ... O_8
        actionsCircle = IntStream.range(0, size)
                .mapToObj(i -> new RequestAction("O_" + i))
                .collect(Collectors.toList());

       automaton =  buildAutomaton();

    }

    /**
     * main method to synthesise and store the strategies
     *
     */
    public static void main(String[] args) throws IOException {
        System.out.println("Creating the plant automaton, this may take more than 10 minutes...");
        AppBuildStrategy stra = new AppBuildStrategy(9);
        System.out.println("Plant automaton created.");

        AutDataConverter<CALabel> adc = new AutDataConverter<>(CALabel::new);
        adc.exportMSCA(dir + "strategy"+new Cross().getSymbol()+".data", stra.choosePlayerAndStrategySynthesis(new Cross()));
        adc.exportMSCA(dir + "strategy"+new Circle().getSymbol()+".data", stra.choosePlayerAndStrategySynthesis(new Circle()));

        System.out.println("The strategies are stored under the folder "+dir);
    }

    public Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> getAutomaton() {
        return automaton;
    }

    /**
     * this method build the plant automaton, which is composed of the two players moving in turn and performing only valid moves.
     *
     */
    private Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> buildAutomaton()  {
        State<String> cs_circle = new State<>(List.of(new BasicState<>("0", true, true)));
        State<String> cs_cross = new State<>(List.of(new BasicState<>("0", true, true)));

        Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>>
                circlePlayer = new Automaton<>(createTransitions(cs_circle,cs_circle,ac->new CALabel(1, 0, ac),l->true,new Circle()));

        Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>>
                crossPlayer = new Automaton<>(createTransitions(cs_cross,cs_cross,ac->new CALabel(1, 0, ac),l->true,new Cross()));

        MSCACompositionFunction<String> mcf = new MSCACompositionFunction<>(List.of(crossPlayer, circlePlayer), null);

        Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> aut =
                mcf.apply(Integer.MAX_VALUE);

        aut = turnMoves(aut);
        aut = noDuplicateMoves(aut);
        return aut;
    }

    /**
     * auxiliary method for creating transitions
     */
    private <L extends Label<Action>> Set<ModalTransition<String, Action, State<String>, L>> createTransitions
            (State<String> source, State<String> target, Function<Action,L> createLabel, Predicate<Action> p, Symbol s)
    {
        Stream<Action> stream = (s instanceof Cross)? actionsCross.stream()
                :(s instanceof Circle)? actionsCircle.stream()
                :Stream.concat(actionsCross.stream(),actionsCircle.stream());
        return  stream.filter(p)
                    .map(ac -> new ModalTransition<>(source,
                            createLabel.apply(ac),
                            target, ModalTransition.Modality.PERMITTED))
                    .collect(Collectors.toSet());

    }

    /**
     * this method builds the automaton property for enforcing turns between the players, and
     * synthesise the automaton enforcing this property
     */
    private Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>>
    turnMoves( Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> aut) {
        State<String> cs_cross = new State<>(List.of(new BasicState<>("TurnCross", true, true)));
        State<String> cs_circle = new State<>(List.of(new BasicState<>("TurnCircle", false, true)));
        State<String> cs_fail = new State<>(List.of(new BasicState<>("Fail", false, false)));

        Set<ModalTransition<String, Action, State<String>, Label<Action>>> transitions = new HashSet<>();

        //cross turn
        Map.of(cs_cross, cs_circle, cs_circle, cs_fail).entrySet()
                        .forEach(e->  transitions.addAll(createTransitions(e.getKey(),e.getValue(),ac->new Label<>(List.of(ac)),l->true, new Cross())));

        //circle turn
        Map.of(cs_circle,cs_cross, cs_cross, cs_fail).entrySet()
                        .forEach(e->transitions.addAll(createTransitions(e.getKey(),e.getValue(),ac->new Label<>(List.of(ac)),l->true, new Circle())));

        MpcSynthesisOperator<String> mso = new MpcSynthesisOperator<>(l->true, new Automaton<>(transitions));

        return mso.apply(aut);
    }

    /**
     * this method builds the automaton property for forbidding invalid moves, which are inserting in a position
     * already occupied or inserting from a configuration where the game is over
     */
    private Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>>
    noDuplicateMoves(Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> aut){
        Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> a = aut;
        for (int i=0;i<size;i++) {
            a = noDuplicateMove(i + "", a);

            //no transitions outgoing winning states
            a = new Automaton<>(a.getTransition().parallelStream()
                    .filter(t->!(new Grid(t.getSource().toString()).win()))
                    .collect(Collectors.toSet()));
        }

        return a;
    }

    /**
     * used by method noDuplicateMoves for a single move
     */
    private Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>>
    noDuplicateMove(String move,
                    Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> aut) {
        State<String> cs_can = new State<>(List.of(new BasicState<>("_"+move, true, true)));
        State<String> cs_cross = new State<>(List.of(new BasicState<>(Cross.cross+"_"+move, false, true)));
        State<String> cs_circle = new State<>(List.of(new BasicState<>(Circle.circle+"_"+move, false, true)));
        State<String> cs_fail = new State<>(List.of(new BasicState<>("Fail", false, false)));

        Set<ModalTransition<String, Action, State<String>, Label<Action>>> transitions = new HashSet<>();

        //performing the move the first time
        Map.of(Cross.cross,cs_cross, Circle.circle, cs_circle).entrySet()
                .forEach(e->   transitions.add(new ModalTransition<>(cs_can,
                        new Label<>(List.of(new Action(e.getKey()+"_"+move))),
                        e.getValue(), ModalTransition.Modality.PERMITTED)));

        //transitions to a failure state (second time the same move is performed)
        Stream.of(cs_cross,cs_circle)
                .forEach(s->  transitions.addAll(Stream.of(new Action(Cross.cross+"_"+move), new Action(Circle.circle+"_"+move))
                        .map(act->new ModalTransition<>(s,
                                new Label<>(List.of(act)),
                                cs_fail, ModalTransition.Modality.PERMITTED))
                        .collect(Collectors.toSet())));

        //other transitions that are not the selected move
        Stream.of(cs_can,cs_cross,cs_circle)
                        .forEach(s->transitions.addAll(createTransitions(s,s,ac->new Label<>(List.of(ac)),a->!a.getLabel().equals(Cross.cross+"_"+move),new Cross())));
        Stream.of(cs_can,cs_cross,cs_circle)
                .forEach(s->transitions.addAll(createTransitions(s,s,ac->new Label<>(List.of(ac)),a->!a.getLabel().equals(Circle.circle+"_"+move),new Circle())));


        MpcSynthesisOperator<String> mso = new MpcSynthesisOperator<>(l->true, new Automaton<>(transitions));
        System.out.println(LocalDateTime.now() + " Synthesis of no duplicate move for position "+move+" of 8");
        return mso.apply(aut);
    }


    /**
     * this method synthesise the strategy for one of the two players.
     * It consists in rendering uncontrollable the transitions of the opponent, mark the
     * successful configurations and synthesise the strategy.
     *
     * @param player
     * @return
     */
    private Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> choosePlayerAndStrategySynthesis(Symbol player)  {

        //removing final states  and turning the transitions of the opponent to uncontrollable
        RelabelingOperator<String,CALabel> ro = new RelabelingOperator<>(CALabel::new,s->s, BasicState::isInitial,s->false);

        Set<ModalTransition<String, Action, State<String>, CALabel>> transitions = ro.apply(this.getAutomaton()).parallelStream()
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
                .flatMap(t->Stream.of(t.getSource(),t.getTarget()))
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
