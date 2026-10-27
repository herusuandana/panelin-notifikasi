plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val releaseStore = providers.environmentVariable("PANELIN_KEYSTORE")
android {
    namespace = "id.panelin.notifikasi"
    compileSdk = 35
    defaultConfig {
        applicationId = "id.panelin.notifikasi"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { buildConfig = true }
    if (releaseStore.isPresent) signingConfigs.create("clientRelease") {
        storeFile = file(releaseStore.get())
        storePassword = providers.environmentVariable("PANELIN_STORE_PASSWORD").get()
        keyAlias = "panelin-release"
        keyPassword = providers.environmentVariable("PANELIN_KEY_PASSWORD").get()
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (releaseStore.isPresent) signingConfig = signingConfigs.getByName("clientRelease")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = true }
}
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
tasks.register("requireReleaseSigning") {
    doLast { check(releaseStore.isPresent) { "Release signing missing. Use scripts/build-release.py." } }
}
tasks.matching { it.name == "assembleRelease" }.configureEach { dependsOn("requireReleaseSigning") }
