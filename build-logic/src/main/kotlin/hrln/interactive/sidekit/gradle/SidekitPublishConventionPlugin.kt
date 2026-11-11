package hrln.interactive.sidekit.gradle

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create

/**
 * Maven publication for a Sidekit Android library.
 *
 * The artifact host is deliberately not configured here: it is an open distribution decision.
 * Set `sidekitPublish { artifactId.set(...) }` in the module and add a repository wherever the
 * artifacts are published.
 */
class SidekitPublishConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")
        project.group = "hrln.interactive.sidekit"
        project.version = GitVersion.name(project)

        val sidekitPublish = project.extensions.create<SidekitPublishExtension>("sidekitPublish")
        sidekitPublish.artifactId.convention(project.name)
        sidekitPublish.description.convention("Sidekit library ${project.name}")

        project.plugins.withId("com.android.library") {
            project.extensions.configure<LibraryExtension> {
                publishing {
                    singleVariant("release") {
                        withSourcesJar()
                    }
                }
            }
        }

        project.afterEvaluate {
            val release = project.components.findByName("release") ?: error(
                "No release component found; apply sidekit.android.library before sidekit.publish.",
            )
            project.extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("release") {
                        from(release)
                        artifactId = sidekitPublish.artifactId.get()
                        pom {
                            name.set(project.name)
                            description.set(sidekitPublish.description.get())
                            url.set("https://github.com/manuel-rtsch/Sidekit")
                            licenses {
                                license {
                                    name.set("MIT License")
                                    url.set("https://opensource.org/licenses/MIT")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
