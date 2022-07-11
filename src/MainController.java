import java.util.*;

public class MainController implements Runnable {

    public Listener listener;
    public int threadMode = 0;
    public int similaritiesCount = 0;

    //parameters
    int bands = 20;       // or stages
    int bucketCount = 80;    // the number of buckets used by the hash function
    int n;               // the number of rows of the characteristic matrix ( = count of all the elements possible)

    private CharacteristicMatrix characteristicMatrix;
    ArrayList<HashMap<Integer, ArrayList<Integer>>> listOfBuckets;
    private LSHMinHash lsh;
    private Data data;

    public void setData(Data data) {
        this.data = data;
        n = data.universalSet.size();
        lsh = new LSHMinHash(bands, bucketCount, n);
    }

    /*public static void main(String[] args){
        System.out.println("Reading in data file...");
        MyIO myIO = new MyIO();
        Data _data = myIO.ReadData();
        int _n = _data.universalSet.size();
        LSHMinHash _lsh = new LSHMinHash(20, 70, _n);

        System.out.println("Generating characteristic matrix...");
        CharacteristicMatrix _characteristicMatrix = new CharacteristicMatrix();
        for (int i = 0; i < _data.document.size(); i++) {
            boolean[] ret = new boolean[_data.universalSet.size()];
            ArrayList<String> shingles = Shingler.Shingle(_data.document.get(i));
            var iterator = _data.universalSet.iterator();
            for (int j = 0; j < _data.universalSet.size() && iterator.hasNext(); j++)
                ret[j] = shingles.contains(iterator.next());

            _characteristicMatrix.addVector(ret);
            System.out.println((int)Math.ceil((double)i/_characteristicMatrix.getSize() * 100));
        }

        System.out.println("Hashing...");
        int[][] hashes = new int[_characteristicMatrix.getSize()][];
        HashMap<Integer, ArrayList<Integer>> buckets = new HashMap<Integer, ArrayList<Integer>>();
        for(int i = 0; i < _characteristicMatrix.getSize(); i++) {
            hashes[i] = _lsh.hash(_characteristicMatrix.getVector(i));
            for(int j = 0; j < hashes[i].length; j++) {
                buckets.computeIfAbsent(hashes[i][j], k -> new ArrayList<Integer>());
                buckets.get(hashes[i][j]).add(i);
            }

            System.out.println((int)Math.ceil((double)i/_characteristicMatrix.getSize() * 100));
        }

        System.out.println("Results:");
        for (var entry : buckets.entrySet()) {
            System.out.print("Bucket #" + entry.getKey() + " : ");
            for(Integer setIndex: entry.getValue()){
                System.out.print(setIndex + "\t");
            }
            System.out.println();
        }
    }*/

