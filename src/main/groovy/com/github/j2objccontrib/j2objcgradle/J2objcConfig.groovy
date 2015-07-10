/*
 * Copyright (c) 2015 the authors of j2objc-gradle (see AUTHORS file)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.j2objccontrib.j2objcgradle

import com.github.j2objccontrib.j2objcgradle.tasks.Utils
import com.google.common.annotations.VisibleForTesting
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.util.PatternSet
import org.gradle.util.ConfigureUtil

/**
 * Further configuration uses the following fields, setting them in j2objcConfig within build.gradle
 */
class J2objcConfig {

    final protected Project project

    J2objcConfig(Project project) {
        assert project != null
        this.project = project

        // The NativeCompilation effectively provides further extensions
        // to the Project, by configuring the 'Objective-C' native build plugin.
        // We don't want to expose the instance to clients of the 'j2objc' plugin,
        // but we also need to configure this object via methods on J2objcConfig.
        nativeCompilation = new NativeCompilation(project)

        // Provide defaults for assembly output locations.
        destSrcDir = "${project.buildDir}/j2objcOutputs/src/main/objc"
        destSrcDirTest = "${project.buildDir}/j2objcOutputs/src/test/objc"
        destLibDir = "${project.buildDir}/j2objcOutputs/lib"
    }

    /**
     * Where to assemble generated main source files.
     * <p/>
     * Defaults to $buildDir/j2objcOutputs/src/main/objc
     */
    String destSrcDir = null

    /**
     * Where to assemble generated test source files.
     * <p/>
     * Can be the same directory as destDir
     * Defaults to $buildDir/j2objcOutputs/src/test/objc
     */
    String destSrcDirTest = null

    /**
     * Where to assemble generated main libraries.
     * <p/>
     * Defaults to $buildDir/j2objcOutputs/lib
     */
    String destLibDir = null

    /**
     * Generated source files directories, e.g. from dagger annotations.
     * <p/>
     * The plugin will ignore changes in this directory so they must
     * be limited to files generated solely from files within your
     * main and/or test sourceSets.
     */
    List<String> generatedSourceDirs = new ArrayList<>()
    /**
     * Add generated source files directories, e.g. from dagger annotations.
     * <p/>
     * The plugin will ignore changes in this directory so they must
     * be limited to files generated solely from files within your
     * main and/or test sourceSets.
     *
     * @param generatedSourceDirs adds generated source directories for j2objc translate
     */
    void generatedSourceDirs(String... generatedSourceDirs) {
        appendArgs(this.generatedSourceDirs, 'generatedSourceDirs', generatedSourceDirs)
    }


    // CYCLEFINDER
    /**
     * Command line arguments for j2objc cycle_finder.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/cycle_finder.html
     */
    List<String> cycleFinderArgs = new ArrayList<>()
    /**
     * Add command line arguments for j2objc cycle_finder.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/cycle_finder.html
     *
     * @param cycleFinderArgs add args for 'cycle_finder' tool
     */
    void cycleFinderArgs(String... cycleFinderArgs) {
        appendArgs(this.cycleFinderArgs, 'cycleFinderArgs', cycleFinderArgs)
    }
    /**
     * Expected number of cycles, defaults to all those found in JRE.
     * <p/>
     * This is an exact number rather than minimum as any change is significant.
     */
    // TODO(bruno): convert to a default whitelist and change expected cyles to 0
    int cycleFinderExpectedCycles = 40


    // TRANSLATE
    /**
     * Command line arguments for j2objc translate.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/j2objc.html
     */
    List<String> translateArgs = new ArrayList<>()
    /**
     * Add command line arguments for j2objc translate.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/j2objc.html
     *
     * @param translateArgs add args for the 'j2objc' tool
     */
    void translateArgs(String... translateArgs) {
        appendArgs(this.translateArgs, 'translateArgs', translateArgs)
    }

    /**
     *  Local jars for translation e.g.: "lib/json-20140107.jar", "lib/somelib.jar".
     *  This will be added to j2objc as a '-classpath' argument.
     */
    List<String> translateClasspaths = new ArrayList<>()
    /**
     *  Local jars for translation e.g.: "lib/json-20140107.jar", "lib/somelib.jar".
     *  This will be added to j2objc as a '-classpath' argument.
     *
     *  @param translateClasspaths add libraries for -classpath argument
     */
    void translateClasspaths(String... translateClasspaths) {
        appendArgs(this.translateClasspaths, 'translateClasspaths', translateClasspaths)
    }

