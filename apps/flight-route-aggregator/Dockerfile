FROM docker.io/bitnami/spark:latest 
RUN pip3 install pyspark
WORKDIR /app
COPY . .
CMD ["python3", "./main.py"]
