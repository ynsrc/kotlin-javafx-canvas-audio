package ynsrc.examplefx.audiograph

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine
import kotlin.math.abs

class MainController : Initializable {
    @FXML
    private lateinit var btnStartStop: Button

    @FXML
    private lateinit var label: Label

    @FXML
    private lateinit var canvas: Canvas
    private lateinit var g: GraphicsContext
    private var canvasWidth = 640
    private var canvasHeight = 480

    private val isAppRunning = AtomicBoolean(false)

    private val audioFormat = AudioFormat(44100F, 16, 1, true, false)
    private var microphone: TargetDataLine = AudioSystem.getTargetDataLine(audioFormat)
    private var circularShortBuffer = CircularShortBuffer(canvasWidth)

    private var minValue: Short = 0
    private var maxValue: Short = 0
    private var meanValue: Double = 0.0

    @FXML
    override fun initialize(url: URL?, resources: ResourceBundle?) {
        val canvasPane = canvas.parent as Pane
        canvasPane.widthProperty().addListener { _, _, newValue ->
            canvas.width = newValue.toDouble()
            canvasWidth = newValue.toInt()
        }
        canvasPane.heightProperty().addListener { _, _, newValue ->
            canvas.height = newValue.toDouble()
            canvasHeight = newValue.toInt()
        }
        g = canvas.graphicsContext2D
    }

    @FXML
    private fun onStartStopButtonClick() {
        if (microphone.isOpen || isAppRunning.get()) {
            isAppRunning.set(false)
            microphone.stop()
            microphone.close()
            btnStartStop.text = "Start Sampling"
            return
        } else {
            if (circularShortBuffer.size() != canvasWidth) {
                circularShortBuffer = CircularShortBuffer(canvasWidth)
            }
            microphone.open(audioFormat)
            microphone.start()
            btnStartStop.text = "Stop Sampling"
        }

        object : Thread() {
            override fun run() {
                val byteArray = ByteArray(microphone.bufferSize)
                while (microphone.isOpen && isAppRunning.get()) {
                    if (microphone.available() > 0) {
                        val read = microphone.read(byteArray, 0, microphone.bufferSize)
                        for (i in 0 until read/2 - 1 step 2) {
                            if (!isAppRunning.get()) break;
                            val value = (byteArray[i].toShort() * 0x100 + byteArray[i].toShort()).toShort()
                            circularShortBuffer.put(value)
                        }
                    }
                }
            }
        }.start()

        isAppRunning.set(true)

        object : Thread() {
            override fun run() {
                var x = 0
                g.fill = Color.WHITE
                var counter = 1.0
                while (isAppRunning.get()) {
                    if (circularShortBuffer.available() > 0) {
                        val value = circularShortBuffer.get()
                        minValue = minOf(value, minValue)
                        maxValue = maxOf(value, maxValue)
                        meanValue += value / counter++
                        if (++x >= canvasWidth) x = 0
                        g.fillRect(x.mod(canvasWidth).toDouble(), 0.0, 1.0, canvasHeight.toDouble())
                        val normalizedValue = (value.toFloat() / Short.MAX_VALUE)
                        val h = abs((canvasHeight / 2.0) * normalizedValue)
                        g.strokeLine(x.toDouble(), canvasHeight / 2.0 - h, x.toDouble(), canvasHeight / 2.0 + h)
                    }
                }
            }
        }.start()

        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Platform.runLater {
                    label.textProperty().set("Min: $minValue, Max: $maxValue, Mean: $meanValue")
                }
            }
        }, 0, 1000)
    }
}