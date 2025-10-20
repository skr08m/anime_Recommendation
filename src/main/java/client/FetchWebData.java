package client;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bonigarcia.wdm.WebDriverManager;
import model.AnimeDataModel;
import model.EmotionModel;
import model.ItemRepository;

public class FetchWebData {

	private final BlockingQueue<AnimeDataModel> queue = new LinkedBlockingQueue<>();
	private final AtomicBoolean isScrapingFinished = new AtomicBoolean(false);
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ObjectMapper objectMapper = new ObjectMapper();

	ItemRepository itemRepository;

	public FetchWebData(ItemRepository itemRepository) {
		this.itemRepository = itemRepository;
	}

	// スクレイピング処理（逐次）
	public void scrapeAnimeByIdRange() throws InterruptedException {
		final int START_ID = 15114;//1がスタート、ここを変えると範囲が変わる
		final int END_ID = 15114;//15114が上限
		final int MIN_REVIEW_LENGTH = 100;
		final int MAX_REVIEW_LENGTH = 2000;

		try {
			WebDriverManager.chromedriver().setup();
		} catch (Exception e) {
			System.err.println("・WebDriverManagerセットアップ失敗: " + e.getMessage());
		}

		WebDriver driver = new ChromeDriver();

		try {
			for (int id = START_ID; id <= END_ID; id++) {
				List<AnimeDataModel> resultList = new ArrayList<>();

				String url = "https://www.anikore.jp/anime_review/" + id + "/";
				driver.get(url);
				if (isAdPage(driver)) {
					id--;
					continue;
				}

				String currentUrl = driver.getCurrentUrl();
				if (currentUrl.equals("https://www.anikore.jp/")) {
					System.out.println("🔸 ID " + id + " は存在しないためスキップ");
					continue;
				}

				WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
				WebElement titleElement = wait.until(
						ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='page-top']/section[4]/div/h1")));
				String title = titleElement.getText().trim();

				// --- 「もっと見る」ボタンを安全に連続クリック ---
				try {
					while (true) {
						List<WebElement> moreButtons = driver.findElements(By.cssSelector("button.more_review_button"));
						if (moreButtons.isEmpty()) break;

						WebElement moreButton = moreButtons.get(0);
						if (moreButton.isDisplayed() && moreButton.isEnabled()) {
							moreButton.click();
							Thread.sleep(300);
						} else {
							break;
						}
					}
				} catch (ElementClickInterceptedException e) {
					// クリックできなかった場合はループ終了
				} catch (Exception e) {
					System.out.println("「もっと見る」ボタン処理中の例外: " + e.getMessage());
				}

				List<WebElement> spoilerButtons = driver.findElements(By.cssSelector(".spoiler_expand_button"));
				for (WebElement btn : spoilerButtons) {
					if (btn.isDisplayed() && btn.isEnabled()) {
						try {
							btn.click();
						} catch (Exception ignore) {}
					}
				}

				List<WebElement> reviewBlocks = driver.findElements(
						By.cssSelector("p.m-reviewUnit_userText_content.ateval_description"));

				if (reviewBlocks.isEmpty()) {
					System.out.println("🟡 ID " + id + ": 感想なしのためスキップ");
					continue;
				}

				for (WebElement review : reviewBlocks) {
					String impression = review.getText().trim();
					//字数制限を満たすようカット
					if(impression.length() > MAX_REVIEW_LENGTH)
						impression = impression.substring(0, MAX_REVIEW_LENGTH);

					if (isValidReview(impression, MIN_REVIEW_LENGTH)) {
						AnimeDataModel data = new AnimeDataModel(title, impression);
						resultList.add(data);
						queue.offer(data); // API用キューに投入
					}
				}

				if (resultList.isEmpty()) {
					System.out.println("🟡 ID " + id + ": 有効な感想なしのためスキップ");
				} else {	
					// 作品切り替え通知として特別オブジェクトをキューに投入
					queue.offer(new AnimeDataModel("its finish", null));
					System.out.println("✅ ID " + id + ": " + title + "（レビュー登録数: " + resultList.size() + "）");
				}
			}
		} finally {
			driver.quit();
			markScrapingFinished(); // スクレイピング終了通知
		}
	}

