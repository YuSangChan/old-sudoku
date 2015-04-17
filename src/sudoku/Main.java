
package sudoku;

import java.util.Scanner;

public class Main {

    public static void solveHardestSudokuPuzzleInTheWorld() {
        
        Sudoku sudoku;        
        sudoku = new Sudoku("hardestinworld.txt"); //Select the hardest sudoku puzzle in the world
        long startTime = System.currentTimeMillis();
        sudoku.startSearch(4);  //Start depth-limited search. Recommend absolute depth-limit of 4
        System.out.println("Time: " + (System.currentTimeMillis() - startTime));
    }
    
    public static void runMultipuzzleTest() {
        
        
        
        for(int i = 13; i <= 20; i++) {
            
            String fileName = "17-" + i + ".txt";
            Sudoku sudoku = new Sudoku(fileName);
            System.out.println("Starting " + fileName);
            long startTime = System.currentTimeMillis();
            sudoku.startSearch(4);
            System.out.println("Time: " + (System.currentTimeMillis() - startTime));
            
        }
    }
    
    public static void main(String[] args) {
        
        //Interactive filename
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("Enter filename");
//        Sudoku sudoku = new Sudoku(scanner.nextLine());
//        sudoku.startSearch(4); //Start depth-limited search. Reccommend absolute depth-limit of 4
//        
        solveHardestSudokuPuzzleInTheWorld();
//        runMultipuzzleTest();
        
        
    }
    
    
}
