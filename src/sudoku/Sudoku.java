/*
  PURPOSE:

 Solves 9x9 Sudoku puzzles

 INPUT:
    
 FORMAT
 9x9 sudoku puzzle in .txt comma-separated format; sample attached


 FUNCTIONALITY & ORGANIZATION:

 ALGORITHM:
 Greedy depth-first iterative-deepening constraint-propagating search/constraint algorithm

 Constraint-propagating:
 Escalates through constraint techniques until all of them have been applied
 exhaustively, and no more deductions can be made. Makes use of the Lone Single,
 Hidden Single, Open/Naked Pair and Open/Naked Triple methods, which
 are well-documented online

 Greedy depth-limited:
 Searches deeper greedily (picking a square and value to guess from among the 
 most-constrained squares first) using depth-first, depth-limited search, 
 continuing to exhaustively propagate constraints after each guess.

 Iterative-deepening:
 Escalates to higher depth-limits (iterative-deepening search) if solutions are not
 found in shallower states, up to an absolute depth limit after which continued
 search is not practical due to extremely high computational complexity. Because
 the search space grows exponentially with depth, with a branching factor often in
 excess of 300, finding solutions at greater depths is extremely unlikely.

 DATA STRUCTURES:
    
 Primarily relies on a 3-dimensional boolean array, with the array 
 coordinates corresponding to x and y locations on the puzzle and remaining
 potential values at that x-y location, so for example if 5 remains a potential 
 value for the square at 3,4, then graph[3][4][5] is true. Various metadata is
 stored in other structures; this allows some unnecessary computations to be
 avoided.


 OUTPUT:

 Returns a solution if a solution exists within the absolute depth-limit assigned
 at initialization.

 */
package sudoku;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;

public class Sudoku {

    //[x][y][n] : number n (except 0, which 
    //indicates whether that point has been played
    //at location x, y such that 0,0 is at the 
    //upper-left
    private boolean[][][] graph;

    //METADATA
    int unplayed; //The number of points remaining whose values are uncertain
    private boolean[][] row; //[y][n] : number n has been played in row y
    private boolean[][][] block; //[b][n] : number n has been played in blockX, blockY
    private boolean[][] column; //[x][n] : number n has been played in column x

    //METHOD USAGE/BEHAVIOR TRACKING
    private static int loneSingles;
    private static int hiddenSingles;
    private static int nakedPairs;
    private static int nakedTriples;
    private static int visited;
    private static int failures;

