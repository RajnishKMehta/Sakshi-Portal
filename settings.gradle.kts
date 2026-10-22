/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val gprUser = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
        val gprKey = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        if (!gprUser.isNullOrEmpty() && !gprKey.isNullOrEmpty()) {
            maven {
                url = uri("https://maven.pkg.github.com/RajnishKMehta/sakshi-sdk")
                credentials {
                    username = gprUser
                    password = gprKey
                }
                content {
                    includeModule("io.github.rajnishkmehta.sakshi", "sakshi-sdk")
                }
            }
        }

        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "Sakshi-Portal"
include(":app")
