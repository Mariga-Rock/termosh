plugins {
    id("termosh.android.library")
}

android {
    namespace = "app.termosh.core.terminal"
}

dependencies {
    implementation(project(":core:common"))
    // TODO: termux/terminal-view (Apache 2.0) — добавить через JitPack или локально
}
