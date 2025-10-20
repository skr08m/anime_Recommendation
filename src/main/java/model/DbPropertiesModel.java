package model;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DbPropertiesModel {
	private final String driverClassName;
	private final String url;
	private final String user;
	private final String password;

	public DbPropertiesModel() {
		Properties props = new Properties();
		try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
			if (input == null) {
				throw new RuntimeException("プロパティファイルが見つかりません");
			}
			props.load(input);
		} catch (Exception e) {
			throw new RuntimeException("プロパティの読み込みに失敗しました", e);
		}

		this.driverClassName = props.getProperty("driverClassName");
		this.url = props.getProperty("url");
		this.user = props.getProperty("user");
		this.password = props.getProperty("password");
	}
	
	public Connection getConnection() throws Exception {
        Class.forName(driverClassName);
        return DriverManager.getConnection(url, user, password);
    }
}
