# Бронирование переговорных — лабораторная №2 (вариант 10)

Дерево интервалов (BST по началу брони + max_end) на собственных узлах, Java 21, Gradle, JavaFX (OpenJFX 21).

    gradlew run        # приложение
    gradlew focusedTest # независимые проверки ключевых сценариев
    gradlew selfTest   # самопроверка структуры (случайные операции против перебора)
    gradlew bench      # таблица замеров в консоль
    gradlew race       # демонстрация гонки потоков в консоль

Данные для проверки загрузки: data/bookings.csv, data/bookings_broken.csv.
Структура: ds (структуры) · model · io · service · experiment · gui (JavaFX).
GUI общается только с BookingService; тяжёлые операции идут в фоне (javafx.concurrent.Task), обновление таблицы и дерева — тоже.

Рекомендация: запускайте `focusedTest`, затем `selfTest` и отдельно `race`.
Экспорт CSV производится через временный файл; отмена не повреждает существующие данные.
