import sbt._
import Keys._
import play.Project._

// import PlayProject._
import com.github.play2war.plugin._

object ApplicationBuild extends Build {

    val appName         = "quickgame"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "org.scala-lang" % "scala-compiler" % "2.10.0",
      "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
      routesImport ++= Seq("binders._", "common.GameType"),
      templatesImport ++= Seq("common.GameType"),
      testOptions in Test := Nil, // No imports for ScalaTest
      Play2WarKeys.servletVersion := "3.0"
    ).settings(Play2WarPlugin.play2WarSettings: _*)

}
