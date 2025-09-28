import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

open class Human(
    var fullName: String,
    var age: Int,
    override var currentSpeed: Double
) : Movable {
    override var x: Double = 0.0
    override var y: Double = 0.0
    private val random = Random.Default

    // Случайное движение — Random Walk
    override fun move() {
        val angle = random.nextDouble(0.0, 2 * Math.PI)
        x += currentSpeed * cos(angle)
        y += currentSpeed * sin(angle)
    }

    override fun toString(): String {
        return "$fullName [$age], position: (%.2f, %.2f)".format(x, y)
    }
}
