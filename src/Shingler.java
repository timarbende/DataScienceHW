import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Shingler {
    private static int shingleSize = 3;
    public static void SetShingleSize(int size){ shingleSize = size; }

    /**
     * generate count-sized word-sets (shingles) from the bag (one entry of the data)
     *
     * @param bag   one entry of the data (the data of one patient)
     * @return a two-dimensional String-array where the first index is the index of the shingle and the second is the word in the shingle
     */
    public static ArrayList<String> Shingle(String bag) {
        ArrayList<String> bagArray = getBagArray(bag);
        ArrayList<String> ret = new ArrayList<>();
        StringBuilder builder;

        for (int i = 0; i + shingleSize <= bagArray.size(); i++) {
            builder = new StringBuilder();
            builder.append(bagArray.get(i));
            for (int j = 1; j < shingleSize; j++) {
                builder.append("|");
                builder.append(bagArray.get(i + j));
            }
            ret.add(builder.toString());
        }
        return ret;
    }

    /**
     * prepare a bag and return it in an array of its words
     *
     * @param bag one entry
     * @return array of the words of the bag
     */
    private static ArrayList<String> getBagArray(String bag) {
        ArrayList<String> bagArray = new ArrayList<String>(List.of(bag.split("-?[0-9.\"/,;: ?@#()%&*+!$=<>]+-?")));

        bagArray = removeStopWords(bagArray);

        /*for (int i = 0; i < bagArray.size(); i++) {
            String word = bagArray.get(i);
            bagArray.set(i, strip(word));
        }*/

        return bagArray;
    }

    private static ArrayList<String> removeStopWords(ArrayList<String> bagArray) {
        ArrayList<String> ret = new ArrayList<>();
        for(int i = 0; i < bagArray.size(); i++){
            if(!Constants.StopWords.contains(bagArray.get(i).toLowerCase(Locale.ROOT)))
                ret.add(bagArray.get(i));
        }
        return ret;
    }

    /**
     * strip striplings (defined at the end of the code) from the beginning and ending of the word
     *
     * @param word
     * @return stripped word. NULL if the whole word is made of striplings
     */
    private static String strip(String word) {
        if (word.length() == 0)
            return word;

        char[] wordChars = word.toCharArray();
        int i = 0;
        while (i < word.length() && Constants.Striplings.contains(wordChars[i]))
            i++;

        int j = word.length();
        if (Constants.Striplings.contains(wordChars[j - 1])) {
            j--;
            while (j >= 0 && Constants.Striplings.contains(wordChars[j]))
                j--;
        }

        //the whole word is striplings
        if (j == -1)
            return null;

        return word.substring(i, j);
    }
}
