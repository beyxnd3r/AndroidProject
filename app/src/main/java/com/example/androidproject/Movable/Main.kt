fun main() {
    val humans = listOf(
        Human("Иванов Иван", 20, 1.5),
        Human("Петров Петр", 22, 1.3),
        Human("Сидоров Сидор", 19, 1.4)
    )
    val driver = Driver("Кузнецов Кузьма", 25, 1.8, Math.PI / 4) // 45 градусов

    val allEntities = humans + driver
    val simulationTime = 10

    val threads = allEntities.map { entity ->
        Thread {
            for (t in 0 until simulationTime) {
                entity.move()
                println("${Thread.currentThread().name}: $entity")
                Thread.sleep(1000)
            }
        }
    }

    threads.forEach { it.start() }
    threads.forEach { it.join() }
}
