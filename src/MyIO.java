import java.io.*;

public class MyIO implements Runnable {

    private String fileName = "./mtsamples.csv";
    public Listener listener;
    
    private int count = 0;
    private int maxCount = 401;

    /**
     * Returns the data from the file line by line
     *
     * @return the data from the file
     */
    public Data ReadData() {
        Data ret = new Data();

        File file = new File(fileName);
        InputStream fis = null;
        BufferedReader reader = null;
        try {
            fis = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                ret.document.add(line);
                ret.universalSet.addAll(Shingler.Shingle(line));
                count++;
                if(listener != null) {
                	int progress = (int)Math.ceil((double)count/maxCount * 100);
                	listener.OnProgress(progress);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        listener.OnDataReadDone(ret);
        return ret;
    }

	@Override
	public void run() {
		ReadData();
	}
}
