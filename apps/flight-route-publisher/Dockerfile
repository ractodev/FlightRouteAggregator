FROM mcr.microsoft.com/dotnet/sdk:6.0 AS build-env
WORKDIR /App

# Copy everything
COPY . ./
# Restore as distinct layers
RUN dotnet restore
# Build and publish a release
RUN dotnet publish -c Release -o out

# Build runtime image
FROM mcr.microsoft.com/dotnet/aspnet:6.0
# ENV ConnectionString=mongodb+srv://flight-route-publisher:WGfvPkzfyNL31grO@flightdatacluster.neyieqx.mongodb.net/?retryWrites=true&w=majority
# ENV DatabaseName=flights
# ENV CollectionName=flight_path_data_v2
# ENV KafkaHost=kafka:9092
# ENV KafkaTopic="live_flight_traffic"
WORKDIR /App
COPY --from=build-env /App/out .
ENTRYPOINT ["dotnet", "flight-route-publisher.dll"]
