plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    implementation(project(":openrndr-application"))
    implementation(project(":openrndr-event"))
    implementation(libs.lwjgl.nfd)
}