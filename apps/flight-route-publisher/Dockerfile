FROM docker.io/bitnami/spark:latest 
RUN pip3 install kafka-python pymongo python-dotenv
WORKDIR /app
COPY . .
CMD ["python3", "./main.py"]
