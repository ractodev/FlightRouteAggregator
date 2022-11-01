ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.15"

Compile / packageBin / mappings += {
  (baseDirectory.value / "src" / "main" / "scala" / "cities1000.txt") -> "cities1000.txt"
}

val sparkVersion = "3.3.0"

libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion % "provided"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided"
libraryDependencies += "org.mongodb.spark" % "mongo-spark-connector" % "10.0.4" % "provided"
/* libraryDependencies += "io.github.coordinates2country" % "coordinates2country" % "1.2" */
// libraryDependencies += "com.github.Vineeth-Mohan" % "OfflineReverseGeocode" % "1.0"

lazy val root = (project in file("."))
  .settings(
    name := "flight-route-aggregator-v2"
  )

//resolvers += "jitpack" at "https://jitpack.io"
resourceDirectory in Compile := baseDirectory.value / "src" / "main" / "data"

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  "flight-route-aggregator.jar"
}

