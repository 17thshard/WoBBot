plugins {
	application
	kotlin("jvm") version "2.4.20"
	kotlin("plugin.serialization") version "2.4.20"
}

group = "wiresegal.wob"
version = "2.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	implementation("dev.kord:kord-core:0.18.1")
	implementation("org.slf4j:slf4j-simple:2.0.20")
	implementation("org.jsoup:jsoup:1.23.2")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
	implementation("io.github.mltheuser:khtmltomarkdown:1.0.1")
}

application {
	mainClass = "wiresegal.wob.MainKt"
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll(
			"-Xcollection-literals",
			"-Xcontext-sensitive-resolution",
			"-Xname-based-destructuring=name-mismatch",
		)
	}
}
