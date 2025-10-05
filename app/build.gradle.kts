plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.devmiax.spamilagros"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.devmiax.spamilagros"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Para usar vectores de Material Icons en APIs antiguas
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)
    implementation(libs.coordinatorlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Material Design 3: TextInputLayout, MaterialButton, ChipGroup, MaterialToolbar, MaterialCardView, etc.
    implementation("com.google.android.material:material:1.12.0")
// Compatibilidad y Toolbar base
    implementation("androidx.appcompat:appcompat:1.7.0")
// RecyclerView para “Mis citas”
    implementation("androidx.recyclerview:recyclerview:1.3.2")
// Swipe to refresh para recargar disponibilidad
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
// CoordinatorLayout (contenedor base de varias pantallas)
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
// (Opcional) ConstraintLayout si alguna otra pantalla la usa
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}