package people;
import java.io.*;
public class MeetingRecord {


    private Person p;
    private String conv;

    public MeetingRecord(Person p, String conv) {
        this.p = p;
        this.conv = conv;
    }
    public void createFile() {
        String fileName = p.getId() + ".txt";
        boolean fileExists = new File(fileName).exists();


        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){

            if (p.getMeetingRecord() == null) {
                bw.write(p + "\n\n" + conv + "\n\n");
            } else {
                bw.write(conv + "\n\n");
            }
            bw.close();
        } catch (IOException e) {}
    }


    public void readHistory(){

        try {
            BufferedReader br = new BufferedReader(new FileReader(p.getId() + ".txt"));
            if (p.getMeetingRecord() == null) {
                return;
            }

            String line;

            while((line = br.readLine()) != null){
                System.out.println(line);
            }


        } catch (IOException e) {}

    }
}
