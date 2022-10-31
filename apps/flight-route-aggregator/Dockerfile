FROM docker.io/bitnami/spark:latest 
RUN pip3 install pyspark geopy
WORKDIR /app
COPY . .
CMD ["spark-submit", "--packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.2.2,org.mongodb.spark:mongo-spark-connector:10.0.0", "main.py"]
