module com.example.mrs {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;

    requires java.sql;
    requires json.simple;
    requires mysql.connector.j;

    requires javafx.media;
    requires java.desktop;
    requires java.mail;

    opens com.example.mrs to javafx.fxml;
    exports com.example.mrs;
    exports com.example.mrs.dataModel;
}