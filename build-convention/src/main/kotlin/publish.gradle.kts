plugins {
    id("com.vanniktech.maven.publish")
}

group = "io.github.darriousliu"
version = "0.25.0"

val commonPom = Action<MavenPom> {
    name.set("Commonmark-Kotlin")
    description.set("A multiplatform library for parsing CommonMark Markdown syntax in Kotlin")
    url.set("https://github.com/darriousliu/commonmark-kotlin")

    licenses {
        license {
            name.set("BSD 2-Clause License")
            url.set("https://opensource.org/licenses/BSD-2-Clause")
        }
    }
    developers {
        developer {
            id.set("darriousliu")
            name.set("Darrious Liu")
        }
    }
    scm {
        url.set("https://github.com/darriousliu/commonmark-kotlin")
        connection.set("scm:git:git://github.com/darriousliu/commonmark-kotlin.git")
        developerConnection.set("scm:git:git://github.com/darriousliu/commonmark-kotlin.git")
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/darriousliu/commonmark-kotlin")
            credentials(PasswordCredentials::class)
        }
    }
    publications {
        getByName<MavenPublication>("kotlinMultiplatform") {
            groupId = project.group.toString()
            version = project.version.toString()

            pom(commonPom)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    pom(commonPom)
}