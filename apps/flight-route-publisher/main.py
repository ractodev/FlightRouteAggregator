from ensurepip import bootstrap
import time
import json
import pymongo
from bson.json_util import dumps, loads, object_hook
from kafka import KafkaProducer

user = "flight-route-publisher"
password = "WGfvPkzfyNL31grO"
database_name = "flight_route_data"
collection_name ="flight_route_data" 
kafka_host = "localhost:9092"
kafka_topic = "live_flight_traffic"

mongo_client = pymongo.MongoClient(f"mongodb+srv://{user}:{password}@flightdatacluster.neyieqx.mongodb.net/?retryWrites=true&w=majority")
mongo_client.server_info()
collection = mongo_client[database_name].get_collection(collection_name)


data = collection.find_one()
print(data)
deserialized = dumps(data)
x = json.loads(deserialized, object_hook=object_hook)

kafka_producer = KafkaProducer(bootstrap_servers=kafka_host, value_serializer=lambda x: dumps(data).encode('utf-8'))
# for j in range(9999):
#     print("Iteration", j)
#     data = {'counter': j}
#     kafka_producer.send('topic_test', value=data)
#     time.sleep(0.5)

kafka_producer.send(kafka_topic, deserialized)
# kafka_producer.flush()

# print(collection.find_one({}))


# while True:
#     for item in collection.find({}):
#         print(item)
    
#     print("Iter")
#     time.sleep(100)
