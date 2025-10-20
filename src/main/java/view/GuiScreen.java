package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import controller.GuiController;

public class GuiScreen extends JFrame {
	private GuiController controller;
	private DefaultListModel<String> titleModel;
	private DefaultListModel<String> addedModel;
	private JList<String> availableList;
	private JList<String> addedList;

	private JButton btnDbUpdate;
	private JButton btnAdd;
	private JButton btnSearch;
	private JButton btnDelete;

	private JTextArea resultArea;
	private JLabel noticeLabel;

	private JTextField searchField;

	public GuiScreen() {
		setTitle("Swing GuiScreen Sample");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setLocationRelativeTo(null);

		setLayout(new BorderLayout(10, 10));

		// --- 上部パネル ---
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout(5, 5));

		noticeLabel = new JLabel("<html><body style='width: 100%; padding:5px;'>"
				+ "<b>注意：</b>検索の際の選択について<br>"
				+ "・左のリストから項目を選択して「追加」ボタンを押してください。<br>"
				+ "・毎回の検索ごとに、なるべく似ている作品を選ぶようにしてください。<br>"
				+ "・DB更新はスクレイピングを行うため時間がかかります"
				+ "</body></html>");
		noticeLabel.setVerticalAlignment(SwingConstants.TOP);
		topPanel.add(noticeLabel, BorderLayout.NORTH);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		btnDbUpdate = new JButton("DB更新");
		btnAdd = new JButton("追加");
		btnSearch = new JButton("検索");
		btnDelete = new JButton("選択を削除");
		buttonPanel.add(btnDbUpdate);
		buttonPanel.add(btnAdd);
		buttonPanel.add(btnSearch);
		buttonPanel.add(btnDelete);
		topPanel.add(buttonPanel, BorderLayout.SOUTH);

		add(topPanel, BorderLayout.NORTH);

		// --- 中央パネル ---
		JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

		titleModel = new DefaultListModel<>();
		addedModel = new DefaultListModel<>();

		availableList = new JList<>(titleModel);
		addedList = new JList<>(addedModel);

		JScrollPane scrollAvailable = new JScrollPane(availableList);
		JScrollPane scrollAdded = new JScrollPane(addedList);

		JPanel listsPanel = new JPanel(new GridLayout(1, 2, 10, 10));

		JPanel leftPanel = new JPanel(new BorderLayout());
		JLabel lblAvailable = new JLabel("選択してください");
		lblAvailable.setHorizontalAlignment(SwingConstants.CENTER);
		leftPanel.add(lblAvailable, BorderLayout.NORTH);
		leftPanel.add(scrollAvailable, BorderLayout.CENTER);

		// 🔽 新規追加：検索欄とイベント通知
		searchField = new JTextField();
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { notifySearchTextChanged(); }
			public void removeUpdate(DocumentEvent e) { notifySearchTextChanged(); }
			public void changedUpdate(DocumentEvent e) { notifySearchTextChanged(); }
		});
		JPanel searchPanel = new JPanel(new BorderLayout());
		searchPanel.add(new JLabel("検索:"), BorderLayout.WEST);
		searchPanel.add(searchField, BorderLayout.CENTER);
		leftPanel.add(searchPanel, BorderLayout.SOUTH);

		JPanel rightPanel = new JPanel(new BorderLayout());
		JLabel lblAdded = new JLabel("選択済み");
		lblAdded.setHorizontalAlignment(SwingConstants.CENTER);
		rightPanel.add(lblAdded, BorderLayout.NORTH);
		rightPanel.add(scrollAdded, BorderLayout.CENTER);

		listsPanel.add(leftPanel);
		listsPanel.add(rightPanel);

		centerPanel.add(listsPanel, BorderLayout.CENTER);

		resultArea = new JTextArea(12, 50);
		resultArea.setEditable(false);
		resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		JScrollPane scrollResult = new JScrollPane(resultArea);

		JPanel resultPanel = new JPanel(new BorderLayout());
		JLabel lblResult = new JLabel("結果出力");
		lblResult.setHorizontalAlignment(SwingConstants.CENTER);
		resultPanel.add(lblResult, BorderLayout.NORTH);
		resultPanel.add(scrollResult, BorderLayout.CENTER);

		centerPanel.add(resultPanel, BorderLayout.SOUTH);

		add(centerPanel, BorderLayout.CENTER);

		btnAdd.addActionListener(e -> controller.onAddButtonClicked());
		btnSearch.addActionListener(e -> controller.onSearchButtonClicked());
		btnDbUpdate.addActionListener(e -> controller.onDbUpdateButtonClicked());
		btnDelete.addActionListener(e -> controller.onDeleteButtonClicked());
	}

	private void notifySearchTextChanged() {
		if (controller != null) {
			controller.onSearchTextChanged(searchField.getText());
		}
	}

	public void setResultText(List<String> topList, List<String> worstList) {
		StringBuilder sb = new StringBuilder();
		sb.append("■ 似ているもの:上位"+ topList.size()+ "件\n");
		for (int i = 1; i <= topList.size(); i++) {
			sb.append(String.format("%2d位: %s\n", i, topList.get(i - 1)));
		}
		sb.append("\n");
		sb.append("■ 似ていないもの:上位" + worstList.size()+ "件\n");
		for (int i = 1; i <= worstList.size(); i++) {
			sb.append(String.format("%2d位: %s\n", i, worstList.get(i - 1)));
		}
		resultArea.setText(sb.toString());
	}

	public void addToAddedList(String item) {
		if (!addedModel.contains(item)) {
			addedModel.addElement(item);
		}
	}

	public void removeSelectedByAddedList() {
		String selectedItem = addedList.getSelectedValue();
		if (selectedItem != null) {
			addedModel.removeElement(selectedItem);
		}
	}

	public void setAllTitle(List<String> dataList) {
		titleModel.clear();
		for (String title : dataList) {
			titleModel.addElement(title);
		}
	}

	public void setController(GuiController controller) {
		this.controller = controller;
	}

	public List<String> getAddedItems() {
		List<String> items = new ArrayList<>();
		for (int i = 0; i < addedModel.getSize(); i++) {
			items.add(addedModel.getElementAt(i));
		}
		return items;
	}

	public String getSelectedAvailableItem() {
		return availableList.getSelectedValue();
	}
}