    public void generateCharacteristicMatrix() {
        characteristicMatrix = new CharacteristicMatrix();
        for (int i = 0; i < data.document.size(); i++) {
            characteristicMatrix.addVector(generateCharacteristicVector(data.document.get(i)));

            if (listener != null) {
                int progress = (int) Math.ceil((double) i / data.document.size() * 100);
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

    public boolean concludeSimilarity(String bag1, String bag2) {
        boolean[] characteristicVector1 = generateCharacteristicVector(bag1);
        boolean[] characteristicVector2 = generateCharacteristicVector(bag2);

        int[] hashes1 = lsh.hash(characteristicVector1);
        int[] hashes2 = lsh.hash(characteristicVector2);

        for (int i = 0; i < hashes1.length; i++) {
            if (hashes1[i] == hashes2[i])
                return true;
        }

        return false;
    }

    //uses LSH
    public Double estimateSimilarity(String bag1, String bag2) {
        boolean[] characteristicVector1 = generateCharacteristicVector(bag1);
        boolean[] characteristicVector2 = generateCharacteristicVector(bag2);

        int[] hashes1 = lsh.hash(characteristicVector1);
        int[] hashes2 = lsh.hash(characteristicVector2);

        int matches = 0;
        for (int i = 0; i < hashes1.length; i++) {
            if (hashes1[i] == hashes2[i])
                matches++;
        }

        return (double) matches / bucketCount;
    }

    public void getMaxEstimate() {
        SimilarityDataHolder ret = new SimilarityDataHolder();

        if(listOfBuckets == null)
            generateHashMaps();

        for(HashMap<Integer, ArrayList<Integer>> buckets : listOfBuckets) {
            double progress = 0;
            for (var entry : buckets.entrySet()) {
                ArrayList<Integer> bagIndexes = entry.getValue();
                for (int i = 0; i < bagIndexes.size(); i++) {
                    for (int j = i + 1; j < bagIndexes.size(); j++) {

                        double similarity = MinHash.jaccardIndex(
                                characteristicMatrix.getVector(bagIndexes.get(i)),
                                characteristicMatrix.getVector(bagIndexes.get(j)));

                        if (similarity > ret.similarity) {
                            ret.similarity = similarity;
                            ret.i = bagIndexes.get(i);
                            ret.j = bagIndexes.get(j);
                        }
                    }
                }
            }
            progress += 1;
            listener.OnCalculationProgress((int) Math.ceil(progress / listOfBuckets.size() * 100));
        }

        ArrayList<SimilarityDataHolder> retList = new ArrayList<SimilarityDataHolder>();
        retList.add(ret);
        listener.OnSimilarPairsComputed(retList);
    }

    public void getEstimatedPairs(int count) {
        ArrayList<SimilarityDataHolder> ret = new ArrayList<>();

        if(listOfBuckets == null)
            generateHashMaps();

        for(HashMap<Integer, ArrayList<Integer>> buckets : listOfBuckets) {
            Iterator<Map.Entry<Integer, ArrayList<Integer>>> setIterator = buckets.entrySet().iterator();
            while (ret.size() < count) {
                if (setIterator.hasNext()) {
                    ArrayList<Integer> bagIndexes = setIterator.next().getValue();
                    for (int i = 0; i < bagIndexes.size() && ret.size() < count; i++) {
                        for (int j = i + 1; j < bagIndexes.size() && ret.size() < count; j++) {
                            SimilarityDataHolder dataHolder = new SimilarityDataHolder();
                            int indexI = bagIndexes.get(i);
                            int indexJ = bagIndexes.get(j);
                            dataHolder.i = indexI;
                            dataHolder.j = indexJ;
                            dataHolder.similarity = MinHash.jaccardIndex(
                                    characteristicMatrix.getVector(indexI),
                                    characteristicMatrix.getVector(indexJ));
                            ret.add(dataHolder);

                            listener.OnCalculationProgress((int) Math.ceil((double) ret.size() / count * 100));
                        }
                    }
                }
            }
        }

        listener.OnSimilarPairsComputed(ret);
    }

    /**
     * generates the hashmaps / list of buckets, that contain a different HashMap of buckets for each band
     * of the signature, and each bucket contains all the bags/documents/entries that would be hashed there
     */
    private void generateHashMaps(){
        /*
        Data structure: list (A) of hashmaps where one value is a list (B) of document/bag indexes
        the hashmap is just the buckets, where each bucket has its key and contains a list (B) of the indexes of the documents,
            that hash into that bucket
        list (A) holds a different map of buckets for each band of the signatures
        */
        listOfBuckets = new ArrayList<HashMap<Integer, ArrayList<Integer>>>();
        for(int i = 0; i < bands; i++)
            listOfBuckets.add(new HashMap<Integer, ArrayList<Integer>>());

        for (int i = 0; i < data.document.size(); i++) {
            boolean[] vector = characteristicMatrix.getVector(i);
            int[] hash = lsh.hash(vector);

            for (int j = 0; j < hash.length; j++) { // j iterates through the hash values (= bucket indexes)
                HashMap<Integer, ArrayList<Integer>> buckets = listOfBuckets.get(j);
                if(buckets.get(hash[j]) == null)
                    buckets.put(hash[j], new ArrayList<>());

                buckets.get(hash[j]).add(i);
            }
            listener.OnCalculationProgress((int) Math.ceil((double) i / data.document.size() * 100));
        }
    }

    private boolean tryInsertItemToIndex(ArrayList<SimilarityDataHolder> list, int index, double estimate, int i, int j) {
        if (estimate > list.get(index).similarity) {
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

    public void getMaxJaccard() {
        SimilarityDataHolder ret = new SimilarityDataHolder();

        for (int i = 0; i < characteristicMatrix.getSize(); i++) {
            for (int j = i + 1; j < characteristicMatrix.getSize(); j++) {
                double jaccard = MinHash.jaccardIndex(characteristicMatrix.getVector(i), characteristicMatrix.getVector(j));
                if (jaccard > ret.similarity) {
                    ret.similarity = jaccard;
                    ret.i = i;
                    ret.j = j;
                }
            }
            listener.OnCalculationProgress((int) Math.ceil((double) i / characteristicMatrix.getSize() * 100));
        }
        ArrayList<SimilarityDataHolder> retList = new ArrayList<SimilarityDataHolder>();
        retList.add(ret);
        listener.OnSimilarPairsComputed(retList);
    }

    public void getHighestJaccards(int count) {
        ArrayList<SimilarityDataHolder> ret = new ArrayList<>();
        for (int i = 0; i < count; i++)
            ret.add(new SimilarityDataHolder());

        for (int i = 0; i < characteristicMatrix.getSize(); i++) {
            for (int j = i + 1; j < characteristicMatrix.getSize(); j++) {
                double jaccard = MinHash.jaccardIndex(characteristicMatrix.getVector(i), characteristicMatrix.getVector(j));
                for (int l = 0; l < count; l++) {
                    if (tryInsertItemToIndex(ret, l, jaccard, i, j)) {
                        ret.remove(ret.size() - 1);
                        break;
                    }
                }
            }
            listener.OnCalculationProgress((int) Math.ceil((double) i / characteristicMatrix.getSize() * 100));
        }
        listener.OnSimilarPairsComputed(ret);
    }

    @Override
    public void run() {
        if (characteristicMatrix == null)
            generateCharacteristicMatrix();
        switch (threadMode) {
            case 0:
                break;
            case 1:
                getMaxEstimate();
                break;
            case 2:
                getEstimatedPairs(similaritiesCount);
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
