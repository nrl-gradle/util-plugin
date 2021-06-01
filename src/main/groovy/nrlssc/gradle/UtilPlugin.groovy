package nrlssc.gradle

import nrlssc.gradle.tasks.AddCopyrightTask
import nrlssc.gradle.tasks.ClassDiagram
import nrlssc.gradle.tasks.CollectLibsIvyTask
import nrlssc.gradle.tasks.DelegateTask
import nrlssc.gradle.tasks.GoOfflineTask
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UtilPlugin implements Plugin<Project>{
    private static Logger logger = LoggerFactory.getLogger(UtilPlugin.class)

    public static String NRL_GROUP = 'nrl'
    
    @Override
    void apply(Project project) {
        Jar sj = createSourcesJarTask(project)
        Jar jj = createJavadocJarTask(project)
        Sync cl = createCollectLibsTask(project)

        AddCopyrightTask.createFor(project)
        ClassDiagram.createFor(project)
        ClassDiagram.createExtendedFor(project)
        //CollectLibsIvyTask.createFor(project)
        GoOfflineTask.createFor(project)
    }

    
    
    static Sync createCollectLibsTask(Project project) {
        Sync clTask = project.tasks.create("collectLibs", Sync.class)
        clTask.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        clTask.configure {
            project.gradle.projectsEvaluated {
                from project.configurations.compileClasspath
                from project.configurations.runtimeClasspath
                into "libs/raw"
            }
        }
        clTask.group = NRL_GROUP
        clTask.description = 'Collect all runtime and compile dependencies into the "libs/raw" folder as jars'
        return clTask
    }
    
    static Jar createSourcesJarTask(Project project)
    {
        Jar sj = project.tasks.create('sourcesJar', Jar.class)
        sj.classifier = 'sources'
        sj.configure{
            project.gradle.projectsEvaluated {
                from project.sourceSets.main.allSource
            }
        }
        sj.dependsOn project.tasks.classes

        sj.group = 'build'
        sj.description = 'Builds an internalJar with .java file sources in it, for distribution to clients who need source code.  This will run the addCopyrightText task.'

        return sj

    }

    static Jar createJavadocJarTask(Project project)
    {
        Jar jj = project.tasks.create('javadocJar', Jar.class)
        jj.classifier = 'javadoc'
        jj.configure{
            project.gradle.projectsEvaluated {
                from project.tasks.javadoc.destinationDir
            }
        }
        jj.dependsOn project.tasks.javadoc
        jj.group = 'build'
        jj.description = 'Builds an internalJar of the javadocs for this project.'
        return jj
    }
}
