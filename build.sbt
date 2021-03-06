import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._

organization in ThisBuild := "io.iteratee"

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

lazy val catsVersion = "0.8.1"
lazy val disciplineVersion = "0.7.2"
lazy val monixVersion = "2.1.2"
lazy val scalaCheckVersion = "1.13.4"
lazy val scalaTestVersion = "3.0.1"

lazy val previousIterateeVersion = "0.8.0"

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

lazy val baseSettings = Seq(
  scalacOptions ++= (compilerOptions :+ "-Yno-predef") ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, p)) if p >= 11 => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  scalacOptions in (Compile, console) := compilerOptions,
  scalacOptions in (Compile, test) := compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  coverageHighlighting := (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) => false
      case _ => true
    }
  ),
  (scalastyleSources in Compile) ++= (sourceDirectories in Compile).value,
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.3" cross CrossVersion.binary)
)

lazy val allSettings = baseSettings ++ publishSettings

lazy val commonJsSettings = Seq(
  scalaJSStage in Global := FastOptStage
)

lazy val docSettings = ghpages.settings ++ unidocSettings ++ Seq(
  docMappingsApiDir := "api",
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), docMappingsApiDir),
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-groups",
    "-implicits",
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
  ),
  git.remoteRepo := "git@github.com:travisbrown/iteratee.git",
  unidocProjectFilter in (ScalaUnidoc, unidoc) :=
    inAnyProject -- inProjects(coreJS, benchmark, monixJS, tests, testsJS, twitter)
)

lazy val iteratee = project.in(file("."))
  .enablePlugins(CrossPerProjectPlugin)
  .settings(allSettings)
  .settings(docSettings)
  .settings(noPublishSettings)
  .aggregate(benchmark, core, coreJS, files, monix, monixJS, scalaz, tests, testsJS, twitter)
  .dependsOn(core, scalaz)

lazy val coreBase = crossProject.crossType(CrossType.Pure).in(file("core"))
  .settings(
    moduleName := "iteratee-core",
    name := "core"
  )
  .settings(allSettings: _*)
  .settings(
    libraryDependencies += "org.typelevel" %%% "cats-core" % catsVersion
  )
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.iteratee" %% "iteratee-core" % previousIterateeVersion)
  )
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "core"))
  .jsConfigure(_.copy(id = "coreJS"))

lazy val core = coreBase.jvm
lazy val coreJS = coreBase.js

lazy val testsBase = crossProject.in(file("tests"))
  .configs(IntegrationTest)
  .settings(
    moduleName := "iteratee-tests",
    name := "tests"
  )
  .settings(allSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(Defaults.itSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion,
      "org.scalatest" %%% "scalatest" % scalaTestVersion,
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline" % disciplineVersion
    ),
    scalacOptions ~= {
      _.filterNot(Set("-Yno-predef"))
    },
    parallelExecution in Test := true,
    testForkedParallel in Test := true,
    parallelExecution in IntegrationTest := true,
    testForkedParallel in IntegrationTest := true
  )
  .settings(
    coverageExcludedPackages := "io\\.iteratee\\.tests\\..*",
    testOptions in Test ++= (
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 10)) => Seq(Tests.Argument("-l", "io.iteratee.tests.NoScala210Test"))
        case _ => Nil
      }
    )
  )
  .jvmSettings(fork := false)
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "tests").dependsOn(files))
  .jsConfigure(_.copy(id = "testsJS"))
  .dependsOn(coreBase)

lazy val tests = testsBase.jvm
lazy val testsJS = testsBase.js

lazy val files = project
  .settings(
    moduleName := "iteratee-files",
    mimaPreviousArtifacts := Set("io.iteratee" %% "iteratee-files" % previousIterateeVersion)
  )
  .settings(allSettings)
  .dependsOn(core)

lazy val twitter = project
  .configs(IntegrationTest)
  .settings(
    crossScalaVersions ~= (_.tail),
    moduleName := "iteratee-twitter",
    mimaPreviousArtifacts := Set("io.iteratee" %% "iteratee-twitter" % previousIterateeVersion)
  )
  .settings(allSettings ++ Defaults.itSettings)
  .settings(
    libraryDependencies += "io.catbird" %% "catbird-util" % "0.10.0"
  ).dependsOn(core, files, tests % "test,it")

lazy val scalaz = project
  .configs(IntegrationTest)
  .settings(
    moduleName := "iteratee-scalaz",
    mimaPreviousArtifacts := Set("io.iteratee" %% "iteratee-scalaz" % previousIterateeVersion)
  )
  .settings(allSettings ++ Defaults.itSettings)
  .settings(
    libraryDependencies += "org.scalaz" %% "scalaz-concurrent" % "7.2.8"
  ).dependsOn(core, files, tests % "test,it")

lazy val monixBase = crossProject.in(file("monix"))
  .configs(IntegrationTest)
  .settings(
    moduleName := "iteratee-monix"
  )
  .settings(allSettings: _*)
  .settings(Defaults.itSettings: _*)
  .settings(
    libraryDependencies += "io.monix" %%% "monix-eval" % monixVersion
  )
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.iteratee" %% "iteratee-monix" % previousIterateeVersion)
  )
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "monix").dependsOn(files))
  .jsConfigure(_.copy(id = "monixJS"))
  .dependsOn(coreBase, testsBase % "test,it")

lazy val monix = monixBase.jvm
lazy val monixJS = monixBase.js

lazy val benchmark = project
  .configs(IntegrationTest)
  .settings(
    crossScalaVersions ~= (_.tail),
    moduleName := "iteratee-benchmark"
  )
  .settings(allSettings ++ Defaults.itSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "0.9.2",
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
      "org.scalaz" %% "scalaz-iteratee" % "7.2.7",
      "org.scalaz.stream" %% "scalaz-stream" % "0.8.6a",
      "org.typelevel" %% "cats-free" % catsVersion
    )
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(core, monix, scalaz, tests, twitter)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/travisbrown/iteratee")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  autoAPIMappings := true,
  apiURL := Some(url("https://travisbrown.github.io/iteratee/api/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/travisbrown/iteratee"),
      "scm:git:git@github.com:travisbrown/iteratee.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>travisbrown</id>
        <name>Travis Brown</name>
        <url>https://twitter.com/travisbrown</url>
      </developer>
    </developers>
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq

val jvmProjects = Seq(
  "benchmark",
  "core",
  "files",
  "monix",
  "scalaz",
  "twitter",
  "tests"
)

val jsProjects = Seq(
  "coreJS",
  "monixJS",
  "testsJS"
)

addCommandAlias("testJVM", jvmProjects.map(";" + _ + "/test").mkString)
addCommandAlias("validateJVM", ";testJVM;tests/it:test;benchmark/it:test;monix/it:test;scalaz/it:test;twitter/it:test;scalastyle;unidoc")
addCommandAlias("testJS", jsProjects.map(";" + _ + "/test").mkString)
addCommandAlias("validateJS", ";testJS;scalastyle;unidoc")
addCommandAlias("validate", ";validateJVM;validateJS")