	// 感情API送信用（1スレッドで十分）
	public void insertEmotionUsingApi() {
		Thread apiThread = new Thread(() -> {
			List<EmotionModel> currentEmotionList = new ArrayList<>();
			String currentTitle = null;

			try {
				while (!isScrapingFinished.get() || !queue.isEmpty()) {
					AnimeDataModel data = queue.poll(1, TimeUnit.SECONDS);
					if (data == null) continue;

					if ("its finish".equals(data.getTitle())) {
						// 作品の切り替わり通知を受け取ったらバッファの合成・DB登録
						if (currentTitle != null && !currentEmotionList.isEmpty()) {
							EmotionModel combined = EmotionModel.combineAndNormalize(currentEmotionList);
							combined.setTitle(currentTitle);
							itemRepository.insertEmotion(combined);
							System.out.println("🟢 感情登録: " + currentTitle);
						}
						currentEmotionList.clear();
						currentTitle = null;
					} else {
						if (currentTitle == null) {
							currentTitle = data.getTitle();
						} else if (!currentTitle.equals(data.getTitle())) {
							// タイトルが変わったら前作品を登録して切り替え
							if (!currentEmotionList.isEmpty()) {
								EmotionModel combined = EmotionModel.combineAndNormalize(currentEmotionList);
								combined.setTitle(currentTitle);
								itemRepository.insertEmotion(combined);
								System.out.println("🟢 感情登録: " + currentTitle);
							}
							currentEmotionList.clear();
							currentTitle = data.getTitle();
						}

						EmotionModel emotion = fetchEmotionFromApi(data.getImpression());
						if (emotion != null) {
							emotion.normalize();
							currentEmotionList.add(emotion);
							Thread.sleep(100);
						}
					}
				}

				// キュー空・スクレイピング終了後の未登録作品を最後に登録
				if (currentTitle != null && !currentEmotionList.isEmpty()) {
					EmotionModel combined = EmotionModel.combineAndNormalize(currentEmotionList);
					combined.setTitle(currentTitle);
					itemRepository.insertEmotion(combined);
					System.out.println("🟢 感情登録: " + currentTitle);
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		apiThread.start();
	}

	private EmotionModel fetchEmotionFromApi(String text) {
		try {
			String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
			String url = "http://shiratori.cdl.im.dendai.ac.jp:50000/?text=" + encodedText;

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.GET()
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			JsonNode root = objectMapper.readTree(response.body());
			JsonNode score = root.get("responce").get(0).get("emotion_score");

			EmotionModel em = new EmotionModel();
			em.setJoy(score.get("Joy").asDouble());
			em.setSadness(score.get("Sadness").asDouble());
			em.setAnticipation(score.get("Anticipation").asDouble());
			em.setSurprise(score.get("Surprise").asDouble());
			em.setAnger(score.get("Anger").asDouble());
			em.setFear(score.get("Fear").asDouble());
			em.setDisgust(score.get("Disgust").asDouble());
			em.setTrust(score.get("Trust").asDouble());
			return em;

		} catch (Exception e) {
			System.err.println("⚠️ API失敗: " + e.getMessage());
			return null;
		}
	}

	public void markScrapingFinished() {
		isScrapingFinished.set(true);
	}

	private boolean isValidReview(String text, int minLength) {
		return text.length() >= minLength;
	}

	private boolean isAdPage(WebDriver driver) {
		try {
			String url = driver.getCurrentUrl();
			return url.contains("#google_vignette") || url.contains("google_vignette") || url.contains("ads");
		} catch (Exception e) {
			return false;
		}
	}
}
