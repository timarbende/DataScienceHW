import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

public class CharacteristicMatrix {
    private ArrayList<boolean[]> matrix = new ArrayList<>();

    public final boolean[] getVector(int index){
        return matrix.get(index);
    }

    public final int getSize(){return matrix.size();}

    public void addVector(boolean[] vector){
        matrix.add(vector);
    }

    public void print(){
        if(matrix.size() == 0)
            return;

        int vectorLength = matrix.get(0).length;

        for(int i = 0; i < vectorLength; i++) {
            for (int j = 0; j < matrix.size(); j++) {
                System.out.print(matrix.get(j)[i] + "\t");
            }
            System.out.println();
        }
    }

    public void printWithUniversalSet(TreeSet<String> universalSet){
        if(matrix.size() == 0)
            return;

        int vectorLength = matrix.get(0).length;
        Iterator setIterator = universalSet.iterator();

        for(int i = 0; i < vectorLength && setIterator.hasNext(); i++) {
            System.out.print(setIterator.next() + "\t\t\t\t\t");

            int trueCounter = 0;
            for (int j = 0; j < matrix.size(); j++) {
                System.out.print(matrix.get(j)[i] + "\t");
                trueCounter += matrix.get(j)[i] ? 1 : 0;
            }
            System.out.println(trueCounter);
        }
    }
}
