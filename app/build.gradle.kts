plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

// O plugin do Google Services só é aplicado se o google-services.json
// existir. Assim o build de CI continua passando antes do Firebase ser
// configurado — o app simplesmente roda com o login desativado.
if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "app.nexus.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.nexus.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // URL base do backend (Django REST). Sobrescreva por buildType.
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"https://api.nexusdrive.example.com/\""
        )

        // Segredo HMAC usado para assinar/validar códigos de ativação.
        // ATENÇÃO: troque antes de publicar. Como fica embutido no APK,
        // qualquer um com decompilador consegue gerar códigos. Para
        // produto pago em escala, mova validação para o backend.
        // Pode ser sobrescrito via local.properties (NEXUS_LICENSE_SECRET=...)
        // — esse arquivo é gitignored.
        val licenseSecret: String = run {
            val props = java.util.Properties()
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { props.load(it) }
            props.getProperty("NEXUS_LICENSE_SECRET")
                ?: "REPLACE-ME-WITH-YOUR-OWN-SECRET-KEY-32-BYTES"
        }
        buildConfigField("String", "LICENSE_SECRET", "\"$licenseSecret\"")
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://staging.nexusdrive.example.com/\""
            )
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Ponte entre as Task<T> do Firebase/Play Services e as coroutines (.await()).
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Autenticação de usuários (login) e Firestore (estado da licença
    // por conta). Funcionam apenas quando o app é compilado com um
    // google-services.json válido.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Background work
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
