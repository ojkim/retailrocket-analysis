plugins {
    kotlin("jvm") version "2.2.20"
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib")) // 코틀린 표준 라이브러리 추가
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("MainKt")
}


tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(24)
}

tasks.jar {
    manifest {
        // Main.kt 파일이 컴파일된 클래스 이름인 "MainKt"를 지정
        attributes["Main-Class"] = "MainKt"
    }

    // 코틀린 라이브러리 등 모든 의존성을 JAR 안에 포함시키는 로직
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    // 파일이 겹칠 경우 중복 제거 전략 (오류 방지)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}