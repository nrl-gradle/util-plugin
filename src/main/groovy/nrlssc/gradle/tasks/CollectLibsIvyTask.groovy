package nrlssc.gradle.tasks

import nrlssc.gradle.UtilPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

class CollectLibsIvyTask extends DefaultTask {

    static CollectLibsIvyTask createFor(Project project)
    {
        CollectLibsIvyTask cliTask = project.tasks.create("collectLibsIvy", CollectLibsIvyTask.class)
        cliTask.group = UtilPlugin.NRL_GROUP
        cliTask.description = 'Collect all runtime dependencies into the "libs" folder with basic ivy format'
        return cliTask
    }


    private File outputDirectory

    @OutputDirectory
    File getOutputDirectory()
    {
        if(outputDirectory == null)
        {
            outputDirectory = getProject().file("libs/ivy")
        }
        return outputDirectory
    }

    void setOutputDirectory(File output)
    {
        this.outputDirectory = output
    }




    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd")

    void recurseBuild(ResolvedDependency dep, Map<String, File> artifactPaths, String ivyString)
    {
        String depString = "<dependency org=\"{group}\" name=\"{name}\" rev=\"{version}\" conf=\"compile->default\"/>"
        Project project = getProject()
        List<String> dependencyStrings = new ArrayList<>()
        if(dep.children != null) {
            dep.children.each { child ->
                dependencyStrings.add(depString.replace("{group}", child.module.id.group).replace("{name}", child.module.id.name).replace("{version}", child.module.id.version))
                recurseBuild(child, artifactPaths, ivyString)
            }
        }

        if(artifactPaths.containsKey(dep.module.id.name)) {


            File fl = artifactPaths[dep.module.id.name]
            artifactPaths.remove(fl)

            File ivyOutput = new File(getOutputDirectory().getPath() + "/" + dep.module.id.group + "/" + dep.module.id.name + "/" + dep.module.id.version + "/ivys")
            File jarOutput = new File(getOutputDirectory().getPath() + "/" + dep.module.id.group + "/" + dep.module.id.name + "/" + dep.module.id.version + "/jars")
            if (!ivyOutput.exists()) {
                ivyOutput.mkdirs()
            }
            if (!jarOutput.exists()) {
                jarOutput.mkdirs()
            }
            project.copy {
                from fl
                into jarOutput
            }

            String ivyTemplate = ivyString.replace("{group}", dep.module.id.group).replace("{name}", dep.module.id.name).replace("{version}", dep.module.id.version)
                    .replace("{date}", sdf.format(new Date(System.currentTimeMillis()))).replace("{dependencies}", dependencyStrings.join("\n\t"))

            File ivyFile = new File(ivyOutput.path + "/ivy-" + dep.module.id.version + ".xml")
            ivyFile.write(ivyTemplate)
        }
    }

    @TaskAction
    void run()
    {
        Project project = getProject()

        def conf = project.configurations.default

        Map<String, File> artifactPaths = new ConcurrentHashMap<>()
        conf.resolvedConfiguration.resolvedArtifacts.each {artifact ->
            artifactPaths.put(artifact.name, artifact.file)
        }
        String ivyTemplate = new BufferedInputStream(CollectLibsIvyTask.class.getResourceAsStream("ivy.xml.basic-template")).readLines().join("\n")

        conf.resolvedConfiguration.firstLevelModuleDependencies.each { dep ->
            recurseBuild((ResolvedDependency)dep, artifactPaths, ivyTemplate)
        }
    }
}
