# Reactor monitoring demo

## Preparation
You will need Java 8+, Docker and Docker Compose installed.

## Quick start

1. Start Prometheus & Grafana with Docker Compose:

    ```shell script
    $ docker-compose up -d
    ```
    
    It will start Grafana in port 3000 and Prometheus on port 9090.
1. Open Grafana and explore the dashboards:

    - "Schedulers" - dashboard for various metrics reported by Reactor's schedulers
    - "Business" - dashboard with a few metrics related to the business logic based on Reactor's metrics mechanism
1. Start `com.example.demo.DemoApplication`

    The application is a simple Spring Boot application that listens to Wikipedia's change stream
    and exposes the latest change via HTTP endpoint `GET /latestChange`.
    
    It enables Reactor schedulers' metrics by calling `Schedulers.enableMetrics()`.
    
    The processing pipeline converts the received change into a human readable form.
    Note that it does not know how to process the updates from bots and reports an error.
    
    There are two "reactive" metrics in the pipeline:
        
     1. `recentchange` for every event sent by the Wikipedia stream
     1. `processing` for every result of the processing
1. Once the application is started, you should start seeing the metrics in Grafana.
    
    Try hitting various endpoints of the app to observe the changes on the "Schedulers" dashboard.
    For instance, you can run some load on `GET /latestChange` or `GET /actuator/health` with tools like WRK:
    ```shell script
    $ wrk -t12 -c1000 -d30s http://localhost:8080/actuator/health
    Running 30s test @ http://localhost:8080/actuator/health
      12 threads and 1000 connections
      Thread Stats   Avg      Stdev     Max   +/- Stdev
        Latency    24.40ms   51.39ms   2.00s    99.10%
        Req/Sec     3.34k   651.18     5.31k    70.00%
      1197831 requests in 30.07s, 130.23MB read
      Socket errors: connect 0, read 1081, write 0, timeout 14
    Requests/sec:  39834.50
    Transfer/sec:      4.33MB
    ```


## Where to find the dashboards' definitions
If you want to import them into your running Grafana, see `dashboards/` folder for the Grafana dashboard definitions in JSON format.

One can also export the definition from the UI by clicking on "Share dashboard -> Export -> View JSON".