# Commonmark-Kotlin

[![许可证](https://img.shields.io/badge/License-BSD%202--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-blue.svg?logo=kotlin)]([http://kotlinlang.org](https://www.jetbrains.com/kotlin-multiplatform/))

一个用于解析 CommonMark Markdown 语法的 Kotlin
多平台库，基于原始的 [commonmark-java](https://github.com/commonmark/commonmark-java) 项目改编，
将 Java 文件全部转为 Kotlin 文件。

该库旨在让 `commonmark-java` 能用于 Kotlin 多平台，提供 CommonMark Markdown 语法的解析和渲染功能。

## 特性

- 🚀 **Kotlin 多平台**：支持 Android、iOS 和 JVM 平台
- 📝 **兼容 CommonMark**：完全支持 CommonMark 规范
- 🔧 **可扩展**：支持多种扩展
- 🎯 **类型安全**：完全用 Kotlin 编写，具备类型安全

## 支持平台

- **Android** - 安卓应用
- **iOS** - 苹果应用
- **JVM** - Java 虚拟机（桌面应用、服务器）

## 扩展

该库包含多个实用扩展：

- **commonmark-ext-autolink** - 自动链接识别
- **commonmark-ext-gfm-tables** - GitHub 风格 Markdown 表格
- **commonmark-ext-gfm-strikethrough** - GitHub 风格 Markdown 删除线
- **commonmark-ext-latex** - LaTeX 数学公式

## 安装

### Gradle (Kotlin DSL)

将以下内容添加到 `build.gradle.kts`：

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("io.github.mrl:commonmark:0.25.0")
    // 可选扩展
    implementation("io.github.mrl:commonmark-ext-autolink:0.25.0")
    implementation("io.github.mrl:commonmark-ext-gfm-tables:0.25.0")
    implementation("io.github.mrl:commonmark-ext-gfm-strikethrough:0.25.0")
    implementation("io.github.mrl:commonmark-ext-latex:0.25.0")
}
```

### 多平台配置

Kotlin 多平台项目配置示例：

```kotlin 
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.mrl:commonmark:0.25.0")
        }
    }
}
```

## 使用

### 基本用法

```kotlin
val parser = Parser.builder().build()
val document = parser.parse("This is _Sparta_")
val renderer = HtmlRenderer.builder().build()
val html = renderer.render(document) // "This is _Sparta_\n"
```

### 使用扩展

```kotlin
val extensions = listOf(TablesExtension.create())
val parser = Parser.builder()
    .extensions(extensions)
    .build()
val renderer = HtmlRenderer.builder()
    .extensions(extensions)
    .build()

val markdown = """
| Feature | Support |
|---------|---------|
| Tables  | ✅      |
| Kotlin  | ✅      |
"""

val document = parser.parse(markdown)
val html = renderer.render(document)
```

### 示例

可参考 commonmark-java 项目的[示例](https://github.com/commonmark/commonmark-java#usage)