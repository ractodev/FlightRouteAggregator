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

spark_conf = SparkConf().setAppName(
    "flight-route-aggregator").setMaster("spark://spark-master:7077")

spark = SparkSession.builder.config(conf=spark_conf).getOrCreate()
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
    .withColumn("country", get_country_udf(col("lat"), col("lon"))) \
    .writeStream \
    .outputMode("Append") \
    .format("console") \
    .start() \

# .select(col("parsed_value.*")) \
# query = df.select(from_json(col("value").cast("string").alias("parsed_value"), schema)) \
#     .writestream \
#     .outputmode("append") \
#     .format("console") \
#     .start() \

query.awaitTermination()


# Reverse geolocator function (input: (lat, lon) | output: country name)

# data = sc.parallelize(list("Hello World"))
# counts = data.map(lambda x:
#                   (x, 1)).reduceByKey(add).sortBy(lambda x: x[1],
#                                                   ascending=False).collect()

# for (word, count) in counts:
#     print("{}: {}".format(word, count))

# spark.stop()
# quit()
