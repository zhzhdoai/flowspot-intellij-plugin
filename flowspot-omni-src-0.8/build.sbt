ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.6.4"

// 导入必要的类
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.nio.file.attribute.PosixFilePermission
import java.util.HashSet

ThisBuild / compile / javacOptions ++= Seq(
  "-encoding", "UTF-8", "-parameters",
  "-g", // debug symbols
  "-Xlint",
  "-g:none",
  "--release=11"
) ++ {
  // fail early if users with JDK8 try to run this
  val javaVersion = sys.props("java.specification.version").toFloat
  assert(javaVersion.toInt >= 11, s"this build requires JDK11+ - you're using $javaVersion")
  Nil
}

ThisBuild / scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "--release",
  "11",
  "-Wshadow:type-parameter-shadow"
)
// 启用 sbt-proguard 和 sbt-assembly 插件
enablePlugins(SbtProguard, AssemblyPlugin)

// ProGuard 配置
Proguard / proguardOptions += "-dontoptimize"
Proguard / proguardVersion := "7.6.1"
Proguard / proguardOptions ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")
Proguard / proguardOptions ++= Seq(
  // 保留主类
  "-keep class omni.flowspot.FlowSpotWebServiceApplication { *; }",


  // 保留Scala集合类
  "-keep class scala.collection.** { *; }",
  "-keep class scala.Predef$ { *; }",
  "-keep class scala.Product { *; }",
  "-keep class scala.Tuple* { *; }",



  // 其他保留规则
  "-keep class edu.umd.** { *; }",
  "-keep class org.** { *; }",
  "-keep class com.** { *; }",
  "-keep class java.** { *; }",
  "-keep class javax.** { *; }",
  "-keep class sun.** { *; }",
  "-keep class sunw.** { *; }",
  "-keep class jdk.** { *; }",
  "-keep class net.** { *; }",
  "-dontwarn edu.umd.**"
)
Proguard / proguardOptions += ProguardOptions.keepMain("omni.flowspot.FlowSpotWebServiceApplication")
Proguard / proguardOptions += "-printmapping mappings.txt"
// 项目配置
lazy val root = (project in file("."))
  .settings(
    name := "omni",
    organization := "omni",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "3.6.4",
    mainClass := Some("omni.flowspot.FlowSpotWebServiceApplication"),
    resolvers ++= Seq(
      "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/public",
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Atlassian" at "https://packages.atlassian.com/maven-public/",
      "Gradle Libs" at "https://repo.gradle.org/gradle/libs-releases"
    ),

    // 打包项目代码为一个 JAR 文件
    Compile / packageBin / mappings := (Compile / packageBin / mappings).value,
    Compile / packageBin / artifactPath := target.value / s"${name.value}-${version.value}.jar",

    // 将依赖复制到 lib 目录
    TaskKey[Unit]("copyDependencies") := {
      val dependencies = (Compile / dependencyClasspath).value.files
      val targetDir = target.value / "lib"
      IO.createDirectory(targetDir)
      dependencies.foreach { file =>
        IO.copyFile(file, targetDir / file.getName)
      }
    },

    // 配置 ProGuard 只对 packageBin 生成的 JAR 文件进行混淆
    Proguard / proguardInputs := Seq((Compile / packageBin).value),
    Proguard / proguardOutputs := Seq(target.value / s"${name.value}-${version.value}-obfuscated.jar"),

    // 资源文件处理
    Compile / unmanagedResourceDirectories ++= Seq(
      baseDirectory.value / "src" / "main" / "resources",
      baseDirectory.value / "src" / "main" / "java"  // Include the java directory to pick up image resources
    ),




    // 依赖配置
    libraryDependencies ++= Seq(
      // 原有依赖 - 排除旧版本ANTLR
      "io.shiftleft" %% "codepropertygraph" % "1.7.48",
      "io.joern" %% "dataflowengineoss" % "4.0.422",
      "io.joern" %% "semanticcpg" % "4.0.422",
      "com.github.javaparser" % "javaparser-symbol-solver-core" % "3.26.3",
      "org.ow2.asm" % "asm"  % "9.7.1",
      "io.joern" %% "javasrc2cpg" % "4.0.422",
      "io.shiftleft" %% "codepropertygraph-schema" % "1.7.48",
      "io.joern" %% "joern-cli" % "4.0.422",
      "org.xerial" % "sqlite-jdbc" % "3.45.2.0",
      "org.json4s" %% "json4s-native" % "4.0.7",
      "com.github.jsqlparser" % "jsqlparser" % "5.3",
      "org.antlr" % "antlr4" % "4.7.2",
      "org.antlr" % "antlr4-runtime" % "4.7.2",
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("org", "antlr", "v4", "runtime", xs @ _*) => MergeStrategy.first
      case PathList("org", "antlr", xs @ _*) => MergeStrategy.first
      case PathList("META-INF", "services", xs @ _*) => MergeStrategy.concat
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case "application.conf" => MergeStrategy.concat
      case "reference.conf" => MergeStrategy.concat
      case x => MergeStrategy.first
    },

    // 排除Spring Boot中的ANTLR类
    assembly / assemblyExcludedJars := {
      val cp = (assembly / fullClasspath).value
      cp filter { jar =>
        jar.data.getName.contains("antlr4") &&
          !jar.data.getName.contains("4.7.2")
      }
    },

    // 排除冲突的依赖 (移除H2排除规则，因为现在需要H2数据库)
    // libraryDependencies := libraryDependencies.value.map(_.exclude("com.h2database", "h2-mvstore")),

    // Assembly 合并策略
//    assembly / assemblyMergeStrategy := {
//      case PathList("META-INF", "web-fragment.xml") => MergeStrategy.first
//      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//      case "application.conf" => MergeStrategy.concat
//      case "reference.conf" => MergeStrategy.concat
//      case x if x.contains("h2") && x.endsWith(".class") => MergeStrategy.first
//      case x => MergeStrategy.first
//    },

    // 其他配置
    fork := true,
    run / fork := true,
    Test / fork := true,
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-optimise"
    )
  )
