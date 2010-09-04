import sbt._

class ConferencePortal(info: ProjectInfo) extends DefaultWebProject(info) {

 // http://code.google.com/p/simple-build-tool/wiki/BuildConfiguration

 // Versions

  lazy val LIFT_VERSION  = "2.1-SNAPSHOT"
  lazy val SCALA_SPECS_VERSION  = "1.6.6-SNAPSHOT"

  // Compiler settings

  override def compileOptions = super.compileOptions ++
    Seq("-target:jvm-1.5",
        "-nowarn",
        "-make:transitivenocp",
        "-deprecation",
        "-Xmigration",
        "-Xcheckinit",
        "-Xwarninit",
  //    DisableWarnings,
        "-encoding", "utf8")
        .map(x => CompileOption(x))
  override def javaCompileOptions = JavaCompileOption("-Xlint:unchecked") :: super.javaCompileOptions.toList


  // JRebel (redeploy on any changes)

  override def scanDirectories = Nil

  // Repositories

  lazy val mavenLocal = "Local Maven2 Repository" at "file://" + Path.userHome + "/.m2/repository"
  lazy val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
  lazy val scalaToolsReleases = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-releases"
  
  lazy val jBossReleases = "Jboss Maven2 Releases Repository" at "http://repository.jboss.org/maven2"
  lazy val jBossSnapshots = "Jboss Maven2 Snapshots Repository" at "http://snapshots.jboss.org/maven2"

  // Dependencies (Compile)

  // Lift

  lazy val liftMapper = "net.liftweb" % "lift-mapper_2.8.0" % LIFT_VERSION % "compile->default"  
  lazy val liftTextile = "net.liftweb" % "lift-textile_2.8.0" % LIFT_VERSION % "compile->default"  
  lazy val liftJson = "net.liftweb" % "lift-json_2.8.0" % LIFT_VERSION % "compile->default" 
  
  // Dispatch

  lazy val dispatchHttp = "net.databinder" % "dispatch-http_2.8.0" % "0.7.6-SNAPSHOT" % "compile->default" 

  // Drools (Jboss - Drools Solver automatically assigns the resources of planning problems within a given amount of time.)
  
  lazy val droolsCore = "org.drools" % "drools-core" % "5.1.0.M1" % "compile->default" 
  lazy val droolsApi = "org.drools" % "drools-api" % "5.1.0.M1" % "compile->default" 
  lazy val droolsCompiler = "org.drools" % "drools-compiler" % "5.1.0.M1" % "compile->default" 
  lazy val droolsSolverCore = "org.drools.solver" % "drools-solver-core" % "5.1.0.M1" % "compile->default" 

  lazy val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0.1" % "compile->default"
  lazy val javaMail = "javax.mail" % "mail" % "1.4.3" % "compile->default"
  lazy val slf4j = "org.slf4j" % "slf4j-log4j12" % "1.5.0" % "compile->default"
  // Dependencies (Provided)

  lazy val servlet = "javax.servlet" % "servlet-api" % "2.5" % "provided->default"
  lazy val mySqlConnector = "mysql" % "mysql-connector-java" % "5.1.9" % "provided->default"  

  // Dependencies (Test)

  lazy val junit = "junit" % "junit" % "4.8.1" % "test->default"
  lazy val specs = "org.scala-tools.testing" % "specs_2.8.0" % SCALA_SPECS_VERSION % "test->default"
  lazy val jetty6 = "org.mortbay.jetty" % "jetty" % "6.1.23" % "test->default"

}          
