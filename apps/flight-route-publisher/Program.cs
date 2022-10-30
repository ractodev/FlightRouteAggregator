// See https://aka.ms/new-console-template for more information

using System.Text.Json;
using Confluent.Kafka;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MongoDB.Driver;

var configuration = new ConfigurationBuilder()
    .AddJsonFile("appsettings.json")
    .AddEnvironmentVariables()
    .Build();

var logger = LoggerFactory.Create(cfg => cfg.AddConsole()).CreateLogger("default");

var config = new Config();
configuration.Bind(config);

logger.LogInformation("Config {Config}", JsonSerializer.Serialize(config));

var mongoClient = new MongoClient(config.ConnectionString);
var mongoCollection = mongoClient.GetDatabase(config.DatabaseName).GetCollection<FlightRoute>(config.CollectionName);

var from = await GetFirstTimestamp();
var to = from.AddMinutes(1);

async Task<DateTime> GetFirstTimestamp()
{
    var sort = Builders<FlightRoute>.Sort.Ascending(o => o.Timestamp);
    var flightRoute = await mongoCollection.Find(Builders<FlightRoute>.Filter.Empty).Sort(sort).FirstOrDefaultAsync();
    return flightRoute.Timestamp;
}

var producerConfig = new ProducerConfig()
{
    BootstrapServers = config.KafkaHost
};

var producer = new ProducerBuilder<Null, string>(producerConfig).Build();

logger.LogInformation("Starting Producer");
while (true)
{
    var filter = Builders<FlightRoute>.Filter.Gt(o => o.Timestamp, from);
    filter &= Builders<FlightRoute>.Filter.Lt(o => o.Timestamp, to);
    var sort = Builders<FlightRoute>.Sort.Ascending(o => o.Timestamp);
    var flightRoutes = await mongoCollection.Find(filter).Sort(sort).ToListAsync();
    logger.LogInformation("Processing {FlightRoutesCount} Items", flightRoutes.Count);
    if (flightRoutes.Count > 0)
    {
        foreach (var flightRoute in flightRoutes)
        {
            var json = JsonSerializer.Serialize(flightRoute);
            await producer.ProduceAsync(config.KafkaTopic, new Message<Null, string>() { Value = json });
            logger.LogInformation("Sent {Json} over kafka", json);
        }
    }

    from = to;
    to = to.AddMinutes(1);
    await Task.Delay(TimeSpan.FromMinutes(1));
}

logger.LogInformation("Shutting Down Producer");