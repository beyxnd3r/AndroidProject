import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Human(
    var fullName: String,
    var age: Int,
    var currentSpeed: Double
) {
    var x: Double = 0.0
    var y: Double = 0.0
    private val random = Random.Default

    // Метод движения — Random Walk
    fun move() {
        val angle = random.nextDouble(0.0, 2 * Math.PI)
        x += currentSpeed * cos(angle)
        y += currentSpeed * sin(angle)
    }

    override fun toString(): String {
        return "$fullName [$age], position: (%.2f, %.2f)".format(x, y)
    }
}

fun main() {
    val numberOfHumans = 5 // количество людей, зависит от номера в списке
    val simulationTime = 10 // время симуляции в секундах

    val humans = arrayOf(
        Human("Иванов Иван", 20, 1.5),
        Human("Петров Петр", 22, 1.3),
        Human("Сидоров Сидор", 19, 1.4),
        Human("Кузнецов Кузьма", 21, 1.6),
        Human("Морозов Мороз", 23, 1.2)
    )

    for (t in 0 until simulationTime) {
        println("Time: ${t}s")
        humans.forEach {
            it.move()
            println(it)
        }
        println("----------------------")
        Thread.sleep(1000) // пауза 1 секунда
    }
}
