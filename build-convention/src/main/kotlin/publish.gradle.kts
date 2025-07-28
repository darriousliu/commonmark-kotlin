plugins {
    `maven-publish`
    signing
}

publishing {
    repositories {

    }
    publications {
        getByName<MavenPublication>("kotlinMultiplatform") {
            groupId = "io.github.mrl"
            version = "0.25.0"

            pom {
                name.set("Commonmark-Kotlin")
                description.set("A multiplatform library for parsing CommonMark Markdown syntax in Kotlin")
                url.set("https://github.com/master-lzh/commonmark-kotlin")

                licenses {
                    license {
                        name.set("BSD 2-Clause License")
                        url.set("https://opensource.org/licenses/BSD-2-Clause")
                    }
                }
                scm {
                    url.set("https://github.com/master-lzh/commonmark-kotlin")
                    connection.set("scm:git:git://github.com/master-lzh/commonmark-kotlin.git")
                    developerConnection.set("scm:git:git://github.com/master-lzh/commonmark-kotlin.git")
                }
            }
        }
    }
}