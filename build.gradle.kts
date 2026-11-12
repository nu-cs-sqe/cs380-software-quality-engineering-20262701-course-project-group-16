import java.math.BigDecimal
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("java")
    id("application")
    checkstyle
    jacoco
    id("com.github.spotbugs") version "6.5.13"
    id("info.solidsoft.pitest") version "1.19.0"
}

group = "nu.csse.sqe"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

tasks.compileJava {
    options.release = 11
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}

application {
    mainClass.set("ui.Main")
}

// Shared scope includes betting, settlement, statistics and stable-seat continuation.
// This is not a whole-team coverage or mutation score.
val measuredClasses = listOf(
    "domain.model.Card",
    "domain.model.Deck",
    "domain.model.PokerHandEvaluator",
    "domain.model.ShowdownResolver",
    "domain.model.ShowdownResult",
    "application.GameInitializer",
    "application.InitialDeal",
    "application.HandStatistics",
    "application.MatchStatistics",
    "application.NextHand",
    "application.StatisticsView",
    "domain.model.PotSettlement",
    "domain.model.Settlement",
    "domain.model.PlayStats",
    "domain.model.BettingEngine",
    "domain.model.BettingAmounts",
    "domain.model.BettingRound",
    "domain.model.BettingHistory",
    "domain.model.BettingProgress",
    "domain.model.GameConfig",
    "domain.model.GameState",
    "domain.model.Player",
    "domain.model.PlayerState",
    "domain.model.HandPositions",
    "domain.model.SeatOrder",
    "domain.model.ActionRequest",
    "domain.model.AcceptedAction",
    "domain.model.BettingResult",
    "domain.model.LegalActions",
    "domain.model.StatsContext",
    "domain.model.StatsAction",
    "application.SetupSession",
    "application.SetupView",
    "localization.Localizations"
)
val measuredClassFiles = measuredClasses.flatMap {
    val path = it.replace('.', '/')
    listOf("$path.class", "$path\$*.class")
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    classDirectories.setFrom(sourceSets.main.get().output.classesDirs.asFileTree.matching {
        include(measuredClassFiles)
        exclude("domain/model/ShowdownResult\$Status.class")
    })
    reports {
        html.required = true
        xml.required = true
        csv.required = false
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

checkstyle {
    // The 10.x release line runs on the Java 11 project toolchain.
    toolVersion = "10.26.1"
    maxWarnings = 0
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        html.required = true
        xml.required = true
    }
}

tasks.named<Checkstyle>("checkstyleTest") {
    configFile = file("config/checkstyle/checkstyle-test.xml")
}

spotbugs {
    toolVersion = "4.10.2"
    effort = Effort.MAX
    reportLevel = Confidence.LOW
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}

tasks.withType<SpotBugsTask>().configureEach {
    // Analyze Java 11 bytecode using the JDK that runs Gradle (17 locally, 25 in CI).
    launcher.set(javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(JavaVersion.current().majorVersion)
    })
    reports.create("html") { required = true }
    reports.create("xml") { required = true }
}

pitest {
    pitestVersion.set("1.22.1")
    junit5PluginVersion.set("1.2.3")
    targetClasses.set(measuredClasses.flatMap { listOf(it, "$it\$*") }.toSet())
    excludedClasses.set(setOf("domain.model.ShowdownResult\$Status"))
    targetTests.set(setOf(
        "domain.model.*Test", "application.*Test", "integration.*Test",
        "localization.LocalizationsTest"
    ))
    threads.set(2)
    mutators.set(setOf("DEFAULTS"))
    outputFormats.set(setOf("HTML", "XML"))
    timestampedReports.set(false)
    exportLineCoverage.set(true)
    // A mutated loop can hang; allow baseline time * 2 plus 2 seconds per test.
    timeoutFactor.set(BigDecimal("2.0"))
    timeoutConstInMillis.set(2000)
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
}
