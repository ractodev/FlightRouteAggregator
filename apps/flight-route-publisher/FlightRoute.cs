using System.Text.Json.Serialization;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

public class FlightRoute
{
    [BsonElement("_id")]
    [JsonIgnore]
    public ObjectId Id { get; set; }
    
    [BsonElement("tag")]
    public string Tag { get; set; }

    [BsonElement("to")]
    public string To { get; set; }

    [BsonElement("from")]
    public string From { get; set; }
    
    [BsonElement("timestamp")]
    public DateTime Timestamp { get; set; }

    [BsonElement("lat")]
    public double Lat { get; set; }

    [BsonElement("lon")]
    public double Lon { get; set; }

    [BsonElement("alt")]
    public double Alt { get; set; }
}