plugins {
    id("java")
}

group = "com.gmail.genek530"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.github.mwiede:jsch:0.2.18")
}

tasks.test {
    useJUnitPlatform()
}