    /**
     * Source jars for translation e.g.: "lib/json-20140107-sources.jar"
     */
    List<String> translateSourcepaths = new ArrayList<>()
    /**
     * Source jars for translation e.g.: "lib/json-20140107-sources.jar"
     *
     *  @param translateSourcepaths args add source jar for translation
     */
    void translateSourcepaths(String... translateSourcepaths) {
        appendArgs(this.translateSourcepaths, 'translateSourcepaths', translateSourcepaths)
    }


    // Do not use groovydoc, this option should remain undocumented.
    // WARNING: Do not use this unless you know what you are doing.
    // If true, incremental builds will be supported even if --build-closure is included in
    // translateArgs. This may break the build in unexpected ways if you change the dependencies
    // (e.g. adding new files or changing translateClasspaths). When you change the dependencies and
    // the build breaks, you need to do a clean build.
    boolean UNSAFE_incrementalBuildClosure = false

    /**
     * Additional libraries that are part of the j2objc distribution.
     * <p/>
     * For example:
     * <pre>
     * translateJ2objcLibs = ["j2objc_junit.jar", "jre_emul.jar"]
     * </pre>
     */
    // J2objc default libraries, from $J2OBJC_HOME/lib/...
    // TODO: auto add libraries based on java dependencies, warn on version differences
    List<String> translateJ2objcLibs = [
            // Comments indicate difference compared to standard libraries...
            // Memory annotations, e.g. @Weak, @AutoreleasePool
            "j2objc_annotations.jar",
            // Libraries that have CycleFinder fixes, e.g. @Weak and code removal
            "j2objc_guava.jar", "j2objc_junit.jar", "jre_emul.jar",
            // Libraries that don't need CycleFinder fixes
            "javax.inject-1.jar", "jsr305-3.0.0.jar",
            "mockito-core-1.9.5.jar"]


    // TODO: warn if different versions than testCompile from Java plugin
    /**
     * Makes sure that the translated filenames don't collide.
     * <p/>
     * Recommended if you choose to use --no-package-directories.
     */
    boolean filenameCollisionCheck = true

