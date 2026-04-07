package cosc.brocku.ca.watchnext;
//This class stores the resulting movie or show, the score and an explaination for it to be recommended
public class ScoredRec {
    private ProfileMovieShow item;
    private double score;
    private String explanation;

    public ScoredRec(ProfileMovieShow item, double score, String explanation) {
        this.item = item;
        this.score = score;
        this.explanation = explanation;
    }

    public ProfileMovieShow getItem() {
        return item;
    }

    public double getScore() {
        return score;
    }

    public String getExplanation() {
        return explanation;
    }
}

