module com.javaadv {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;


    opens com.javaadv to javafx.fxml;
    exports com.javaadv;
    exports com.javaadv.Model;
    opens com.javaadv.Model to javafx.fxml;
}