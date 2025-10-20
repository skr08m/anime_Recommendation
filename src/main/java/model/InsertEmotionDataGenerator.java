package model;

import java.util.Random;

public class InsertEmotionDataGenerator {//テスト用DBデータ生成関数

    public static void main(String[] args) {
        Random rand = new Random();

        for (int i = 1; i <= 100; i++) {
            double[] vec = new double[8];
            double sumSq = 0;

            // ランダムな8次元ベクトルを生成
            for (int j = 0; j < 8; j++) {
                vec[j] = rand.nextDouble();
                sumSq += vec[j] * vec[j];
            }

            // ノルムで割って正規化
            double norm = Math.sqrt(sumSq);
            for (int j = 0; j < 8; j++) {
                vec[j] /= norm;
            }

            // SQL出力
            System.out.printf(
                "INSERT INTO emotion (title, joy, sadness, anticipation, surprise, anger, fear, disgust, trust) " +
                "VALUES ('test%d', %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f);%n",
                i, vec[0], vec[1], vec[2], vec[3], vec[4], vec[5], vec[6], vec[7]
            );
        }
    }
}
