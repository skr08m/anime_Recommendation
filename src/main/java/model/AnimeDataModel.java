package model;

public class AnimeDataModel {
	private String title;
    private String impression;

    public AnimeDataModel(String title, String impression) {
        this.title = title;
        this.impression = impression;
    }
    
    public String getTitle() {
        return title;
    }

    public String getImpression() {
        return impression;
    }
}
