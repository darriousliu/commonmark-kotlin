# Commonmark-Kotlin

[English Version](README-en.md) | [中文版本](README.md)

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
- **commonmark-ext-footnotes** - 脚注支持
- **commonmark-ext-gfm-strikethrough** - GitHub 风格 Markdown 删除线
- **commonmark-ext-gfm-tables** - GitHub 风格 Markdown 表格
- **commonmark-ext-heading-anchor** - 标题锚点
- **commonmark-ext-image-attributes** - 图片属性
- **commonmark-ext-ins** - 插入文本支持
- **commonmark-ext-latex** - LaTeX 数学公式
- **commonmark-ext-task-list-items** - 任务列表项
- **commonmark-ext-yaml-front-matter** - YAML 前置数据

## 安装

### Gradle (Kotlin DSL)

将以下内容添加到 `settings.gradle.kts`：

```kotlin
pluginManagement {
    repositories {
        mavenCentral() // 或者 maven { url = uri("https://jitpack.io") }
    }
}
```

然后，在你的 `build.gradle.kts` 中添加依赖：

### Android

```kotlin
dependencies {
    implementation("io.github.darriousliu:commonmark:0.25.1")
    // 可选扩展
    implementation("io.github.darriousliu:commonmark-ext-autolink:0.25.1")
    implementation("io.github.darriousliu:commonmark-ext-footnotes:0.25.1")
    implementation("io.github.darriousliu:commonmark-ext-gfm-strikethrough:0.25.1")
    implementation("io.github.darriousliu:commonmark-ext-gfm-tables:0.25.1")
    implementation("io.github.darriousliu:commonmark-ext-heading-anchor:0.25.1")
    implementation("io.github.darriousliu:commonmark-ext-image-attributes:0.25.1")
    implementation("io.github.darriousliu:commonmark-ext-ins:0.25.1")
    implementation("io.github.darriousliu:commonmark-ext-task-list-items:0.25.1")
    implementation("io.github.darriousliu:commonmark-ext-latex:0.25.1")
}
```

### Kotlin 多平台：

```kotlin 
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.darriousliu:commonmark:0.25.1")
            // 可选扩展
            implementation("io.github.darriousliu:commonmark-ext-autolink:0.25.1")
            implementation("io.github.darriousliu:commonmark-ext-footnotes:0.25.1")
            implementation("io.github.darriousliu:commonmark-ext-gfm-strikethrough:0.25.1")
            implementation("io.github.darriousliu:commonmark-ext-gfm-tables:0.25.1")
            implementation("io.github.darriousliu:commonmark-ext-heading-anchor:0.25.1")
            implementation("io.github.darriousliu:commonmark-ext-image-attributes:0.25.1")
            implementation("io.github.darriousliu:commonmark-ext-ins:0.25.1")
            implementation("io.github.darriousliu:commonmark-ext-task-list-items:0.25.1")
            implementation("io.github.darriousliu:commonmark-ext-latex:0.25.1")
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