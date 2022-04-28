# Tic-tac-toe

This repository contains an implementation of the tic-tac-toe game.
This software is an example of usage of the contract automata library (CATLib) to model this game and 
synthesise (offline) the strategies of both players, which will be used at runtime by the 
computer to play against a human.

It is possible to choose between X and O players to play against the computer. 
The computer can either play randomly, or following a synthesised strategy. 
In this last case, the strategy allows the computer to never lose a game (it always wins or ties).
The synthesised strategy is <em>most permissive</em>: all plays in which the computer ties or wins are 
in the strategy. 
To keep this example as simple as possible, no optimization is performed to reduce the state-space.
In particular, there are 3878 reachable configurations in the strategy of player X and 2094 in the strategy of player O.

### Getting started

The easiest way to start is to download the released jar files of this repository (see Releases in the right column). 
These files are runnable from command-line, and  they only require a Java distribution compatible with version 11.

To play the game type:
```console
java -jar tictactoe-0.0.3-SNAPSHOT-jar-with-dependencies.jar
```

To synthesise the strategies type: (the strategies will be stored in the same folder where the jar is located).
```console
java -jar tictactoeBuildStrategy-0.0.3-SNAPSHOT-jar-with-dependencies.jar
```

Otherwise, clone the repository and see below for information about the classes.

### Implementation

The two executable classes are `App.java` and `AppBuildStrategy.java`.

`App.java` contains the game, whilst  `AppBuildStrategy.java` is used to create (offline) the strategies that 
are used by `App.java` to play the game. The used automata are built or synthesised inside `AppBuildStrategy.java`.

Inside the package `grid`, the class `Grid.java` is used to store a configuration of the game, to update the configuration, and 
has methods to check if the configuration is winning for some player or if it is a tie. 
This class has also facilities to print at console the current configuration of the game, and to import/export 
a configuration stored as label of a state of an automaton.

Finally, inside the package `symbols`, the class `Symbol.java` and its sub-classes `Circle.java` and `Cross.java` are used to store 
information about the representation of each player.

#### Synthesis 

Concerning the realization of the strategy, this is implemented in `AppBuildStrategy.java`.
Firstly, 9 automata are instantiated, each one represents the typing of X or O to a specific position. 
Each of these automata has three states (no symbol, X or O). The one for position 0 is displayed below.

![Position zero](src/main/resources/move_0.png)

A further automaton `turns` (depicted below) is necessary for enforcing turns between X and O, where X is the first to move.
This automaton has two states, one for each turn. 
From each turn/state, there are 8 outgoing transitions to the other state.

![Turns](src/main/resources/turns.png)

The composition of these 9+1 automata is firstly computed. 
In the composition, the requests of `turns` are all matched by the offers of one of the other 
automata in the composition, so that all transitions in the composition are matches between an offer and a request.
After that, depending on which player is selected, the composed automaton is slightly modified before 
starting the synthesis. 
The moves of the opponent are turned to uncontrollable, transitions outgoing winning states are removed (for both players), and only the configurations where the selected player wins or ties are marked as final. 
To check if a state is winning or draw, the class `Grid.java` is used: an object is instantiated passing as argument the state, and subsequently the corresponding methods are invoked.

The synthesised controller/strategy will guarantee the maximal behaviour where a final state is reachable, forbidden states where the opponent wins are 
never traversed, and uncontrollable transitions are never blocked. 
Below is an image of the strategy for player O that is automatically computed.
The images of the automata have been created using CAT_App.

![Strategy 0](src/main/resources/strategyO.png)
### License

This software is available under GPL-3.0 license.


### Contacts

For further information contact me at davide.basile@isti.cnr.it
