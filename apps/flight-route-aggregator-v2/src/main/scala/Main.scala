import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, from_json}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType};

case class FlightRoute(Tag: String, To: String, From: String, Timestamp: String, Lat: Double, Lon: Double, Alt: Double);

object Main {
  def main(args: Array[String]): Unit = {
    val schema = StructType(Array(
      new StructField("Tag", StringType),
      new StructField("To", StringType),
      new StructField("From", StringType),
      new StructField("Timestamp", StringType),
      new StructField("Lat", DoubleType),
      new StructField("Lon", DoubleType),
      new StructField("Alt", DoubleType))
    );

    val spark = SparkSession
      .builder()
      .master("spark://spark-master:7077")
      .appName("flight-route-aggregator")
      .config("spark.jars.packages", "org.mongodb.spark:mongo-spark-connector:10.0.0")
      .config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10:3.2.2")
      .getOrCreate();

    spark.sparkContext.setLogLevel("WARN")

    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:29092")
      .option("subscribe", "live_flight_traffic")
      .load()

    //    get_country_udf = udf(lambda lat, lon: get_country(lat, lon), StringType())

    df
      .select(from_json(col("value").cast("string"), schema).alias("parsed_value"))
      .select(col("parsed_value.*"))
      //.withColumn("country", get_country_udf(col("lat"), col("lon")))
      .writeStream
      .format("mongodb")
      .option("spark.mongodb.connection.uri", "mongodb+srv:g/flight-route-publisher:WGfvPkzfyNL31grO@flightdatacluster.neyieqx.mongodb.net/flights.flight-aggregated-data?retryWrites=true&w=majority")
      .option("spark.mongodb.database", "flights")
      .option("spark.mongodb.collection", "flight-aggregated-data")
      .option("spark.mongodb.change.stream.publish.full.document.only", "true")
      .option("checkpointLocation", "gtmp/pyspark-flight-agg/")
      .option("forceDeleteTempCheckpointLocation", "true")
      .outputMode("append")
      .start()
      .awaitTermination()
  }
}