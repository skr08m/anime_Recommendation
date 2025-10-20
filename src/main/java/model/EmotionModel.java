package model;

import java.util.List;

public class EmotionModel {
    private String title;
    private double joy;
    private double sadness;
    private double anticipation;
    private double surprise;
    private double anger;
    private double fear;
    private double disgust;
    private double trust;

    // Setter
    public void setTitle(String title) {
        this.title = title;
    }
    public void setJoy(double joy) {
        this.joy = joy;
    }
    public void setSadness(double sadness) {
        this.sadness = sadness;
    }
    public void setAnticipation(double anticipation) {
        this.anticipation = anticipation;
    }
    public void setSurprise(double surprise) {
        this.surprise = surprise;
    }
    public void setAnger(double anger) {
        this.anger = anger;
    }
    public void setFear(double fear) {
        this.fear = fear;
    }
    public void setDisgust(double disgust) {
        this.disgust = disgust;
    }
    public void setTrust(double trust) {
        this.trust = trust;
    }

    // Getter
    public String getTitle() {
        return title;
    }
    public double getJoy() {
        return joy;
    }
    public double getSadness() {
        return sadness;
    }
    public double getAnticipation() {
        return anticipation;
    }
    public double getSurprise() {
        return surprise;
    }
    public double getAnger() {
        return anger;
    }
    public double getFear() {
        return fear;
    }
    public double getDisgust() {
        return disgust;
    }
    public double getTrust() {
        return trust;
    }

    // 正規化
    public void normalize() {
        double norm = Math.sqrt(
            joy * joy + sadness * sadness + anticipation * anticipation +
            surprise * surprise + anger * anger + fear * fear +
            disgust * disgust + trust * trust
        );
        if (norm == 0) return;

        joy /= norm;
        sadness /= norm;
        anticipation /= norm;
        surprise /= norm;
        anger /= norm;
        fear /= norm;
        disgust /= norm;
        trust /= norm;
    }

    // コサイン類似度
    public double cosineSimilarity(EmotionModel other) {
        return
            joy * other.joy +
            sadness * other.sadness +
            anticipation * other.anticipation +
            surprise * other.surprise +
            anger * other.anger +
            fear * other.fear +
            disgust * other.disgust +
            trust * other.trust;
    }

    // 合成して正規化
    public static EmotionModel combineAndNormalize(List<EmotionModel> list) {
        EmotionModel result = new EmotionModel();
        for (EmotionModel e : list) {
            result.joy += e.joy;
            result.sadness += e.sadness;
            result.anticipation += e.anticipation;
            result.surprise += e.surprise;
            result.anger += e.anger;
            result.fear += e.fear;
            result.disgust += e.disgust;
            result.trust += e.trust;
        }
        result.normalize();
        return result;
    }
}
