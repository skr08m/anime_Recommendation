package main;

import java.sql.Connection;

import javax.swing.SwingUtilities;

import controller.GuiController;
import model.DbPropertiesModel;
import view.GuiScreen;

public class Main {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			Connection conn;
			
			try {
				DbPropertiesModel prop = new DbPropertiesModel();
				conn = prop.getConnection();
				
				GuiScreen view = new GuiScreen();
				GuiController controller = new GuiController(view, conn);
				
				view.setController(controller);
				controller.loadItemsToView();
				view.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
				
		});
		//long maxMemory = Runtime.getRuntime().maxMemory();
		//System.out.println("Max heap size: " + (maxMemory / 1024 / 1024) + " MB");
	}
}
