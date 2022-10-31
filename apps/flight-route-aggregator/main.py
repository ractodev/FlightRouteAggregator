from geopy.geocoders import Nominatim
from pyspark.conf import SparkConf
from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json, col, udf
from pyspark.sql.types import StructType, StructField, StringType, DoubleType

print("Starting Consumer")


def get_country(lat, lon):
    return "SE"


def locator(lat, lon):
    try:
        geolocator = Nominatim(user_agent="geoapiExercises")
        location = geolocator.reverse(lat + "," + lon)
        country = location.raw['address'].get('country', '')
        return country
    except:
        return "International Waters"


schema = StructType(
    [
        StructField("Tag", StringType()),
        StructField("To", StringType()),
        StructField("From", StringType()),
        StructField("Timestamp", StringType()),
        StructField("Lat", DoubleType()),
        StructField("Lon", DoubleType()),
        StructField("Alt", DoubleType())
    ]
)

spark = SparkSession \
    .builder \
    .master("spark://spark-master:7077") \
    .appName("flight-route-aggregator") \
    .config("spark.jars.packages", "org.mongodb.spark:mongo-spark-connector:10.0.0") \
    .config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.2.2") \
    .getOrCreate()

spark.sparkContext.setLogLevel("WARN")

df = spark \
    .readStream \
    .format("kafka") \
    .option("kafka.bootstrap.servers", "kafka:29092") \
    .option("subscribe", "live_flight_traffic") \
    .load()

get_country_udf = udf(lambda lat, lon: get_country(lat, lon), StringType())

query = df.select(from_json(col("value").cast("string"), schema).alias("parsed_value")) \
    .select(col("parsed_value.*")) \
    .withColumn("country", get_country_udf(col("lat"), col("lon")))

query \
    .writeStream \
    .format("mongodb") \
    .option("spark.mongodb.connection.uri", "mongodb+srv://flight-route-publisher:WGfvPkzfyNL31grO@flightdatacluster.neyieqx.mongodb.net/flights.flight-aggregated-data?retryWrites=true&w=majority") \
    .option('spark.mongodb.database', 'flights') \
    .option('spark.mongodb.collection', 'flight-aggregated-data') \
    .option('spark.mongodb.change.stream.publish.full.document.only', 'true') \
    .option("checkpointLocation", "/tmp/pyspark-flight-agg/") \
    .option("forceDeleteTempCheckpointLocation", "true") \
    .outputMode("append") \
    .start() \
    .awaitTermination()
