import kotlin.math.cos
import kotlin.math.sin

class Driver(
    fullName: String,
    age: Int,
    currentSpeed: Double,
    private val directionAngle: Double
) : Human(fullName, age, currentSpeed) {


    override fun move() {
        x += currentSpeed * cos(directionAngle)
        y += currentSpeed * sin(directionAngle)
    }
}
