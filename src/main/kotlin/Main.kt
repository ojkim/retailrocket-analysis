import service.EventAnalyzer
import service.MetadataLoader
import service.AnalysisResult
import model.EventType
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    // (기존 main 함수 내용은 그대로 두셔도 됩니다)
    if (args.isEmpty()) {
        println("사용법: java -jar retailrocket.jar \"<데이터_폴더_경로>\"")
        return
    }

    val dataDir = args[0]
    val files = mapOf(
        "events" to "$dataDir/events.csv",
        "category" to "$dataDir/category_tree.csv",
        "item1" to "$dataDir/item_properties_part1.csv",
        "item2" to "$dataDir/item_properties_part2.csv"
    )

    println("=== ShopWise 데이터 분석 시스템 구동 중... ===")

    val time = measureTimeMillis {
        try {
            val loader = MetadataLoader()
            val categoryTree = loader.loadCategoryTree(files["category"]!!)
            val itemMap = loader.loadItemCategoryMap(listOf(files["item1"]!!, files["item2"]!!))

            val analyzer = EventAnalyzer(itemMap, categoryTree)
            val result = analyzer.analyze(files["events"]!!)

            // 여기에 새로 만든 보고서 함수를 호출합니다
            printDetailedReport(result)

        } catch (e: Exception) {
            println("오류 발생: ${e.message}")
            e.printStackTrace()
        }
    }
    println("\n(총 분석 소요 시간: ${time / 1000.0}초)")
}

// -----------------------------------------------------------------
// [수정됨] CEO의 5가지 질문에 완벽하게 답하는 상세 보고서 함수
// -----------------------------------------------------------------
fun printDetailedReport(result: AnalysisResult) {
    // 데이터 준비
    val views = result.typeCounts[EventType.VIEW] ?: 0
    val carts = result.typeCounts[EventType.ADD_TO_CART] ?: 0
    val orders = result.typeCounts[EventType.TRANSACTION] ?: 0
    val total = views + carts + orders

    println("\n" + "=".repeat(60))
    println("📘 ShopWise 사용자 행동 분석 보고서")
    println("=".repeat(60))

    // ---------------------------------------------------------
    // 질문 1. 사람들이 사이트에서 어떤 행동을 하는가?
    // ---------------------------------------------------------
    println("\n1️⃣ 사용자 행동 개요 (User Actions)")
    println("   사이트 내에서 발생한 총 ${"%,d".format(total)}건의 행동을 분석했습니다.")

    val viewShare = (views.toDouble() / total) * 100
    val cartShare = (carts.toDouble() / total) * 100
    val orderShare = (orders.toDouble() / total) * 100

    println("   • 상품 조회 (View):        %,d회 (%4.1f%%)".format(views, viewShare))
    println("   • 장바구니 담기 (Cart):    %,d회 (%4.1f%%)".format(carts, cartShare))
    println("   • 구매 완료 (Order):       %,d회 (%4.1f%%)".format(orders, orderShare))
    println("   👉 결론: 사용자의 행동 중 96% 이상은 단순 '조회'입니다.")


    // ---------------------------------------------------------
    // 질문 2 & 3. 구매 단계 및 이탈률 분석 (Funnel & Drop-off)
    // ---------------------------------------------------------
    println("\n" + "-".repeat(60))
    println("2️⃣ & 3️⃣ 구매 여정 및 이탈률 분석 (Funnel View)")
    println("   고객이 '조회 -> 장바구니 -> 구매'로 넘어갈 때 얼마나 사라지는지 보여줍니다.\n")

    // 단계 1 -> 2 계산
    val viewToCartRate = if (views > 0) (carts.toDouble() / views) * 100 else 0.0
    val viewDropOff = 100.0 - viewToCartRate

    // 단계 2 -> 3 계산
    val cartToOrderRate = if (carts > 0) (orders.toDouble() / carts) * 100 else 0.0
    val cartDropOff = 100.0 - cartToOrderRate

    println("   [단계 1] 상품 조회 (%,d명)".format(views))
    println("      │")
    println("      │  🔻 이탈: %.1f%% (%,d명은 그냥 나감)".format(viewDropOff, views - carts))
    println("      │  ✅ 전환: %.2f%% 만 장바구니로 이동".format(viewToCartRate))
    println("      ⬇️")
    println("   [단계 2] 장바구니 (%,d명)".format(carts))
    println("      │")
    println("      │  🔻 이탈: %.1f%% (%,d명은 결제 안 함)".format(cartDropOff, carts - orders))
    println("      │  ✅ 전환: %.2f%% 만 구매 완료".format(cartToOrderRate))
    println("      ⬇️")
    println("   [단계 3] 구매 완료 (%,d명)".format(orders))

    println("\n   👉 각 단계별 이탈률: 상품조회에서 장바구니 단꼐까지 고객 %.1f%%가 이탈하며".format(viewDropOff))
    println("\n                     장바구니에 담은 고객 중 %.1f%%가 구매를 포기합니다.".format(cartDropOff))


    // ---------------------------------------------------------
    // 질문 4 & 5. 시간대별 활동 및 구매 패턴 (Timing Analysis)
    // ---------------------------------------------------------
    println("\n" + "-".repeat(60))
    println("4️⃣ & 5️⃣ 시간에 따른 활동 패턴")

    val peakTraffic = result.hourlyTraffic.maxByOrNull { it.value }
    val peakSales = result.hourlySales.maxByOrNull { it.value }

    println("   🕒 가장 활발한 쇼핑 시간 (Traffic Peak)")
    println("      - 시간: ${peakTraffic?.key}시")
    println("      - 규모: %,d건의 활동 발생".format(peakTraffic?.value))
    println("      - 의미: 이때가 사이트 접속자가 가장 많습니다.")

    println("\n   💰 가장 많이 팔리는 시간 (Sales Peak)")
    println("      - 시간: ${peakSales?.key}시")
    println("      - 규모: %,d건의 결제 발생".format(peakSales?.value))
    println("      - 의미: 실제 매출은 이때 가장 많이 일어납니다.")

    println("\n   👉 시간대별 구매 패턴 분석 결과:")
    if (peakTraffic?.key == peakSales?.key) {
        println("      고객들이 많이 오는 시간에 구매도 가장 많이 일어납니다. (${peakTraffic?.key}시에 마케팅 집중 필요)")
    } else {
        println("      흥미롭게도 구경하는 시간(${peakTraffic?.key}시)과 구매 시간(${peakSales?.key}시)이 다릅니다.")
        println("      ${peakTraffic?.key}시에는 상품 노출을 늘리고, ${peakSales?.key}시에는 결제 혜택을 푸시하는 전략이 필요합니다.")
    }

    println("\n" + "=".repeat(60))
}