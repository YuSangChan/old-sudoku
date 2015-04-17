
package sudoku;

public class Pair {
    
    //Coordinates
    public int x1;
    public int y1;
    public int x2;
    public int y2;
    
    //Values
    public int n1 = 0;
    public int n2 = 0;
    
    public Pair(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    public void print() {
        System.out.print("Pair: " + x1 + "," + y1 + ";" + x2 + "," + y2 + "; "+ n1 + "," + n2);
    }
    
}
