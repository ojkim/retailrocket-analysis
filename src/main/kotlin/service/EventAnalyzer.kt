package service

import model.Event
import model.EventType
import java.io.File

data class AnalysisResult(
    val totalEvents: Int,
    val typeCounts: Map<EventType, Int>,
    val categoryViews: Map<Int, Int>,
    val categorySales: Map<Int, Int>,
    val hourlyTraffic: Map<Int, Int>,
    val hourlySales: Map<Int, Int>
)

class EventAnalyzer(
    private val itemCategoryMap: Map<Int, Int>,
    private val categoryTree: Map<Int, Int> // 미사용 경고는 무시하셔도 됩니다 (확장성 고려)
) {

    // 내부 집계를 위한 가변(Mutable) 헬퍼 클래스
  .
    private class AnalysisAccumulator {
        var totalEvents = 0
        val typeCounts = mutableMapOf<EventType, Int>()
        val categoryViews = mutableMapOf<Int, Int>()
        val categorySales = mutableMapOf<Int, Int>()
        val hourlyTraffic = mutableMapOf<Int, Int>()
        val hourlySales = mutableMapOf<Int, Int>()

        // 데이터를 한 건 추가하는 함수
        fun accumulate(event: Event, categoryId: Int?): AnalysisAccumulator {
            totalEvents++

            // 1. 타입 카운트
            typeCounts[event.type] = typeCounts.getOrDefault(event.type, 0) + 1

            // 2. 시간대별 트래픽
            hourlyTraffic[event.hour] = hourlyTraffic.getOrDefault(event.hour, 0) + 1

            // 3. 조건부 로직
            when (event.type) {
                EventType.VIEW -> {
                    if (categoryId != null) {
                        categoryViews[categoryId] = categoryViews.getOrDefault(categoryId, 0) + 1
                    }
                }
                EventType.TRANSACTION -> {
                    hourlySales[event.hour] = hourlySales.getOrDefault(event.hour, 0) + 1
                    if (categoryId != null) {
                        categorySales[categoryId] = categorySales.getOrDefault(categoryId, 0) + 1
                    }
                }
                else -> {}
            }
            return this // 변경된 자기 자신을 반환하여 fold를 이어감
        }

        // 최종 결과를 불변 객체(AnalysisResult)로 변환
        fun toResult(): AnalysisResult {
            return AnalysisResult(
                totalEvents = totalEvents,
                typeCounts = typeCounts.toMap(),
                categoryViews = categoryViews.toMap(),
                categorySales = categorySales.toMap(),
                hourlyTraffic = hourlyTraffic.toMap(),
                hourlySales = hourlySales.toMap()
            )
        }
    }

    fun analyze(filePath: String): AnalysisResult {
        val file = File(filePath)
        println("🚀 CEO 보고를 위한 심층 데이터 분석 시작: ${file.name}")

        return file.useLines { lines ->
            lines.drop(1)
                // 1. 파싱 (null 제외) - FP 스타일
                .mapNotNull { parseLine(it) }

                // 2. 집계 - FP 스타일 (fold 사용 + 내부 가변 최적화)
                .fold(AnalysisAccumulator()) { acc, event ->
                    val categoryId = event.itemId?.let { itemCategoryMap[it] }
                    acc.accumulate(event, categoryId)
                }

                // 3. 변환 - 최종 결과 리턴
                .toResult()
        }
    }

    private fun parseLine(line: String): Event? {
        return try {
            val tokens = line.split(",")
            Event(
                timestamp = tokens[0].toLong(),
                visitorId = tokens[1].toInt(),
                type = EventType.from(tokens[2]),
                itemId = tokens.getOrNull(3)?.takeIf { it.isNotEmpty() }?.toIntOrNull(),
                transactionId = tokens.getOrNull(4)?.takeIf { it.isNotEmpty() }?.toIntOrNull()
            )
        } catch (e: Exception) { null }
    }
}