    /**
     * Sets the filter on files to translate.
     * <p/>
     * If no pattern is specified, all files within the sourceSets are translated.
     * <p/>
     * This filter is applied on top of all files within the 'main' and 'test'
     * java sourceSets.  Use {@link #translatePattern(groovy.lang.Closure)} to
     * configure.
     */
    PatternSet translatePattern = null
    /**
     * Configures the {@link #translatePattern}.
     * <p/>
     * Calling this method repeatedly further modifies the existing translatePattern,
     * and will create an empty translatePattern if none exists.
     * <p/>
     * For example:
     * <pre>
     * translatePattern {
     *     exclude 'CannotTranslateFile.java'
     *     exclude '**&#47;CannotTranslateDir&#47;*.java'
     *     include '**&#47;CannotTranslateDir&#47;AnExceptionToInclude.java'
     * }
     * </pre>
     * @see
     * <a href="https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_trees">Gradle User Guide</a>
     */
    PatternSet translatePattern(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = PatternSet) Closure cl) {
        if (translatePattern == null) {
            translatePattern = new PatternSet()
        }
        return ConfigureUtil.configure(cl, translatePattern)
    }

    /**
     * @see #dependsOnJ2objcLib(org.gradle.api.Project)
     */
    // TODO: Do this automatically based on project dependencies.
    void dependsOnJ2objcLib(String beforeProjectName) {
        dependsOnJ2objcLib(project.project(beforeProjectName))
    }

    protected NativeCompilation nativeCompilation
    /**
     * Uses the generated headers and compiled j2objc libraries of the given project when
     * compiling this project.
     * <p/>
     * Generally every cross-project 'compile' dependency should have a corresponding
     * call to dependsOnJ2objcLib.
     * <p/>
     * It is safe to use this in conjunction with --build-closure.
     * <p/>
     * Do not also include beforeProject's java source or jar in the
     * translateSourcepaths or translateClasspaths, respectively.  Calling this method
     * is preferable and sufficient.
     */
    // TODO: Do this automatically based on project dependencies.
    void dependsOnJ2objcLib(Project beforeProject) {
        project.with {
            // We need to have j2objcConfig on the beforeProject configured first.
            evaluationDependsOn beforeProject.path

            if (!beforeProject.plugins.hasPlugin(J2objcPlugin)) {
                String message = "$beforeProject does not use the j2objc plugin.\n" +
                              "dependsOnJ2objcLib can be used only with another project that\n" +
                              "itself uses the j2objc plugin."
                throw new InvalidUserDataException(message)
            }

            // Build and test the java/objc libraries and the objc headers of
            // the other project first.
            j2objcPreBuild.dependsOn {
                beforeProject.tasks.getByName('j2objcBuild')
            }
            // Since we assert the presence of the J2objcPlugin above,
            // we are guaranteed that the java plugin, which creates the jar task,
            // is also present.
            j2objcPreBuild.dependsOn {
                beforeProject.tasks.getByName('jar')
            }

            logger.debug("$project:j2objcTranslate must use ${beforeProject.jar.archivePath}")
            j2objcConfig {
                translateClasspaths += beforeProject.jar.archivePath.absolutePath
            }
        }

        nativeCompilation.dependsOnJ2objcLib(beforeProject)
    }

    /**
     * Which architectures will be built and supported in packed ('fat') libraries.
     * <p/>
     * The three ios_arm* architectures are for iPhone and iPad devices, while
     * ios_i386 and ios_x86_64 are for their simulators.
     * <p/>
     * Adding an unrecognized new architecture here will fail.
     * <p/>
     * Removing an architecture here will cause that architecture not to be built
     * and corresponding gradle tasks to not be created.
     * <p/>
     * <pre>
     * supportedArchs = ['ios_arm64']  // Only build libraries for 64-bit iOS devices
     * </pre>
     *
     * @see NativeCompilation#ALL_SUPPORTED_ARCHS
     */
    // Public to allow assignment of array of targets as shown in example
    List<String> supportedArchs = NativeCompilation.ALL_SUPPORTED_ARCHS.clone()


    // TEST
    /**
     * Command line arguments for j2objcTest task.
     */
    List<String> testArgs = new ArrayList<>()
    /**
     * Add command line arguments for j2objcTest task.
     *
     * @param testArgs add args for the 'j2objcTest' task
     */
    void testArgs(String... testArgs) {
        appendArgs(this.testArgs, 'testArgs', testArgs)
    }

    /**
     * j2objcTest will fail if it runs less than the expected number of tests; set to 0 to disable.
     * <p/>
     * It is a minimum so adding a unit test doesn't break the j2objc build.
     */
    int testMinExpectedTests = 1

    /**
     * Filter on files to test.  Note this has no effect on which tests are
     * translated, just which tests are executed by the j2objcTest task.
     * <p/>
     * If no pattern is specified, all files within the 'test' sourceSet are translated.
     * <p/>
     * This filter is applied on top of all files within the 'main' and 'test'
     * java sourceSets.  Use {@link #testPattern(groovy.lang.Closure)} to
     * configure.
     */
    PatternSet testPattern = null
    /**
     * Configures the {@link #testPattern}
     * <p/>
     * Calling this method repeatedly further modifies the existing testPattern,
     * and will create an empty testPattern if none exists.
     * <p/>
     * For example:
     * <pre>
     * translatePattern {
     *     exclude 'CannotTranslateFileTest.java'
     *     exclude '**&#47;CannotTranslateDir&#47;*.java'
     *     include '**&#47;CannotTranslateDir&#47;AnExceptionToIncludeTest.java'
     * }
     * </pre>
     * @see
     * <a href="https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_trees">Gradle User Guide</a>
     */
    PatternSet testPattern(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = PatternSet) Closure cl) {
        if (testPattern == null) {
            testPattern = new PatternSet()
        }
        return ConfigureUtil.configure(cl, testPattern)
    }

    // Native build customization.
    /**
     * Directories of Objective-C source to compile in addition to the
     * translated source.
     */
    // Native build accepts empty array but throws exception on empty List<String>
    // "srcDirs j2objcConfig.extraObjcSrcDirs" line in NativeCompilation
    String[] extraObjcSrcDirs = []
    /**
     * Add directories of Objective-C source to compile in addition to the
     * translated source.
     *
     * @param extraObjcSrcDirs add directories for Objective-C source to be compiled
     */
    void extraObjcSrcDirs(String... extraObjcSrcDirs) {
        for (String arg in args) {
            extraObjcSrcDirs += arg
        }
    }
    /**
     * Additional arguments to pass to the native compiler.
     */
    // Native build accepts empty array but throws exception on empty List<String>
    String[] extraObjcCompilerArgs = []
    /**
     * Add arguments to pass to the native compiler.
     *
     * @param extraObjcCompilerArgs add arguments to pass to the native compiler.
     */
    void extraObjcCompilerArgs(String... extraObjcCompilerArgs) {
        for (String arg in args) {
            extraObjcCompilerArgs += arg
        }
    }
    /**
     * Additional arguments to pass to the native linker.
     */
    // Native build accepts empty array but throws exception on empty List<String>
    String[] extraLinkerArgs = []
    /**
     * Add arguments to pass to the native linker.
     *
     * @param extraLinkerArgs add arguments to pass to the native linker.
     */
    void extraLinkerArgs(String... extraLinkerArgs) {
        for (String arg in args) {
            extraLinkerArgs += arg
        }
    }

    /**
     * Additional native libraries to link to.
     */
    List<Map> extraNativeLibs = []
    /**
     * Add additional native library to link to.
     * <p/>
     * For example, if you built native library 'utils' in project 'common':
     * <pre>
     * extraNativeLib project: 'common', library: 'utils', linkage: 'static'
     * </pre>
     */
    void extraNativeLib(Map spec) {
        extraNativeLibs.add(spec)
    }


    // XCODE
    /**
     * Directory of the target Xcode project.
     */
    String xcodeProjectDir = null
    /**
     * Xcode target the generated files should be linked to.
     */
    String xcodeTarget = null

    protected boolean finalConfigured = false
    /**
     * Configures the native build using.  Must be called at the very
     * end of your j2objcConfig block.
     */
    // TODO: When Gradle makes it possible to modify a native build config
    // after initial creation, we can remove this, and have methods on this object
    // mutate the existing native model { } block.  See:
    // https://discuss.gradle.org/t/problem-with-model-block-when-switching-from-2-2-1-to-2-4/9937
    @VisibleForTesting
    void finalConfigure() {
        nativeCompilation.apply(project.file("${project.buildDir}/j2objcSrcGen"))
        finalConfigured = true

        // For convenience, disable all debug and/or release tasks if the user desires.
        // Note all J2objcPlugin-created tasks are of the form `j2objc.*(Debug|Release)?`
        // however Native plugin-created tasks (on our behalf) are of the form `.*((D|d)ebug|(R|r)elease).*(j|J)2objc.*'
        // so these patterns find all such tasks.

        // Disable only if explicitly present and not true.
        boolean debugEnabled = Boolean.parseBoolean(Utils.getLocalProperty(project, 'debugEnabled', 'true'))
        boolean releaseEnabled = Boolean.parseBoolean(Utils.getLocalProperty(project, 'releaseEnabled', 'true'))
        project.tasks.all { Task task ->
            String name = task.name
            if (name.contains('j2objc') || name.contains('J2objc')) {
                if (!debugEnabled && (name.contains('debug') || name.contains('Debug'))) {
                    task.enabled = false
                }
                if (!releaseEnabled && (name.contains('release') || name.contains('Release'))) {
                    task.enabled = false
                }
            }
        }
    }
    boolean isFinalConfigured() {
        return finalConfigured
    }

    // Provides a subset of "args" interface from project.exec as implemented by ExecHandleBuilder:
    // https://github.com/gradle/gradle/blob/master/subprojects/core/src/main/groovy/org/gradle/process/internal/ExecHandleBuilder.java
    // Allows the following:
    // j2objcConfig {
    //     translateArgs '--no-package-directories', '--prefixes', 'prefixes.properties'
    // }
    @VisibleForTesting
    static void appendArgs(List<String> listArgs, String nameArgs, String... args) {
        if (args == null) {
            throw new InvalidUserDataException("args == null!");
        }
        for (String arg in args) {
            if (arg.contains(' ')) {
                String rewrittenArgs = "'" + arg.split(' ').join("', '") + "'"
                throw new InvalidUserDataException(
                        "'$arg' should not contain spaces and be written out as distinct entries:\n" +
                        "$nameArgs $rewrittenArgs")
            }
        }
        listArgs.addAll(Arrays.asList(args));
    }
}
