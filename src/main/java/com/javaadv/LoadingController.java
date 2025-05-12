package com.javaadv;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class LoadingController {
    @FXML
    private Label lblStatus;

    public void setStatusText(String text) {
        lblStatus.setText(text);
    }
}
