package searchengine.searchFolder;
import lombok.Data;
@Data
public class LemmaGear {
    private String lemma;
    private float gear;

    public LemmaGear(String lemma, float rank) {
        this.lemma = lemma;
        this.gear = rank;
    }
}
