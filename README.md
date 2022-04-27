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


### License

This software is available under GPL-3.0 license.


### Contacts

For further information contact me at davide.basile@isti.cnr.it
