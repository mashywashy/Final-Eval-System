module org.jah.newsys2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    opens org.jah.newsys2 to javafx.fxml;
    exports org.jah.newsys2;

    opens org.jah.newsys2.backend to javafx.base;
}