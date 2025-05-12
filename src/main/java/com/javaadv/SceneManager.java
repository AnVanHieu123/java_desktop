package com.javaadv;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;



public class SceneManager {
    private static final double PREF_WIDTH = 1368;
    private static final double PREF_HEIGHT = 768;
    private static final double MIN_WIDTH = 800;
    private static final double MIN_HEIGHT = 600;
    public static void changeScene(Stage stage, String fxmlFile, String title) {

        try {


            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlFile));
            Parent root = loader.load();
            root.resize(PREF_WIDTH, PREF_HEIGHT);
            Scene scene = new Scene(root, PREF_WIDTH, PREF_HEIGHT);

            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMinWidth(MIN_WIDTH);
            stage.setMinHeight(MIN_HEIGHT);

            // Đảm bảo cửa sổ không bị tràn màn hình
            stage.sizeToScene();
            stage.centerOnScreen();

            if (!stage.isShowing()) {
                stage.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load FXML file: " + fxmlFile, e);
        }
    }
}
