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
      "org.scala-lang" % "scala-compiler" % "2.10.0"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
      routesImport ++= Seq("binders._", "_root_.utils._"),
      templatesImport ++= Seq("_root_.utils._"),
      Play2WarKeys.servletVersion := "3.0"
    ).settings(Play2WarPlugin.play2WarSettings: _*)

}
