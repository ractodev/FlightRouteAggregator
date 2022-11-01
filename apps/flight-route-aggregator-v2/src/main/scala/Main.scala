import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, from_json}
import org.apache.spark.sql.types.{
  DoubleType,
  StringType,
  StructField,
  StructType
};
import com.mongodb.spark.sql.connector.config.MongoConfig

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

object Main {

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

    val get_country = (lat: Double, lon: Double) => "SE"
    val get_country_udf = spark.udf.register("get_country", get_country)

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
    // .map(o => (o.Tag, o.Country))
    // .distinct()
    // .groupByKey(o => o._2)
    // .count()
    // .as("Count")

    //    query
    //      .writeStream
    //      .format("console")
    //      .outputMode("update")
    //      .start()
    //      .awaitTermination()
    //
    //

    // import scala.collection.JavaConverters._;
    // var map = Map[String, String]("first" -> "QQ").asJava
    // var mongoConfig = MongoConfig.writeConfig(map).toWriteConfig().getClass()

    query.writeStream
      .format("mongodb")
      .option(
        "spark.mongodb.connection.uri",
        "mongodb+srv://flight-route-publisher:WGfvPkzfyNL31grO@flightdatacluster.neyieqx.mongodb.net/flights.flight-aggregated-data?retryWrites=true&w=majority"
      )
      .option("spark.mongodb.database", "flights")
      .option("spark.mongodb.collection", "flight-aggregated-data")
      .option("spark.mongodb.change.stream.publish.full.document.only", "true")
      .option("checkpointLocation", "/opt/spark/checkpoint")
      .option("forceDeleteTempCheckpointLocation", "true")
      .outputMode("complete")
      .start()
      .awaitTermination()

  }
}
