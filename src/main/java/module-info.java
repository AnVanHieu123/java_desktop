module com.javaadv {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires annotations;
    requires java.sql;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.javaadv to javafx.fxml;
    exports com.javaadv;
    exports com.javaadv.Model;
   opens com.javaadv.Model to javafx.fxml;
    exports com.javaadv.Controller;
   opens com.javaadv.Controller to javafx.fxml;

}