//val generateDomainClasses = taskKey[Seq[File]]("generate domain classes for our schema")
//lazy val schema = project
//  .in(file("schema"))
//  .settings(generateDomainClasses := {
//    val outputRoot = target.value / "fg-codegen"
//    FileUtils.deleteRecursively(outputRoot)
//    val invoked = (Compile / runMain).toTask(s" CpgExtCodegen schema/target/fg-codegen").value
//    FileUtils.listFilesRecursively(outputRoot)
//  })
// 自定义任务：打包为 zip
lazy val packageZip = taskKey[File]("Package the project as a zip file with executable run scripts")
packageZip := {
  // 清理 target 目录中的编译缓存
  println("Cleaning target directory...")
  clean.value

  // 确保先执行依赖的任务
  val _ = (Proguard / proguard).value


  // 准备要包含在 zip 中的文件
  val zipDir = target.value / "zip"
  IO.createDirectory(zipDir)

  // 复制 obfuscated JAR
  val obfuscatedJar = (Proguard / proguardOutputs).value.head
  IO.createDirectory(zipDir / "lib")
  IO.copyFile(obfuscatedJar, zipDir / "lib" / obfuscatedJar.getName)

  // 复制依赖库
  (root / TaskKey[Unit]("copyDependencies")).value
  val libDir = target.value / "lib"
  IO.copyDirectory(libDir, zipDir / "lib") // Corrected line: copy the lib directory itself

  // 复制 config 目录
  val configDir = baseDirectory.value / "config"
  if (configDir.exists() && configDir.isDirectory) {
    IO.copyDirectory(configDir, zipDir / "config")
    println("Copied config directory to zip package")
  } else {
    println("Warning: config directory not found in project root, skipping")
  }



  // 确保 logs 目录存在
  IO.createDirectory(zipDir / "logs")

  // 创建 zip 文件
  val zipFile = target.value / s"${name.value}-${version.value}.zip"
  IO.zip(Path.allSubpaths(zipDir), zipFile, time = None)

  println(s"Created zip package at: ${zipFile.getAbsolutePath}")
  zipFile
}

// 设置任务依赖关系
packageZip := packageZip.dependsOn(clean, Proguard / proguard, root / TaskKey[Unit]("copyDependencies")).value