    ////////////////INITIALIZATION METHODS & CONSTRUCTORS ///////////////////////
    //Used for initialization
    public Sudoku(String fileName) {
        graph = new boolean[9][9][10];
        row = new boolean[9][10];
        column = new boolean[9][10];
        block = new boolean[3][3][10];
        
        loneSingles = 0;
        hiddenSingles = 0;
        nakedPairs = 0;
        nakedTriples = 0;
        visited = 0;
        failures = 0;

        unplayed = 81;

        //initialize graph
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                graph[x][y][0] = false;
                for (int n = 1; n < 10; n++) {
                    graph[x][y][n] = true;
                }
            }
        }

        int[][] puzzle = loadPuzzle(fileName);

        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                if (puzzle[x][y] != 0) {
                    playPoint(x, y, puzzle[x][y]);
                }
            }
        }
    }

    //Used to instantiate branch states for recursive search
    private Sudoku() {
        graph = new boolean[9][9][10];
        row = new boolean[9][10];
        column = new boolean[9][10];
        block = new boolean[3][3][10];
    }

    private static int[][] loadPuzzle(String fileName) {

        int[][] puzzle = new int[9][9]; // x,y

        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            int y = 0;
            while ((line = br.readLine()) != null && y < 9) {
                String[] tokens = line.split(",", 9);

                for (int x = 0; x < 9; x++) {
                    puzzle[x][y] = Integer.parseInt(tokens[x]);
                }

                y++;
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Failed to load");
            System.exit(-1);
        }
        return puzzle;
    }

    //////////////////CONSTRAIN METHODS/////////////////////////////////
    //Constrains row, column, b and vertex based on specified arguments
    private void playPoint(int targetX, int targetY, int number) {

        //Error-checking
        if (targetX < 0 || targetX > 8 || targetY < 0 || targetY > 8 || number < 1 || number > 9) {
            System.out.println("Warning: invalid move number/location");
        } else if (!graph[targetX][targetY][number]) {
            System.out.println("Warning: move precluded by previous constraints");
        } else if (graph[targetX][targetY][0]) {
            System.out.println("Warning: move already played");
        }

        //Update unplayed points count
        unplayed--;

        //Constrain row
        row[targetY][number] = true;
        for (int x = 0; x < 9; x++) {
            graph[x][targetY][number] = false;
        }

        //Constrain column
        column[targetX][number] = true;
        for (int y = 0; y < 9; y++) {
            graph[targetX][y][number] = false;
        }

        //Constrain b
        int blockX = targetX / 3; //Get the b numbers
        int blockY = targetY / 3;
        block[blockX][blockY][number] = true;

        int refX = blockX * 3;
        int refY = blockY * 3;

        for (int x = refX; x < refX + 3; x++) {
            for (int y = refY; y < refY + 3; y++) {
                graph[x][y][number] = false;
            }
        }

        //Constrain & fix target point
        for (int n = 1; n < 10; n++) {
            graph[targetX][targetY][n] = false;
        }
        graph[targetX][targetY][number] = true; //Correct number
        graph[targetX][targetY][0] = true; //Played        
    }

    //Iteratively escaltes through all constraint techniques until all
    //techniques have been exhausted
    private void propagateConstraints() {

        boolean done = false;

        while (!done) {

            //Assume we won't find anything to change
            done = true;

            //CHECKS
            //Check for failure; end immediately if invalid graph detected
            if (!this.checkStateValidity()) {
                return;
            }

            //Check for completion; end immediately if completion detected
            if (this.checkSolutionCompleteness()) {
                return;
            }

            //SINGLES
            //Check for a lone single; restart loop if found 
            if (findAndConstrainLoneSingle()) {
                loneSingles++;
                done = false;
                continue;
            }

            //Check for a hidden single; restart loop if found 
            if (findAndConstrainHiddenSingle()) {
                hiddenSingles++;
                done = false;
                continue;
            }

            //NAKED TRIPLES
            if (findAndConstrainNakedTriplesInRows()) {
                nakedTriples++;
                done = false;
                continue;
            }
            if (findAndConstrainNakedTriplesInColumns()) {
                nakedTriples++;
                done = false;
                continue;
            }

            if (findAndConstrainNakedTriplesInBlocks()) {
                nakedTriples++;
                done = false;
                continue;
            }

            //NAKED PAIRS
            if (findAndConstrainNakedPairsInRows()) {
                nakedPairs++;
                done = false;
                continue;
            }

            if (findAndConstrainNakedPairsInColumns()) {
                nakedPairs++;
                done = false;
                continue;
            }

            if (findAndConstrainNakedPairsInBlocks()) {
                nakedPairs++;
                done = false;
                continue;
            }

            //NAKED QUADS
        }

    }

    //Checks points until a lone potentialSingle is potentialSingle:
    //If only one possible value remains, constrains that point
    //Returns true when & iff a lone potentialSingle is potentialSingle & constrained
    private boolean findAndConstrainLoneSingle() {

        int[][] constraintMap = getConstraintMap();

        //For each point
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {

                //If the point has only one possible value but hasn't been played
                if (constraintMap[x][y] == 1 && !graph[x][y][0]) {

                    //Then constrain that last possible value
                    for (int n = 1; n < 10; n++) {
                        if (graph[x][y][n]) {
                            playPoint(x, y, n);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    //Checks rows, columns & blocks until a hidden potentialSingle is potentialSingle:
    //If any number appears only once, constrain that point
    //Returns true when & iff a hidden potentialSingle is potentialSingle & constrained
    private boolean findAndConstrainHiddenSingle() {

        //For each column
        for (int x = 0; x < 9; x++) {

            //For each number that hasn't been played in this column
            for (int n = 1; n < 10; n++) {
                if (!column[x][n]) {

                    //Count how many columns can possibly hold that value
                    int count = 0;
                    int potentialSingle = 0;
                    for (int y = 0; y < 9; y++) {

                        if (graph[x][y][n]) {
                            count++;
                            potentialSingle = y;
                        }
                    }
                    //Constrain the potentialSingle if it hasn't already been played
                    if (count == 1 && !graph[x][potentialSingle][0]) {
                        playPoint(x, potentialSingle, n);
                        return true;
                    }
                }
            }
        }

        //For each row
        for (int y = 0; y < 9; y++) {

            //For each number that hasn't been played in this row
            for (int n = 1; n < 10; n++) {
                if (!row[y][n]) {

                    //Count how many rows can possibly hold that value
                    int count = 0;
                    int potentialSingle = 0;
                    for (int x = 0; x < 9; x++) {

                        if (graph[x][y][n]) {
                            count++;
                            potentialSingle = x;
                        }
                    }
                    //Constrain the potentialSingle if it hasn't already been played
                    if (count == 1 && !graph[potentialSingle][y][0]) {
                        playPoint(potentialSingle, y, n);
                        return true;
                    }
                }
            }
        }

        //For each block
        for (int i = 0; i < 9; i++) {

            int blockX = i / 3;
            int blockY = i % 3;

            //For each number that hasn't been played in this block
            for (int n = 1; n < 10; n++) {
                if (!block[blockX][blockY][n]) {

                    //Count how many points can possibly hold that value
                    int count = 0;
                    int potentialSingleX = 0;
                    int potentialSingleY = 0;
                    for (int x = blockX * 3; x < blockX * 3 + 3; x++) {
                        for (int y = blockY * 3; y < blockY * 3 + 3; y++) {
                            if (graph[x][y][n]) {
                                count++;
                                potentialSingleX = x;
                                potentialSingleY = y;
                            }
                        }
                    }

                    //Constrain the potentialSingle if it hasn't already been played
                    if (count == 1 && !graph[potentialSingleX][potentialSingleY][0]) {
                        playPoint(potentialSingleX, potentialSingleY, n);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean findAndConstrainNakedPairsInRows() {

        int[][] constraintMap = this.getConstraintMap();
        boolean[][] candidate;
        int[] numberOfCandidates;
        boolean madeChange = false;
        ArrayList<Pair> candidatePairs;
        ArrayList<Pair> filteredPairs;

        //ROWS
        candidate = new boolean[9][9];
        numberOfCandidates = new int[9];
        for (int i = 0; i < 9; i++) {
            numberOfCandidates[i] = 0;
        }

        //get & count candidates for naked-pairs check
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (constraintMap[x][y] == 2) {
                    candidate[x][y] = true;
                    numberOfCandidates[y]++;
                }
            }
        }

        //Build a list of candidate pairs in all rows
        candidatePairs = new ArrayList<>();
        for (int y = 0; y < 9; y++) {
            if (numberOfCandidates[y] >= 2) {
                for (int x1 = 0; x1 < 8; x1++) {
                    for (int x2 = x1 + 1; x2 < 9; x2++) {
                        if (candidate[x1][y] && candidate[x2][y]) {
                            candidatePairs.add(new Pair(x1, y, x2, y));
                        }
                    }
                }
            }
        }

        //Filter the list of pairs to include only valid pairs
        filteredPairs = new ArrayList<>();
        for (Pair pair : candidatePairs) {
            HashSet<Integer> values = new HashSet<>();

            for (int n = 1; n < 10; n++) {
                if (graph[pair.x1][pair.y1][n] || graph[pair.x2][pair.y2][n]) {
                    values.add(n);
                }
            }

            //If there are exactly two possible values between the points, those values must be the same
            //Constrain row based on these values
            if (values.size() == 2) {

                for (int n = 1; n < 10; n++) {
                    if (values.contains(n)) {
                        pair.n1 = n;
                        break;
                    }
                }

                for (int n = 9; n > 0; n--) {
                    if (values.contains(n)) {
                        pair.n2 = n;
                        break;
                    }
                }
                filteredPairs.add(pair);
            }
        }

        //For each triple in the filtered list, constrain row 
        for (Pair pair : filteredPairs) {

            for (int x = 0; x < 9; x++) {

                if ((x != pair.x1 && x != pair.x2)) {
                    //Identify whether new constraints have actually been found
                    if (graph[x][pair.y1][pair.n1] || graph[x][pair.y1][pair.n2]) {
                        madeChange = true;
                    }
                    //Constrain squares not in the triple
                    graph[x][pair.y1][pair.n1] = false;
                    graph[x][pair.y1][pair.n2] = false;
                }
            }
        }
        return madeChange;
    }

    private boolean findAndConstrainNakedPairsInColumns() {

        int[][] constraintMap = this.getConstraintMap();
        boolean[][] candidate;
        int[] numberOfCandidates;
        boolean madeChange = false;
        ArrayList<Pair> candidatePairs;
        ArrayList<Pair> filteredPairs;

        candidate = new boolean[9][9];
        numberOfCandidates = new int[9];
        for (int i = 0; i < 9; i++) {
            numberOfCandidates[i] = 0;
        }

        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (constraintMap[x][y] == 2) {
                    candidate[x][y] = true;
                    numberOfCandidates[x]++;
                }
            }
        }

        //Build a list of candidate pairs in all columns
        candidatePairs = new ArrayList<>();
        for (int x = 0; x < 9; x++) {
            if (numberOfCandidates[x] >= 2) {
                for (int y1 = 0; y1 < 8; y1++) {
                    for (int y2 = y1 + 1; y2 < 9; y2++) {
                        if (candidate[x][y1] && candidate[x][y2]) {
                            candidatePairs.add(new Pair(x, y1, x, y2));
                        }
                    }
                }
            }
        }

        //Filter the list of pairs to include only valid pairs
        filteredPairs = new ArrayList<>();
        for (Pair pair : candidatePairs) {
            HashSet<Integer> values = new HashSet<>();

            for (int n = 1; n < 10; n++) {
                if (graph[pair.x1][pair.y1][n] || graph[pair.x2][pair.y2][n]) {
                    values.add(n);
                }
            }

            //If there are exactly two possible values between the points, those values must be the same
            //Constrain row based on these values
            if (values.size() == 2) {

                for (int n = 1; n < 10; n++) {
                    if (values.contains(n)) {
                        pair.n1 = n;
                        break;
                    }
                }

                for (int n = 9; n > 0; n--) {
                    if (values.contains(n)) {
                        pair.n2 = n;
                        break;
                    }
                }
                filteredPairs.add(pair);
            }
        }

        //For each triple in the filtered list, constrain row 
        for (Pair pair : filteredPairs) {

            for (int y = 0; y < 9; y++) {

                if ((y != pair.y1 && y != pair.y2)) {
                    //Identify whether new constraints have actually been found
                    if (graph[pair.x1][y][pair.n1] || graph[pair.x1][y][pair.n2]) {
                        madeChange = true;
                    }
                    //Constrain squares not in the triple
                    graph[pair.x1][y][pair.n1] = false;
                    graph[pair.x1][y][pair.n2] = false;
                }
            }
        }
        return madeChange;
    }

    private boolean findAndConstrainNakedPairsInBlocks() {

        int[][] constraintMap = this.getConstraintMap();
        boolean[][] candidate;
        int[] numberOfCandidates;
        boolean madeChange = false;
        ArrayList<Pair> candidatePairs;
        ArrayList<Pair> filteredPairs;

        candidate = new boolean[9][9];
        numberOfCandidates = new int[9];
        for (int b = 0; b < 9; b++) {
            numberOfCandidates[b] = 0;
        }

        //get & count candidates for naked-pairs check
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (constraintMap[x][y] == 2) {
                    candidate[x][y] = true;
                }
            }
        }
        for (int b = 0; b < 9; b++) {

            int blockX = b / 3;
            int blockY = b % 3;

            for (int square = 0; square < 9; square++) {

                int x = blockX + square / 3;
                int y = blockY + square % 3;
                if (constraintMap[x][y] == 2) {
                    numberOfCandidates[b]++;
                }
            }
        }

        //Build a list of candidate pairs in all blocks
        candidatePairs = new ArrayList<>();
        for (int b = 0; b < 9; b++) {

            int blockX = b / 3;
            int blockY = b % 3;

            if (numberOfCandidates[b] >= 2) {

                for (int j1 = 0; j1 < 8; j1++) {
                    for (int j2 = j1 + 1; j2 < 9; j2++) {

                        int x1 = blockX + (j1 / 3);
                        int x2 = blockX + (j2 / 3);
                        int y1 = blockY + (j1 % 3);
                        int y2 = blockY + (j2 % 3);

                        if (candidate[x1][y1] && candidate[x2][y2]) {
                            candidatePairs.add(new Pair(x1, y1, x2, y2));
                        }
                    }
                }
            }
        }

        //Filter the list of pairs to include only valid pairs
        filteredPairs = new ArrayList<>();
        for (Pair pair : candidatePairs) {
            HashSet<Integer> values = new HashSet<>();

            for (int n = 1; n < 10; n++) {
                if (graph[pair.x1][pair.y1][n] || graph[pair.x2][pair.y2][n]) {
                    values.add(n);
                }
            }

            //If there are exactly two possible values between the points, those values must be the same
            //Constrain row based on these values
            if (values.size() == 2) {

                for (int n = 1; n < 10; n++) {
                    if (values.contains(n)) {
                        pair.n1 = n;
                        break;
                    }
                }

                for (int n = 9; n > 0; n--) {
                    if (values.contains(n)) {
                        pair.n2 = n;
                        break;
                    }
                }
                filteredPairs.add(pair);
            }
        }

        //For each triple in the filtered list, constrain row 
        for (Pair pair : filteredPairs) {

            int blockX = (pair.x1 / 3) * 3;
            int blockY = (pair.y1 / 3) * 3;

            for (int i = 0; i < 9; i++) {

                int x = blockX + i / 3;
                int y = blockY + i % 3;

                if (!((x == pair.x1 && y == pair.y1) || (x == pair.x2 && y == pair.y2))) {
                    //Identify whether new constraints have actually been found
                    if (graph[x][y][pair.n1] || graph[x][y][pair.n2]) {
                        madeChange = true;
                    }
                    //Constrain squares not in the triple
                    graph[x][y][pair.n1] = false;
                    graph[x][y][pair.n2] = false;
                }
            }
        }
        return madeChange;
    }

    private boolean findAndConstrainNakedTriplesInRows() {

        int[][] constraintMap = this.getConstraintMap();
        boolean[][] candidate;
        int[] numberOfCandidates;
        boolean madeChange = false;
        ArrayList<Triple> candidateTriples;
        ArrayList<Triple> filteredTriples;

        candidate = new boolean[9][9];
        numberOfCandidates = new int[9];
        for (int i = 0; i < 9; i++) {
            numberOfCandidates[i] = 0;
        }

        //O(9^2) - get & count candidates for naked-triples check
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (constraintMap[x][y] == 3 || constraintMap[x][y] == 2) {
                    candidate[x][y] = true;
                    numberOfCandidates[y]++;
                }
            }
        }

        //Build a list of candidate tiplets in all rows
        candidateTriples = new ArrayList<>();
        for (int y = 0; y < 9; y++) {
            if (numberOfCandidates[y] >= 3) {
                for (int x1 = 0; x1 < 7; x1++) {
                    for (int x2 = x1 + 1; x2 < 8; x2++) {
                        for (int x3 = x2 + 1; x3 < 9; x3++) {
                            if (candidate[x1][y] && candidate[x2][y] && candidate[x3][y]) {
                                candidateTriples.add(new Triple(x1, y, x2, y, x3, y));
                            }
                        }
                    }
                }
            }
        }

        //Filter the list of pairs to include only valid triples
        filteredTriples = new ArrayList<>();
        for (Triple triple : candidateTriples) {
            HashSet<Integer> values = new HashSet<>();

            for (int n = 1; n < 10; n++) {
                if (graph[triple.x1][triple.y1][n] || graph[triple.x2][triple.y2][n] || graph[triple.x3][triple.y3][n]) {
                    values.add(n);
                }
            }

            //If there are exactly three possible values between the points...
            //Constrain row based on these values
            if (values.size() == 3) {

                for (int n = 1; n < 10; n++) {
                    if (values.contains(n)) {
                        if (triple.n1 == 0) {
                            triple.n1 = n;
                        } else if (triple.n2 == 0) {
                            triple.n2 = n;
                        } else if (triple.n3 == 0) {
                            triple.n3 = n;
                        }
                    }
                }
                filteredTriples.add(triple);
            }
        }

        //For each triple in the filtered list, constrain row 
        for (Triple triple : filteredTriples) {

            for (int x = 0; x < 9; x++) {

                if ((x != triple.x1 && x != triple.x2 && x != triple.x3)) {
                    //Identify whether new constraints have actually been found
                    if (graph[x][triple.y1][triple.n1] || graph[x][triple.y1][triple.n2] || graph[x][triple.y1][triple.n3]) {
                        madeChange = true;
                    }
                    //Constrain squares not in the triple
                    graph[x][triple.y1][triple.n1] = false;
                    graph[x][triple.y1][triple.n2] = false;
                    graph[x][triple.y1][triple.n3] = false;
                }
            }
        }
        return madeChange;
    }

    private boolean findAndConstrainNakedTriplesInColumns() {

        int[][] constraintMap = this.getConstraintMap();
        boolean[][] candidate;
        int[] numberOfCandidates;
        boolean madeChange = false;
        ArrayList<Triple> candidateTriples;
        ArrayList<Triple> filteredTriples;

        candidate = new boolean[9][9];
        numberOfCandidates = new int[9];
        for (int i = 0; i < 9; i++) {
            numberOfCandidates[i] = 0;
        }

        //get & count candidates for naked-triples checks
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (constraintMap[x][y] == 3 || constraintMap[x][y] == 2) {
                    candidate[x][y] = true;
                    numberOfCandidates[x]++;
                }
            }
        }

        //Build a list of candidate pairs in all columns
        candidateTriples = new ArrayList<>();
        for (int x = 0; x < 9; x++) {
            if (numberOfCandidates[x] >= 3) {
                for (int y1 = 0; y1 < 7; y1++) {
                    for (int y2 = y1 + 1; y2 < 8; y2++) {
                        for (int y3 = y2 + 1; y3 < 9; y3++) {
                            if (candidate[x][y1] && candidate[x][y2] && candidate[x][y3]) {
                                candidateTriples.add(new Triple(x, y1, x, y2, x, y3));
                            }
                        }
                    }
                }
            }
        }

        //Filter the list of triples to include only valid triples
        filteredTriples = new ArrayList<>();
        for (Triple triple : candidateTriples) {
            HashSet<Integer> values = new HashSet<>();

            for (int n = 1; n < 10; n++) {
                if (graph[triple.x1][triple.y1][n] || graph[triple.x2][triple.y2][n] || graph[triple.x3][triple.y3][n]) {
                    values.add(n);
                }
            }

            //If there are exactly three possible values...
            //Constrain row based on these values
            if (values.size() == 3) {
                for (int n = 1; n < 10; n++) {
                    if (values.contains(n)) {
                        if (triple.n1 == 0) {
                            triple.n1 = n;
                        } else if (triple.n2 == 0) {
                            triple.n2 = n;
                        } else if (triple.n3 == 0) {
                            triple.n3 = n;
                        }
                    }
                }
                filteredTriples.add(triple);
            }
        }

        //For each triple in the filtered list, constrain row 
        for (Triple triple : filteredTriples) {

            for (int y = 0; y < 9; y++) {

                if ((y != triple.y1 && y != triple.y2)) {
                    //Identify whether new constraints have actually been found
                    if (graph[triple.x1][y][triple.n1] || graph[triple.x1][y][triple.n2] || graph[triple.x1][y][triple.n3]) {
                        madeChange = true;
                    }
                    //Constrain squares not in the triple
                    graph[triple.x1][y][triple.n1] = false;
                    graph[triple.x1][y][triple.n2] = false;
                    graph[triple.x1][y][triple.n3] = false;
                }
            }
        }
        return madeChange;

    }

    private boolean findAndConstrainNakedTriplesInBlocks() {

        int[][] constraintMap = this.getConstraintMap();
        boolean[][] candidate;
        int[][] numberOfCandidates;
        boolean madeChange = false;
        ArrayList<Triple> candidateTriples;
        ArrayList<Triple> filteredTriples;

        candidate = new boolean[9][9];
        numberOfCandidates = new int[3][3];
        for (int i = 0; i < 9; i++) {
            numberOfCandidates[i / 3][i % 3] = 0;
        }

        //Get & count candidates for naked-triples check
        for (int b = 0; b < 9; b++) {

            int blockX = b / 3;
            int blockY = b % 3;

            int refX = blockX * 3;
            int refY = blockY * 3;

            for (int square = 0; square < 9; square++) {

                int x = refX + square / 3;
                int y = refY + square % 3;
                if (constraintMap[x][y] == 2 || constraintMap[x][y] == 3) {
                    numberOfCandidates[blockX][blockY]++;
                    candidate[x][y] = true;
                }
            }
        }

        //Build a list of candidate pairs in all blocks
        candidateTriples = new ArrayList<>();
        for (int b = 0; b < 9; b++) {

            int blockX = b / 3;
            int blockY = b % 3;

            int refX = blockX * 3;
            int refY = blockY * 3;

            if (numberOfCandidates[blockX][blockY] >= 3) {

                for (int j1 = 0; j1 < 7; j1++) {
                    for (int j2 = j1 + 1; j2 < 8; j2++) {
                        for (int j3 = j2 + 1; j3 < 9; j3++) {

                            int x1 = refX + (j1 / 3);
                            int x2 = refX + (j2 / 3);
                            int x3 = refX + (j3 / 3);
                            int y1 = refY + (j1 % 3);
                            int y2 = refY + (j2 % 3);
                            int y3 = refY + (j3 % 3);

                            if (candidate[x1][y1] && candidate[x2][y2] && candidate[x3][y3]) {
                                candidateTriples.add(new Triple(x1, y1, x2, y2, x3, y3));
                            }
                        }
                    }
                }
            }
        }

        //Filter the list of triples to include only valid triples
        filteredTriples = new ArrayList<>();
        for (Triple triple : candidateTriples) {
            HashSet<Integer> values = new HashSet<>();

            for (int n = 1; n < 10; n++) {
                if (graph[triple.x1][triple.y1][n] || graph[triple.x2][triple.y2][n] || graph[triple.x3][triple.y3][n]) {
                    values.add(n);
                }
            }

            //If there are exactly three possible values between the points...
            //Constrain row based on these values
            if (values.size() == 3) {
                for (int n = 1; n < 10; n++) {
                    if (values.contains(n)) {
                        if (triple.n1 == 0) {
                            triple.n1 = n;
                        } else if (triple.n2 == 0) {
                            triple.n2 = n;
                        } else if (triple.n3 == 0) {
                            triple.n3 = n;
                        }
                    }
                }
                filteredTriples.add(triple);
            }
        }

        //For each triple in the filtered list, constrain row 
        for (Triple triple : filteredTriples) {

            //Get the b reference point
            int blockX = triple.x1 / 3;
            int blockY = triple.y1 / 3;
            blockX *= 3;
            blockY *= 3;

            for (int i = 0; i < 9; i++) {

                int x = blockX + i / 3;
                int y = blockY + i % 3;

                if (!((x == triple.x1 && y == triple.y1) || (x == triple.x2 && y == triple.y2) || (x == triple.x3 && y == triple.y3))) {
                    //Identify whether new constraints have actually been found
                    if (graph[x][y][triple.n1] || graph[x][y][triple.n2] || graph[x][y][triple.n3]) {
                        madeChange = true;
                    }
                    //Constrain squares not in the triple
                    graph[x][y][triple.n1] = false;
                    graph[x][y][triple.n2] = false;
                    graph[x][y][triple.n3] = false;
                }
            }
        }
        return madeChange;

    }

    //////////////////////SEARCH METHODS//////////////////////////
    //Starts iterative-deepening search with the depth-limit specified
    //Depth increases by one per iteration until a solution is found or all
    //valid moves are exhausted within the depth limit
    public int[][] startSearch(int limit) {
        int depth = 1;
        int[][] solution = null;
        while (depth <= limit && solution == null) {
            solution = iterativeDeepeningSearch(depth, limit);
            depth++;
        }
        return solution;
    }

    //Recursive method which calls all necessary 
    private int[][] iterativeDeepeningSearch(int depth, int limit) {

        visited++;

        //Check for failure due to invalid board
        if (!checkStateValidity()) {
            failures++;
            return null;
        }

        //Check for success
        if (checkSolutionCompleteness()) {
            if (checkSolutionCorrectness()) {
                System.out.println("\n\nSolution found at depth " + depth + ":");
                this.print();
                return makeSolution();
            } else {
                System.out.println("Error: all moves played, board not correct");
                print();
                System.exit(depth);
            }
        }

        //Fail at depth limit
        if (depth >= limit) {
            return null;
        }

        //Pick & constrain branches
        //Recursively call iterativeDeepeningSearch on suitable branches
        //Store previous moves in order to avoid duplicates
        HashSet<Sudoku> previousMoves = new HashSet<>();

        //Find the greatest degree of constraint
        int[][] constraintMap = this.getConstraintMap();
        int numberOfPossibleMoves = 8;
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                if (constraintMap[x][y] < numberOfPossibleMoves) {
                    numberOfPossibleMoves = constraintMap[x][y];
                }
            }
        }

        //Starting from the lowest constraint level (# of valid moves at point)
        //and continuing up while the constraint level is valid
        while (numberOfPossibleMoves < 10) {

            //For each point on the graph
            for (int x = 0; x < 9; x++) {
                for (int y = 0; y < 9; y++) {

                    //If the point hasn't been played, and is at the correct restraint level...
                    if (!graph[x][y][0] && constraintMap[x][y] == numberOfPossibleMoves) {
                        //Then for each possible value
                        for (int n = 1; n < 10; n++) {
                            //If that value is still valid
                            if (graph[x][y][n]) {
                                //Create & playPoint a branch
                                Sudoku branch = this.deepClone();
                                branch.playPoint(x, y, n);
                                branch.propagateConstraints();
                                //If that branch hasn't been explored previously
                                if (!previousMoves.contains(branch)) {
                                    //Add the branch to the list of previous moves
                                    previousMoves.add(branch);
                                    //Call iterativeDeepeningSearch() recursively on that branch
                                    int[][] solution = branch.iterativeDeepeningSearch(depth + 1, limit);

                                    //Return a solution, if one was found; else continue searching
                                    if (solution != null) {
                                        return solution;
                                    }
                                }
                            }
                        }
                    }

                }
            }
            numberOfPossibleMoves++; //Iterate to moves on less-constrained vertices
        }
        if (depth == 0) {
            System.out.println("Failed to find a solution"); //If we've exhausted all moves, indicate complete failure
            this.print();
        }
        return null;
    }

    ////////////////////GRAPH EVALUATION METHODS/////////////////////
    //Check all points for no-possible-values
    public boolean checkStateValidity() {

        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                if (!graph[x][y][0]) { //Don't bother checking previously-played points
                    boolean hasPossibleValue = false;

                    for (int n = 1; n < 10; n++) {
                        if (graph[x][y][n]) {
                            hasPossibleValue = true;
                            break;
                        }
                    }

                    if (!hasPossibleValue) { //Point has no possible values so graph is invalid
                        return false;
                    }

                }
            }
        }

        return true;
    }

    //Quick check for completeness
    private boolean checkSolutionCompleteness() {
        if (unplayed == 0) {
            return true;
        } else if (unplayed > 0) {
            return false;
        } else {
            System.out.println("Error: negative number of unplayed moves");
            System.exit(-1);
            return false;
        }
    }

    //Checks a valid, completely-played graph for successful completion
    private boolean checkSolutionCorrectness() {

        //Check columns
        //For each column
        for (int x = 0; x < 9; x++) {

            //Initialize a list of values
            boolean[] values = new boolean[10];
            for (int i = 0; i < 10; i++) {
                values[i] = false;
            }

            //Check each point in the column for its number, add to list
            for (int y = 0; y < 9; y++) {
                for (int n = 1; n < 10; n++) {
                    if (graph[x][y][n]) {
                        values[n] = true;
                        break;
                    }
                }
            }

            //If any value is missing, the solution is incorrect
            for (int i = 1; i < 10; i++) {
                if (!values[i]) {
                    return false;
                }
            }
        }

        //Check rows
        //For each row
        for (int y = 0; y < 9; y++) {

            //Initialize a list of values
            boolean[] values = new boolean[10];
            for (int i = 0; i < 10; i++) {
                values[i] = false;
            }

            //Check each point in the row for its number, add to list
            for (int x = 0; x < 9; x++) {
                for (int n = 1; n < 10; n++) {
                    if (graph[x][y][n]) {
                        values[n] = true;
                        break;
                    }
                }
            }

            //If any value is missing, the solution is incorrect
            for (int i = 1; i < 10; i++) {
                if (!values[i]) {
                    return false;
                }
            }
        }

        //Check blocks
        //For each b
        for (int refX = 0; refX < 7; refX += 3) {
            for (int refY = 0; refY < 7; refY += 3) {
                //Initialize a list of values
                boolean[] values = new boolean[10];
                for (int i = 0; i < 10; i++) {
                    values[i] = false;
                }

                //Check each point in the row for its number, add to list
                for (int x = refX; x < refX + 3; x++) {
                    for (int y = refY; y < refY + 3; y++) {
                        for (int n = 1; n < 10; n++) {
                            if (graph[x][y][n]) {
                                values[n] = true;
                                break;
                            }
                        }
                    }
                }

                //If any value is missing, the solution is incorrect
                for (int i = 1; i < 10; i++) {
                    if (!values[i]) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    ///////////////UTILITY METHODS//////////////
    //Returns the number of remaining possible values of each point
    private int[][] getConstraintMap() {

        int[][] map = new int[9][9];

        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                map[x][y] = 0;
            }
        }

        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                for (int n = 1; n < 10; n++) {
                    if (graph[x][y][n]) {
                        map[x][y] += 1;
                    }
                }
            }
        }

        return map;
    }

    private Sudoku deepClone() {

        Sudoku clone = new Sudoku();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                for (int n = 0; n < 10; n++) {
                    clone.graph[x][y][n] = this.graph[x][y][n];
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            int blockX = i / 3;
            int blockY = i % 3;

            for (int n = 0; n < 10; n++) {
                clone.row[i][n] = this.row[i][n];
                clone.column[i][n] = this.column[i][n];
                clone.block[blockX][blockY][n] = this.block[blockX][blockY][n];
            }
        }

        clone.unplayed = this.unplayed;

        return clone;
    }

    private boolean equals(Sudoku other) {
        //Check unplayed moves
        if (other.unplayed != this.unplayed) {
            return false;
        }

        //Check graph
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                for (int n = 0; n < 10; n++) {
                    if (this.graph[x][y][n] != other.graph[x][y][n]) {
                        return false;
                    }
                }
            }
        }

        //Check rows, columns, blocks
        for (int i = 0; i < 9; i++) {
            int blockX = i / 3;
            int blockY = i % 3;

            for (int n = 0; n < 10; n++) {
                if (other.row[i][n] != this.row[i][n]
                        || other.column[i][n] != this.column[i][n]
                        || other.block[blockX][blockY][n] != this.block[blockX][blockY][n]) {
                    return false;
                }
            }
        }

        return true;
    }

    //Converts the graph into a solution. Only valid if complete.
    private int[][] makeSolution() {

        int[][] solution = new int[9][9];
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                for (int n = 1; n < 10; n++) {
                    if (graph[x][y][n]) {
                        solution[x][y] = n;
                        break;
                    }
                }
            }
        }
        return solution;
    }

    private void print() {

        int[][] constraintMap = getConstraintMap();
        int[][] solution = makeSolution();

        String str = "Times used LS: " + loneSingles + "  HS: " + hiddenSingles + "  NP: " + nakedPairs + "  NT:" + nakedTriples + "\nTotal States Visited: " + visited + "  Invalid States Reached: " + failures;

        for (int y = 0; y < 9; y++) {

            if (y == 3 || y == 6) {
                str += "\n\n";
            } else {
                str += "\n";
            }

            for (int x = 0; x < 9; x++) {
                if (constraintMap[x][y] == 1) {
                    str += solution[x][y];
                } else {
                    str += "_";
                }
                if (x == 2 || x == 5) {
                    str += "  ";
                } else {
                    str += " ";
                }
            }
        }
        str += "\n\n";
        System.out.println(str);
    }

}
