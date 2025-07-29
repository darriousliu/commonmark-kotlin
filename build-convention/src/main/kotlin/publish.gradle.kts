plugins {
    `maven-publish`
    signing
}

group = "io.github.darriousliu"
version = "0.25.0"

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/darriousliu/commonmark-kotlin")
            credentials {
                username = System.getenv("GITHUB_PUBLISH_USERNAME")
                password = System.getenv("GITHUB_PUBLISH_TOKEN")
            }
        }
    }
    publications {
        getByName<MavenPublication>("kotlinMultiplatform") {
            groupId = project.group.toString()
            version = project.version.toString()

            pom {
                name.set("Commonmark-Kotlin")
                description.set("A multiplatform library for parsing CommonMark Markdown syntax in Kotlin")
                url.set("https://github.com/darriousliu/commonmark-kotlin")

                licenses {
                    license {
                        name.set("BSD 2-Clause License")
                        url.set("https://opensource.org/licenses/BSD-2-Clause")
                    }
                }
                scm {
                    url.set("https://github.com/darriousliu/commonmark-kotlin")
                    connection.set("scm:git:git://github.com/darriousliu/commonmark-kotlin.git")
                    developerConnection.set("scm:git:git://github.com/darriousliu/commonmark-kotlin.git")
                }
            }
        }
    }
}