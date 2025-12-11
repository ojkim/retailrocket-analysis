package service

import java.io.File

class MetadataLoader {

    // 1. 카테고리 트리 로딩 (자식ID -> 부모ID)
    fun loadCategoryTree(filePath: String): Map<Int, Int> {
        val file = File(filePath)
        if (!file.exists()) {
            println("⚠️ 경고: category_tree.csv를 찾을 수 없습니다.")
            return emptyMap()
        }

        println("🌳 카테고리 구조 로딩 중...")
        return file.useLines { lines ->
            lines.drop(1) // 헤더 건너뛰기
                .mapNotNull { line ->
                    val tokens = line.split(",")
                    // categoryid, parentid
                    if (tokens.size >= 2) {
                        val childId = tokens[0].toIntOrNull()
                        val parentId = tokens[1].toIntOrNull()
                        if (childId != null && parentId != null) childId to parentId else null
                    } else null
                }
                .toMap()
        }
    }

    // 2. 아이템 속성 로딩 (Part 1 & 2 통합)
    // 아이템 ID -> 카테고리 ID 매핑만 추출 (메모리 절약)
    fun loadItemCategoryMap(filePaths: List<String>): Map<Int, Int> {
        val itemCategoryMap = mutableMapOf<Int, Int>()

        println("🏷️ 아이템-카테고리 매핑 정보 생성 중... (데이터 연결)")

        filePaths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                print("   - 파일 처리 중: ${file.name} ... ")
                var count = 0
                file.useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",")
                        // timestamp, itemid, property, value
                        if (tokens.size >= 4) {
                            val itemId = tokens[1].toIntOrNull()
                            val property = tokens[2]
                            val value = tokens[3]

                            // 전체 속성 중 'categoryid'만 필요함
                            if (itemId != null && property == "categoryid") {
                                value.toIntOrNull()?.let { catId ->
                                    itemCategoryMap[itemId] = catId
                                    count++
                                }
                            }
                        }
                    }
                }
                println("완료 ($count 개 매핑)")
            } else {
                println("\n⚠️ 경고: 파일을 찾을 수 없습니다 ($path)")
            }
        }
        return itemCategoryMap
    }
}