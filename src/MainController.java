import java.util.*;

public class MainController implements Runnable{

	public Listener listener;
	public int threadMode = 0;
	public int similaritiesCount = 0;

    //parameters
    int bands = 20;       // or stages
    int buckets = 20;    // the number of buckets used by the hash function
    int n;               // the number of rows of the characteristic matrix ( = count of all the elements possible)
	
    private CharacteristicMatrix characteristicMatrix;
    private LSHMinHash lsh;
    private Data data;
    public void setData(Data data) {
    	this.data = data;
    	n = data.universalSet.size();
    	lsh = new LSHMinHash(bands, buckets, n);
    }
    
    public void generateCharacteristicMatrix() {
    	characteristicMatrix = new CharacteristicMatrix();
        for (int i = 0; i < data.document.size(); i++) {
            characteristicMatrix.addVector(generateCharacteristicVector(data.document.get(i)));

            if(listener != null) {
            	int progress = (int)Math.ceil((double)i/data.document.size() * 100);
            	listener.OnProgress(progress);
            }
        }
        listener.OnCharacteristicMatrixGenerated();
    }

    private boolean[] generateCharacteristicVector(String bag) {
        boolean[] ret = new boolean[data.universalSet.size()];
        ArrayList<String> shingles = Shingler.Shingle(bag);
        var iterator = data.universalSet.iterator();
        for (int i = 0; i < data.universalSet.size() && iterator.hasNext(); i++)
            ret[i] = shingles.contains(iterator.next());
        return ret;
    }
    
    //uses LSH
    public Double estimateSimilarity(String bag1, String bag2) {
    	boolean[] characteristicVector1 = generateCharacteristicVector(bag1);
    	boolean[] characteristicVector2 = generateCharacteristicVector(bag2);

    	int[] hashes1 = lsh.hash(characteristicVector1);
    	int[] hashes2 = lsh.hash(characteristicVector2);
    	
    	int matches = 0;
    	for(int i = 0; i < hashes1.length; i++) {
    		if(hashes1[i] == hashes2[i])
    			matches++;
    	}
    	
    	return (double)matches / buckets;
    }

    public void getMaxEstimate(){
        SimilarityDataHolder ret = new SimilarityDataHolder();

        int[][] hashes = new int[data.document.size()][];   // the bucket indexes that we get from LSH
        for (int i = 0; i < data.document.size(); i++) {
            boolean[] vector = characteristicMatrix.getVector(i);
            hashes[i] = lsh.hash(vector);
        }

        for(int i = 0; i < hashes.length; i++){
            for(int j = i + 1; j < hashes.length; j++) {
                int matches = 0;
                for(int k = 0; k < bands; k++)     //the size of the hashes vector equals to the number of bands, since each band gets hashed once during LSH
                    matches += hashes[i][k] == hashes[j][k] ? 1 : 0;

                double estimate = (double)matches / buckets;
                if(estimate > ret.similarity){
                    ret.similarity = estimate;
                    ret.i = i;
                    ret.j = j;
                }
                
                listener.OnCalculationProgress((int)Math.ceil((double)i / hashes.length * 100));
            }
        }
        
        ArrayList<SimilarityDataHolder> retList = new ArrayList<SimilarityDataHolder>();
        retList.add(ret);
        listener.OnHighestSimilaritiesComputed(retList);
    }

    public void getHighestEstimates(int count){
        ArrayList<SimilarityDataHolder> ret = new ArrayList<>();
        for(int i = 0; i < count; i++)
            ret.add(new SimilarityDataHolder());

        int[][] hashes = new int[data.document.size()][];   // the bucket indexes that we get from LSH
        for (int i = 0; i < data.document.size(); i++) {
            boolean[] vector = characteristicMatrix.getVector(i);
            hashes[i] = lsh.hash(vector);
        }

        for(int i = 0; i < hashes.length; i++){
            for(int j = i + 1; j < hashes.length; j++) {
                int matches = 0;
                for(int k = 0; k < bands; k++)     //the size of the hashes vector equals to the number of bands, since each band gets hashed once during LSH
                    matches += hashes[i][k] == hashes[j][k] ? 1 : 0;

                double estimate = (double)matches / buckets;
               for(int l = 0; l < count; l++){
                   if(tryInsertItemToIndex(ret, l, estimate, i, j)) {
                       ret.remove(ret.size() -1 );
                       break;
                   }
               }
               
               listener.OnCalculationProgress((int)Math.ceil((double)i / hashes.length * 100));
            }
        }
        listener.OnHighestSimilaritiesComputed(ret);
    }

    private boolean tryInsertItemToIndex(ArrayList<SimilarityDataHolder> list, int index, double estimate, int i, int j){
        if(estimate > list.get(index).similarity){
            SimilarityDataHolder newItem = new SimilarityDataHolder();
            newItem.similarity = estimate;
            newItem.i = i;
            newItem.j = j;
            list.add(index, newItem);
            return true;
        }
        return false;
    }

    public Double computeExactSimilarity(String bag1, String bag2) {
        boolean[] characteristicVector1 = generateCharacteristicVector(bag1);
        boolean[] characteristicVector2 = generateCharacteristicVector(bag2);

        return MinHash.jaccardIndex(characteristicVector1, characteristicVector2);
    }

    public void getMaxJaccard(){
        SimilarityDataHolder ret = new SimilarityDataHolder();

        for(int i = 0; i < characteristicMatrix.getSize(); i++){
            for(int j = i + 1; j < characteristicMatrix.getSize(); j++) {
                double jaccard = MinHash.jaccardIndex(characteristicMatrix.getVector(i), characteristicMatrix.getVector(j));
                if(jaccard > ret.similarity){
                    ret.similarity = jaccard;
                    ret.i = i;
                    ret.j = j;
                }
            }
            listener.OnCalculationProgress((int)Math.ceil((double)i / characteristicMatrix.getSize() * 100));
        }
        ArrayList<SimilarityDataHolder> retList = new ArrayList<SimilarityDataHolder>();
        retList.add(ret);
        listener.OnHighestSimilaritiesComputed(retList);
    }
    
    public void getHighestJaccards(int count){
    	ArrayList<SimilarityDataHolder> ret = new ArrayList<>();
        for(int i = 0; i < count; i++)
            ret.add(new SimilarityDataHolder());

        for(int i = 0; i < characteristicMatrix.getSize(); i++){
            for(int j = i + 1; j < characteristicMatrix.getSize(); j++) {
            	double jaccard = MinHash.jaccardIndex(characteristicMatrix.getVector(i), characteristicMatrix.getVector(j));
            	for(int l = 0; l < count; l++){
            		if(tryInsertItemToIndex(ret, l, jaccard, i, j)) {
            			ret.remove(ret.size() -1 );
            			break;
            		}
            	}
            }
            listener.OnCalculationProgress((int)Math.ceil((double)i / characteristicMatrix.getSize() * 100));
        }
        listener.OnHighestSimilaritiesComputed(ret);
    }

	@Override
	public void run() {
    	if(characteristicMatrix == null)
    		generateCharacteristicMatrix();
        switch (threadMode){
            case 0:
                break;
            case 1:
            	getMaxEstimate();
            	break;
            case 2:
                getHighestEstimates(similaritiesCount);
                break;
            case 3:
            	getMaxJaccard();
            	break;
            case 4:
            	getHighestJaccards(similaritiesCount);
            	break;
        }
	}
}
