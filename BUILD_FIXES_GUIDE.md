# Build Fixes Guide for Flutter Android Plugins

Two common errors on newer Flutter SDKs (3.22+) and how to fix them.

---

## Error 1: Inconsistent JVM-target compatibility

```
Inconsistent JVM-target compatibility detected for tasks
'compileDebugJavaWithJavac' (1.8) and 'compileDebugKotlin' (21)
```

**Cause:** Flutter 3.22+ Gradle plugin sets Kotlin JVM target to 21 while Java compile options default to 1.8.

**Fix:** Open `android/build.gradle` in your plugin and add these three things:

```gradle
apply plugin: 'com.android.library'

// 1. Force every Kotlin compile task to use JVM 17
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
    kotlinOptions {
        jvmTarget = '17'
    }
}

android {
    // ... existing config ...

    // 2. Set Java compatibility to 17
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    // 3. Set Kotlin JVM target inside android block too
    kotlinOptions {
        jvmTarget = '17'
    }
}
```

**Key points:**
- `tasks.withType(...).configureEach` is the safety net — it runs after any plugin overrides
- Both `compileOptions` and `kotlinOptions` must use the same version
- Java 17 (not 1.8, not 21) is the safest middle ground for current tooling

---

## Error 2: Redeclaration — Kotlin stub conflicts with Java class

```
e: ZcsSdkPlugin.kt:10:7 Redeclaration:
class ZcsSdkPlugin : FlutterPlugin, MethodChannel.MethodCallHandler
class ZcsSdkPlugin : Any, FlutterPlugin, MethodChannel.MethodCallHandler
```

**Cause:** When you create a Flutter plugin with `--template=plugin`, it generates a Kotlin stub file. If you later rewrite the plugin in Java (keeping the same class name), both files compile and produce duplicate `.class` files.

**Fix:** Delete the leftover Kotlin stub:

```bash
# Remove the Kotlin stub (adjust path to match your plugin)
rm android/src/main/kotlin/com/example/your_plugin/YourPlugin.kt

# Clean up empty directories (optional)
rmdir -p android/src/main/kotlin/com/example/your_plugin
```

Then verify only your Java implementation remains under `android/src/main/java/`.

---

## Checklist for fixing any plugin

1. **Locate** `android/build.gradle`
2. **Patch** JVM target as shown in Error 1 fix
3. **Check** for duplicate `.kt` + `.java` files with the same class name
4. **Delete** the `.kt` file if Java is the real implementation
5. **Build** with `flutter build apk --debug` to verify
6. **Bump** version in `pubspec.yaml`
7. **Publish** with `flutter pub publish`

---

## Quick one-liner to find stale Kotlin stubs

Run this from your plugin root to find plugins that have both a `.kt` and `.java` file declaring the same class:

```bash
find android/src -name "*.kt" -o -name "*.java" | xargs grep -l "class.*Plugin" | sort
```

If you see both `YourPlugin.kt` and `YourPlugin.java`, the `.kt` is likely stale.
