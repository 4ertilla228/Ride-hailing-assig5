package main.java.ridehailing.facade;

public enum ChainStageStatus {
    PENDING_B,             // Шаг А успешно сохранен, ожидается шаг B
    PENDING_C,             // Шаги А и B успешно сохранены, ожидается шаг C
    COMPLETED,             // Все шаги A > B > C успешно завершены
    COMPENSATED_A,         // Сбой на шаге B > компенсирован шаг A
    COMPENSATED_B_AND_A,   // Сбой на шаге C > компенсированы B, затем A
    REJECTED_AT_A          // Отклонено на старте
}