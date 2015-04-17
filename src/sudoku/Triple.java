
package sudoku;


public class Triple {
    
    //Coordinates
    public int x1;
    public int y1;
    public int x2;
    public int y2;
    public int x3;
    public int y3;
    
    //Values
    public int n1 = 0;
    public int n2 = 0;
    public int n3 = 0;
    
    
    public Triple(int x1, int y1, int x2, int y2, int x3, int y3) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.x3 = x3;
        this.y3 = y3;
    }
}
