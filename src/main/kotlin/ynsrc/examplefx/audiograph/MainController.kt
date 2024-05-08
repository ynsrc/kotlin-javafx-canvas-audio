package ynsrc.examplefx.audiograph

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.Pane
import java.net.URL
import java.nio.IntBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine
import kotlin.math.max
import kotlin.math.min

class MainController : Initializable {
    @FXML
    private lateinit var btnStartStop: Button

    @FXML
    private lateinit var canvas: Canvas
    private var canvasWidth = 640
    private var canvasHeight = 480
    private val isAppRunning = AtomicBoolean(false)

    private val audioFormat = AudioFormat(44100F, 16, 1, true, false)
    private var microphone: TargetDataLine = AudioSystem.getTargetDataLine(audioFormat)

    private lateinit var g: GraphicsContext
    private lateinit var image: WritableImage
    private val pixelFormat = PixelFormat.getIntArgbPreInstance()

    private var circularFloatBuffer = CircularFloatBuffer(2048)

    @FXML
    override fun initialize(url: URL?, resources: ResourceBundle?) {
        val canvasPane = canvas.parent as Pane
        canvasPane.widthProperty().addListener { _, _, newValue ->
            canvas.width = newValue.toDouble()
            canvasWidth = newValue.toInt()
            createImage()
        }
        canvasPane.heightProperty().addListener { _, _, newValue ->
            canvas.height = newValue.toDouble()
            canvasHeight = newValue.toInt()
            createImage()
        }
        g = canvas.graphicsContext2D
    }

    private fun createImage() {
        val width = canvas.widthProperty().get().toInt()
        val height = canvas.heightProperty().get().toInt()
        image = WritableImage(width, height)
        val buffer = IntBuffer.allocate(width * height)
        Arrays.fill(buffer.array(), (0xFFFFFFFF).toInt())
        image.pixelWriter.setPixels(0, 0, width, height, pixelFormat, buffer, width)
    }

    private fun appendValue(v: Float) {
        val width = image.width.toInt()
        val height = image.height.toInt()

        image.pixelWriter.setPixels(0, 0, width -1, height, image.pixelReader, 1, 0)

        val buffer = IntBuffer.allocate(height)

        val line = ((height / 2) + (height / 2) * v).toInt()

        val lineRange = IntRange(
            min(line, height / 2),
            max(line, height / 2)
        )

        for (i in 0 until height) {
            buffer.put(i, when {
                i in lineRange -> (0xFF000000).toInt()
                else -> (0xFFFFFFFF).toInt()
            })
        }

        image.pixelWriter.setPixels(width - 1, 0, 1, height, pixelFormat, buffer, 1)

        g.pixelWriter.setPixels(0, 0, width, height, image.pixelReader, 0, 0)
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
            if (image.width.toInt() != canvasWidth || image.height.toInt() != canvasHeight) {
                createImage()
            }
            microphone.open(audioFormat)
            microphone.start()
            btnStartStop.text = "Stop Sampling"
        }

        object : Thread() {
            override fun run() {
                val byteArray = ByteArray(microphone.bufferSize)
                val floatArray = FloatArray(microphone.bufferSize / 2)
                while (microphone.isOpen && isAppRunning.get()) {
                    if (microphone.available() > 0) {
                        val spp = floatArray.size / canvasWidth
                        val read = microphone.read(byteArray, 0, microphone.bufferSize)
                        for (i in 0 until read/2 - 1 step 2) {
                            if (!isAppRunning.get()) break;
                            val value = byteArray[i].toShort() * 0x100 + byteArray[i].toShort()
                            floatArray[i] = (value.toFloat() / Short.MAX_VALUE.toFloat())
                            val v = floatArray.drop(i * spp).takeIf { it.size >= spp }?.average()?.toFloat()
                            v?.let { appendValue(it) }
                        }
                    }
                }
            }
        }.start()

        isAppRunning.set(true)
    }
}