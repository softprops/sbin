import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  //val snapshots = "Scala Tools Snapshots" at "http://www.scala-tools.org/repo-snapshots/"
  //val specs = "org.scala-tools.testing" % "specs" % "1.6.2.1" % "test"
  
  // persistence
  val akkaRepo = "akka repo" at "http://www.scalablesolutions.se/akka/repository"
  val redis14 = "com.redis" % "redisclient" % "2.8.0.RC3-1.4-SNAPSHOT"
  
  // web
  val jetty7 = "org.eclipse.jetty" % "jetty-webapp" % "7.0.2.RC0" % "test"
  
  // unfiltered
  val ufs = "net.databinder" %% "unfiltered-server" % "0.1.3-SNAPSHOT"
}