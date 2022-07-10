import java.util.ArrayList;

public interface Listener {
	public void OnProgress(int progress);
	public void OnDataReadDone(Data data);
	public void OnCharacteristicMatrixGenerated();
	
	public void OnCalculationProgress(int progress);
	public void OnHighestSimilaritiesComputed(ArrayList<SimilarityDataHolder> highestSimilarities);
}
