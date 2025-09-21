import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

open class Human(
    var fullName: String,
    var age: Int,
    var currentSpeed: Double
) {
    var x: Double = 0.0
    var y: Double = 0.0
    private val random = Random.Default

    // Случайное движение — Random Walk
    open fun move() {
        val angle = random.nextDouble(0.0, 2 * Math.PI)
        x += currentSpeed * cos(angle)
        y += currentSpeed * sin(angle)
    }

    override fun toString(): String {
        return "$fullName [$age], position: (%.2f, %.2f)".format(x, y)
    }
}

// Новый класс-наследник Driver с прямолинейным движением
class Driver(
    fullName: String,
    age: Int,
    currentSpeed: Double,
    private val directionAngle: Double // направление движения
) : Human(fullName, age, currentSpeed) {

    // Прямолинейное движение
    override fun move() {
        x += currentSpeed * cos(directionAngle)
        y += currentSpeed * sin(directionAngle)
    }
}

fun main() {
    val humans = listOf(
        Human("Иванов Иван", 20, 1.5),
        Human("Петров Петр", 22, 1.3),
        Human("Сидоров Сидор", 19, 1.4)
    )
    val driver = Driver("Кузнецов Кузьма", 25, 1.8, Math.PI / 4) // 45 градусов

    val allEntities = humans + driver
    val simulationTime = 10 // секунд

    val threads = allEntities.map { entity ->
        Thread {
            for (t in 0 until simulationTime) {
                entity.move()
                println("${Thread.currentThread().name}: $entity")
                Thread.sleep(1000)
            }
        }
    }

    // Запускаем все потоки
    threads.forEach { it.start() }

    // Ждём завершения всех потоков
    threads.forEach { it.join() }
}
