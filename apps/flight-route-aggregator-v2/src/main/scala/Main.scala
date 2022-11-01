import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, from_json}
import org.apache.spark.sql.types.{
  DoubleType,
  StringType,
  StructField,
  StructType
};
import com.mongodb.spark.sql.connector.config.MongoConfig
import org.apache.spark.sql.streaming.DataStreamWriter
import org.apache.spark.sql.streaming.StreamingQuery
import com.mongodb.spark.sql.connector.config.WriteConfig
import geocode.ReverseGeoCode
import java.io.FileInputStream

case class FlightRoute(
    Tag: String,
    To: String,
    From: String,
    Timestamp: String,
    Lat: Double,
    Lon: Double,
    Alt: Double,
    Country: String
);

case class FlightRouteAggreagted(_id: String, Country: String, Count: Long)

object Main {

  var stream = getClass().getResourceAsStream("cities1000.txt")
  var geoCoder = new ReverseGeoCode(stream, true)

  def get_country(lat: Double, lon: Double): String = {
    // val country =
    //   io.github.coordinates2country.Coordinates2Country.country(lat, lon);

    var place = geoCoder.nearestPlace(lat, lon)
    return if (place != null) place.country else "International Water"
  }

  def main(args: Array[String]): Unit = {
    val schema = StructType(
      Array(
        new StructField("Tag", StringType),
        new StructField("To", StringType),
        new StructField("From", StringType),
        new StructField("Timestamp", StringType),
        new StructField("Lat", DoubleType),
        new StructField("Lon", DoubleType),
        new StructField("Alt", DoubleType)
      )
    );

    val spark = SparkSession
      .builder()
      .master("spark://spark-master:7077")
      .appName("flight-route-aggregator")
      .getOrCreate();

    import spark.implicits._;

    spark.sparkContext.setLogLevel("WARN")

    val get_country_udf = spark.udf.register(
      "get_country",
      (lat: Double, lon: Double) => get_country(lat, lon)
    )

    val query = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:29092")
      .option("subscribe", "live_flight_traffic")
      .load()
      .select(
        from_json(col("value").cast("string"), schema)
          .alias("parsed_value")
      )
      .select(col("parsed_value.*"))
      .withColumn("Country", get_country_udf(col("Lat"), col("Lon")))
      .as[FlightRoute]
      .map(o => (o.Tag, o.Country))
      .distinct()
      .groupByKey(o => o._2)
      .count()
      .as("count")

    val writer = query.writeStream
      .option("checkpointLocation", "/opt/spark/checkpoint")
      .option("forceDeleteTempCheckpointLocation", "true")
      .outputMode("update")

    UseConsole(writer).awaitTermination()
  }

  def UseMongo[T](writer: DataStreamWriter[T]): StreamingQuery = {

    import scala.collection.JavaConverters._;
    var config = Map[String, String](
      "connection.uri" -> "mongodb+srv://flight-route-publisher:WGfvPkzfyNL31grO@flightdatacluster.neyieqx.mongodb.net/flights.flight-aggregated-data?retryWrites=true&w=majority",
      "database" -> "flights",
      "collection" -> "flight-aggregated-data",
      "change.stream.publish.full.document.only" -> "true"
    ).asJava

    var mongoConfig = MongoConfig.writeConfig(config).getOptions().asScala

    return writer
      .format("mongodb")
      .options(mongoConfig)
      // .option(
      //   "spark.mongodb.connection.uri",
      //   "mongodb+srv://flight-route-publisher:WGfvPkzfyNL31grO@flightdatacluster.neyieqx.mongodb.net/flights.flight-aggregated-data?retryWrites=true&w=majority"
      // )
      // .option("spark.mongodb.database", "flights")
      // .option("spark.mongodb.collection", "flight-aggregated-data")
      // .option("spark.mongodb.change.stream.publish.full.document.only", "true")
      .start()
  }

  def UseConsole[T](writer: DataStreamWriter[T]): StreamingQuery = {

    return writer
      .format("console")
      .start()
  }
}
