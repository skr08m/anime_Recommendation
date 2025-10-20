package controller;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import client.FetchWebData;
import model.EmotionModel;
import model.ItemRepository;
import view.GuiScreen;

public class GuiController {
	private GuiScreen view;
	private ItemRepository repository;

	private List<String> allTitles = new ArrayList<>();

	public GuiController(GuiScreen view, Connection conn) {
		this.view = view;
		this.repository = new ItemRepository(conn);
	}

	// データを読み込んでViewに渡す
	public void loadItemsToView() {
		allTitles = repository.fetchAllTitles();
		view.setAllTitle(allTitles);
		System.out.println("行数 :" + allTitles.size());
	}

	//追加ボタンが押された時
	public void onAddButtonClicked() {
		String selected = view.getSelectedAvailableItem();
		if (selected != null) {
			view.addToAddedList(selected);
		}
	}

	//検索ボタンが押されたとき
	public void onSearchButtonClicked() {
		List<String> addedItems = view.getAddedItems();
		if (addedItems.isEmpty())
			return;

		List<EmotionModel> fitsEmotions = repository.fetchTitleAndVector(addedItems);
		EmotionModel compositeEmotionVector = EmotionModel.combineAndNormalize(fitsEmotions);

		List<String> top = repository.searchTopOrLowItems(compositeEmotionVector, fitsEmotions, true);
		List<String> low = repository.searchTopOrLowItems(compositeEmotionVector, fitsEmotions, false);

		view.setResultText(top, low);
	}

	public void onDbUpdateButtonClicked() {
		FetchWebData fetcher = new FetchWebData(repository);
		// API送信用スレッド起動
		fetcher.insertEmotionUsingApi();
		// 感想収集とキュー投入
		try {
			fetcher.scrapeAnimeByIdRange();
		} catch (InterruptedException e) {
			System.err.println("スクレイピング中断: " + e.getMessage());
		}
	}

	//削除ボタンの動作
	public void onDeleteButtonClicked() {
		view.removeSelectedByAddedList();
	}

	// 検索欄入力に応じて availableList を絞り込む
	public void onSearchTextChanged(String query) {
		query = query.trim().toLowerCase();
		List<String> filtered = new ArrayList<>();
		for (String title : allTitles) {
			if (title.toLowerCase().contains(query)) {
				filtered.add(title);
			}
		}
		view.setAllTitle(filtered);  // GUI側へ反映
	}
}
