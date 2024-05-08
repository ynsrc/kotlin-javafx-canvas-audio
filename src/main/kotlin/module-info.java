module ynsrc.examplefx {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;

    requires org.controlsfx.controls;
    requires java.desktop;

    opens ynsrc.examplefx.audiograph to javafx.fxml;
    exports ynsrc.examplefx.audiograph;
}