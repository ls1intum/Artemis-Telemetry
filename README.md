Use `docker compose up` for quick start.

## Development

### Running [Artemis](https://github.com/ls1intum/Artemis) in parallel (locally)
Adjust the port on which the telemetry service and its database are running (with the default configuration Artemis will be running on port `8080` and a mysql database on port `3306`).

Adjust the `docker-compose.yml` accordingly, e.g. the following adjustments will be needed to run the telemetry service on port `8081` and its database on port `3307`:  
```
services:
telemetry:
  ports:
    - '127.0.0.1:8081:8080'
mysql:
  ports:
    - "3307:3306"
```

For using the local telemetry service in a development setup you will need to adjust the `application-dev.yml` (within Artemis, not within the telemetry service) accordingly:
```
artemis:
  telemetry:
    enabled: true
    sendAdminDetails: true
    destination: http://localhost:8081
```

We use basic authentication for getting the data from the telemetry service. You will need to adjust the `application.yml` of the telemetry service accordingly:
```
telemetry:
    user: <user>
    password: <password>
```