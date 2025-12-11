package model

import java.time.Instant
import java.time.ZoneId

// 이벤트 타입을 안전하게 관리하기 위한 Enum
enum class EventType(val value: String) {
    VIEW("view"),
    ADD_TO_CART("addtocart"),
    TRANSACTION("transaction"),
    UNKNOWN("unknown");

    companion object {
        fun from(value: String): EventType = entries.find { it.value == value } ?: UNKNOWN
    }
}

// events.csv의 한 줄을 표현하는 객체
data class Event(
    val timestamp: Long,
    val visitorId: Int,
    val type: EventType,
    val itemId: Int?,
    val transactionId: Int?
) {
    // [수정] Companion object로 한 번만 로딩해서 재사용 (성능 대폭 향상)
    companion object {
        private val SYSTEM_ZONE = ZoneId.systemDefault()
    }

    val hour: Int
        get() = Instant.ofEpochMilli(timestamp)
            .atZone(SYSTEM_ZONE) // 여기서 상수 사용
            .hour
}