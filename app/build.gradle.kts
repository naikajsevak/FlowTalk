plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.flowtalk"
    compileSdk = 34

    packagingOptions{
        exclude("META-INF/DEPENDENCIES")
    }

    defaultConfig {
        applicationId = "com.example.flowtalk"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures{
        viewBinding=true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
dependencies {
    implementation ("com.github.OMARIHAMZA:StoryView:1.0.2-alpha")
    implementation("com.google.firebase:firebase-analytics:22.1.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.emoji2:emoji2-views:1.5.0")
    implementation("androidx.room:room-compiler:2.7.0")
    implementation("androidx.room:room-common-jvm:2.7.0")
    implementation("androidx.room:room-runtime-android:2.7.0")
    testImplementation("junit:junit:4.13.2")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation ("com.google.firebase:firebase-bom:33.6.0")
    implementation ("androidx.legacy:legacy-support-v4:1.0.0")
    implementation ("com.google.firebase:firebase-auth:23.1.0")
    implementation ("com.google.firebase:firebase-database:21.0.0")
    implementation ("com.google.firebase:firebase-storage:21.0.1")
    implementation ("com.github.aabhasr1:OtpView:v1.1.2")
    implementation("com.google.firebase:firebase-config:22.0.1")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.6.1")
    implementation ("com.google.android.gms:play-services-auth:21.2.0")
    implementation ("com.github.pgreze:android-reactions:1.3")
    implementation ("com.github.3llomi:CircularStatusView:V1.0.2")
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation ("io.agora.rtc:full-sdk:4.1.1")
    implementation("com.google.firebase:firebase-messaging:24.1.0")
    implementation ("com.android.volley:volley:1.2.1")
    implementation ("com.github.sharish:ShimmerRecyclerView:v1.3")
    implementation ("com.hbb20:ccp:2.5.0")
    implementation ("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation ("com.airbnb.android:lottie:6.6.0")
    implementation ("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")
    implementation ("io.agora.rtc:full-sdk:4.1.1")
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.android.volley:volley:1.2.1")
}