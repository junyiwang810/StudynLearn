module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;
    requires org.seleniumhq.selenium.api;
    requires org.seleniumhq.selenium.chrome_driver;
    requires org.seleniumhq.selenium.remote_driver;
    requires com.google.common;

    opens com.example to javafx.fxml;
    exports com.example;
}
