package ynsrc.examplefx.audiograph

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import kotlin.system.exitProcess

class ExampleApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(ExampleApplication::class.java.getResource("main-view.fxml"))
        val scene = Scene(fxmlLoader.load())
        stage.title = "JavaFx Example Project"
        stage.scene = scene
        stage.setOnCloseRequest { exitProcess(0) }
        stage.show()
    }
}

fun main() {
    Application.launch(ExampleApplication::class.java)
}