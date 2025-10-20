package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ItemRepository {
	private final Connection connection;

	public ItemRepository(Connection connection) {
		this.connection = connection;
	}

	//感情ベクトルの登録
	public boolean insertEmotion(EmotionModel emotion) {
		String sql = "INSERT INTO emotion (title, joy, sadness, anticipation, surprise, anger, fear, disgust, trust) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, emotion.getTitle());
			ps.setDouble(2, emotion.getJoy());
			ps.setDouble(3, emotion.getSadness());
			ps.setDouble(4, emotion.getAnticipation());
			ps.setDouble(5, emotion.getSurprise());
			ps.setDouble(6, emotion.getAnger());
			ps.setDouble(7, emotion.getFear());
			ps.setDouble(8, emotion.getDisgust());
			ps.setDouble(9, emotion.getTrust());

			int affectedRows = ps.executeUpdate();
			return affectedRows > 0;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	//全タイトル名取得
	public List<String> fetchAllTitles() {
		List<String> titles = new ArrayList<>();
		String sql = "SELECT title FROM emotion";

		try (PreparedStatement ps = connection.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				titles.add(rs.getString("title"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Collator collator = Collator.getInstance(Locale.JAPANESE);
		collator.setStrength(Collator.PRIMARY); // 「濁点」や「小文字」などを区別しない程度の強さ

		Collections.sort(titles, collator);
		return titles;
	}
	//オーバーロード、引数に一致するタイトルのデータ取得
	public List<EmotionModel> fetchTitleAndVector(List<String> titleList) {
		List<EmotionModel> emotions = new ArrayList<>();
		String sql = "SELECT * FROM emotion WHERE title = ?";

		PreparedStatement ps;
		ResultSet result;

		try {
			ps = connection.prepareStatement(sql);

			for (String title : titleList) {
				ps.setString(1, title);
				result = ps.executeQuery();

				if (result.next()) {
					EmotionModel e = new EmotionModel();
					e.setTitle(result.getString("title"));
					e.setJoy(result.getDouble("joy"));
					e.setSadness(result.getDouble("sadness"));
					e.setAnticipation(result.getDouble("anticipation"));
					e.setSurprise(result.getDouble("surprise"));
					e.setAnger(result.getDouble("anger"));
					e.setFear(result.getDouble("fear"));
					e.setDisgust(result.getDouble("disgust"));
					e.setTrust(result.getDouble("trust"));

					emotions.add(e);
				}
				result.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} 
		return emotions;
	}
	//オーバーロード、全データ取得
	public List<EmotionModel> fetchTitleAndVector() {
		List<EmotionModel> emotions = new ArrayList<>();
		String sql = "SELECT * FROM emotion";

		try (PreparedStatement ps = connection.prepareStatement(sql);
				ResultSet result = ps.executeQuery()) {

			while (result.next()) {
				EmotionModel e = new EmotionModel();
				e.setTitle(result.getString("title"));
				e.setJoy(result.getDouble("joy"));
				e.setSadness(result.getDouble("sadness"));
				e.setAnticipation(result.getDouble("anticipation"));
				e.setSurprise(result.getDouble("surprise"));
				e.setAnger(result.getDouble("anger"));
				e.setFear(result.getDouble("fear"));
				e.setDisgust(result.getDouble("disgust"));
				e.setTrust(result.getDouble("trust"));

				emotions.add(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return emotions;
	}
	//上位または下位のランキングリストを返す
	public List<String> searchTopOrLowItems(EmotionModel compareEmotion, List<EmotionModel> selected, Boolean isTop) {
		final int rankLimit = 10;
		Set<String> selectedTitleSet = new HashSet<>();//選択済みのタイトル集合
		List<String> resultList = new ArrayList<String>();//返すリスト
		Map<String, Double> titleAndSimilarity = new HashMap<String, Double>();//タイトルと類似度を保持
		//選択済みタイトルを作成
		for(EmotionModel e: selected) {
			selectedTitleSet.add(e.getTitle());
		}
		//DBから全ベクトル取得
		List<EmotionModel> titleAndVector = fetchTitleAndVector();

		//titleAndSimilarityに登録
		for (EmotionModel e : titleAndVector) {
			if(selectedTitleSet.contains(e.getTitle()))
				continue;

			double similarity = compareEmotion.cosineSimilarity(e);
			titleAndSimilarity.put(e.getTitle(), similarity);
		}
		//comparator定義
		Comparator<Map.Entry<String, Double>> comparator = isTop
				? Comparator.comparing(Map.Entry<String, Double>::getValue).reversed()//降順
						: Comparator.comparing(Map.Entry<String, Double>::getValue);//昇順

		// ソート・上位or下位5件を取得
		resultList = titleAndSimilarity.entrySet().stream()
				.sorted(comparator)
				.limit(rankLimit)
				.map(Map.Entry::getKey)
				.toList();

		return resultList;
	}
}
