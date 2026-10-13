package grails.plugins.conventions

import java.nio.file.Path

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

class ConventionPluginsSpec extends Specification {

    @TempDir
    Path projectDir

    def setup() {
        projectDir.resolve('settings.gradle').toFile().text = "rootProject.name = 'sample-root'\n"
    }

    private def run(String... args) {
        GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(args as List)
                .forwardOutput()
    }

    private void buildScript(String text) {
        projectDir.resolve('build.gradle').toFile().text = text
    }

    def 'config.code-style extracts the bundled rule sets'() {
        given:
        buildScript("plugins { id 'groovy'; id 'config.code-style' }")

        when:
        def result = run('extractCodeStyleConfig').build()

        then:
        result.task(':extractCodeStyleConfig').outcome == TaskOutcome.SUCCESS
        ['checkstyle/checkstyle.xml', 'checkstyle/checkstyle-suppressions.xml', 'codenarc/codenarc.groovy'].every {
            projectDir.resolve("build/code-style-config/${it}").toFile().file
        }
    }

    def 'config.project-metadata fails clearly without project.yml'() {
        given:
        buildScript("plugins { id 'config.project-metadata' }")

        when:
        def result = run('tasks').buildAndFail()

        then:
        result.output.contains('requires a project.yml')
    }

    def 'config.project-metadata exposes project.yml values'() {
        given:
        projectDir.resolve('project.yml').toFile().text = '''
project: { title: T, description: D, org: O }
github: { org: gh, project: p }
license: { name: Apache-2.0 }
'''
        buildScript('''
plugins { id 'config.project-metadata' }
tasks.register('show') { doLast { println "title=${projectTitle} org=${githubOrg}" } }
''')

        expect:
        run('show', '-q').build().output.contains('title=T org=gh')
    }

    def 'config.docs defaults to the template naming and can be overridden'() {
        given:
        projectDir.resolve('settings.gradle').toFile().text = "rootProject.name = 'sample-root'\n"
        buildScript('''
plugins { id 'config.docs' }
tasks.register('show') { doLast { println "${pluginDocs.pluginProject.get()}|${pluginDocs.docsProject.get()}" } }
''')

        expect:
        run('show', '-q').build().output.contains(':sample|:sample-docs')
    }
}
