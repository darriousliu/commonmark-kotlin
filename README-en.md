# Commonmark-Kotlin

[English Version](README-en.md) | [中文版本](README.md)

[![License](https://img.shields.io/badge/License-BSD%202--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-blue.svg?logo=kotlin)]([http://kotlinlang.org](https://www.jetbrains.com/kotlin-multiplatform/))

A Kotlin Multiplatform library for parsing CommonMark Markdown syntax, adapted from the
original [commonmark-java](https://github.com/commonmark/commonmark-java) project.
Convert all Java files to Kotlin files.

This library aims to enable commonmark-java to be used in Kotlin Multiplatform, providing parsing
and rendering functions for CommonMark Markdown syntax.

## Features

- 🚀 **Kotlin Multiplatform**: Supports Android, iOS, and JVM platforms
- 📝 **CommonMark Compliant**: Full support for CommonMark specification
- 🔧 **Extensible**: Support for various extensions
- 🎯 **Type Safe**: Written entirely in Kotlin with type safety

## Supported Platforms

- **Android** - Android applications
- **iOS** - iOS applications
- **JVM** - Java Virtual Machine (Desktop applications, servers)

## Extensions

This library includes several useful extensions:

- **commonmark-ext-autolink** - Automatic link detection
- **commonmark-ext-gfm-tables** - GitHub Flavored Markdown tables
- **commonmark-ext-gfm-strikethrough** - GitHub Flavored Markdown strikethrough
- **commonmark-ext-latex** - LaTeX math expressions

## Installation

### Gradle (Kotlin DSL)

Add the following to your `build.gradle.kts`:

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("io.github.mrl:commonmark:0.25.0")
    // Extensions (optional)
    implementation("io.github.mrl:commonmark-ext-autolink:0.25.0")
    implementation("io.github.mrl:commonmark-ext-gfm-tables:0.25.0")
    implementation("io.github.mrl:commonmark-ext-gfm-strikethrough:0.25.0")
    implementation("io.github.mrl:commonmark-ext-latex:0.25.0")
}
```

### Multiplatform Setup

For Kotlin Multiplatform projects:

```kotlin 
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.mrl:commonmark:0.25.0")
        }
    }
}
```

## Usage

### Basic Usage

```kotlin
val parser = Parser.builder().build()
val document = parser.parse("This is _Sparta_")
val renderer = HtmlRenderer.builder().build()
val html = renderer.render(document) // "This is _Sparta_\n"
```

### With Extensions

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

### Example

You can refer to [Example](https://github.com/commonmark/commonmark-java#usage) in commonmark-java
