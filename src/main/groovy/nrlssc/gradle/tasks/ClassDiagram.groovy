package nrlssc.gradle.tasks

import io.github.classgraph.ClassGraph
import nrlssc.gradle.UtilPlugin
import nrlssc.gradle.helpers.PluginUtils
import nrlssc.gradle.helpers.PropertyName
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by scraft on 6/15/2017.
 */
class ClassDiagram extends DefaultTask{
    private static Logger logger = LoggerFactory.getLogger(ClassDiagram.class)

    static ClassDiagram createFor(Project project)
    {
        ClassDiagram gcd = project.tasks.create("classDiagram", ClassDiagram.class)
        gcd.group = UtilPlugin.NRL_GROUP
        gcd.dependsOn project.tasks.classes
        gcd.description = 'Generates a Class Hierarchy Diagram for your project'
        return gcd
    }

    static ClassDiagram createExtendedFor(Project project)
    {
        ClassDiagram gcd = project.tasks.create("classDiagramExtended", ClassDiagram.class)
        gcd.extended = true
        gcd.group = UtilPlugin.NRL_GROUP
        gcd.dependsOn project.tasks.classes
        gcd.description = 'Generates an Extended Class Hierarchy Diagram for your project'
        return gcd
    }

    @Input
    boolean extended = false
    @Input
    boolean deep = false
    @Input
    int width = 3840
    @Input
    int height = 2160
    @Input
    String imageType = "png"


    String[] packages = new String[0]

    File outputFile

    @OutputFile
    File getOutputFile() {
        if(outputFile == null)
        {
            Project project = getProject()
            def hgit = project.extensions.getByName('hgit')
            outputFile = project.file("$project.buildDir/diagrams/classgraph-diagram-${extended ? "extended-" : ""}${deep ? "deep-" : ""}${hgit.getProjectVersion()}.dot")
        }
        return outputFile
    }

    @OutputFile
    File getOutputPng() {
        if(outputFile == null)
        {
            Project project = getProject()
            def hgit = project.extensions.getByName('hgit')
            outputFile = project.file("$project.buildDir/diagrams/classgraph-diagram-${extended ? "extended-" : ""}${deep ? "deep-" : ""}${hgit.getProjectVersion()}.png")
        }
        return outputFile
    }

    void setOutputFile(File outputFile) {
        this.outputFile = outputFile
    }

    @TaskAction
    void generateDiagram()
    {
        Project project = getProject()
        File outputFile = getOutputFile()
        packages = getAllPackagesInProject(project)
        String classpath = project.sourceSets.main.runtimeClasspath.asPath

        logger.lifecycle("Packages searched:")
        packages.each { pk ->
            logger.lifecycle("\t$pk")
        }

        ClassGraph scanner = new ClassGraph().whitelistPackages(packages)

        if(classpath != null && classpath.length() > 0)
        {
            scanner = scanner.overrideClasspath(classpath)
        }
        if(extended)
        {
            scanner = scanner.enableFieldInfo().enableMethodInfo()
        }
        if(deep)
        {
            scanner = scanner.ignoreFieldVisibility().ignoreMethodVisibility()
        }

        String dotFileContents = scanner.scan().getAllClasses().generateGraphVizDotFile(width, height)

        if(!outputFile.parentFile.exists())
        {
            outputFile.parentFile.mkdirs()
        }
        if(outputFile.exists())
        {
            outputFile.delete()
        }
        try {
            Files.write(Paths.get(outputFile.absolutePath), dotFileContents.getBytes())
            if(project.hasProperty("graphVizPath")) {
                String command = getSFDP(project)
                if(command != null)
                {
                    command = command + " -x -Goverlap=scale -T $imageType ${outputFile.absolutePath} -o ${project.file(outputFile.absolutePath.replace(".dot", ".$imageType"))}"
                    PluginUtils.execute(command, project.projectDir)
                }
            }
        } catch (IOException e) {
            logger.error("Error: ", e)
        }
    }

    static String getSFDP(Project project)
    {
        String com = PropertyName.graphVizPath.exists(project) ? String.join("/", PropertyName.graphVizPath.getAsString(project).replace("\\", "/"), "bin", "sfdp.exe") : null
        return com
    }

    static String[] getPackagesIn(File fl, String trim)
    {
        def packages = []

        if(fl.listFiles() != null) {
            boolean hasFile = false
            for (File file : fl.listFiles()) {
                if (file.isFile()) {
                    hasFile = true
                    def val = fl.absolutePath.replace(trim, "").replace("/", ".").replace("\\", ".")
                    if(val.toString().startsWith("."))
                    {
                        val = val.substring(1)
                    }
                    packages << val
                    break
                }
            }
            if(!hasFile){
                for(File dir : fl.listFiles()) {
                    packages.addAll(getPackagesIn(dir, trim))
                }
            }
        }

        return packages
    }

    static String[] getAllPackagesInProject(Project project)
    {
        def packageList = []
        project.sourceSets.each { ss ->
            ss.allJava.dirs.each{ srcDir ->
                if(srcDir.exists()) {
                    srcDir.eachFile { fl ->
                        packageList.addAll(getPackagesIn(fl, srcDir.getAbsolutePath()))
                    }
                }
            }
        }
        return packageList
    }
}
