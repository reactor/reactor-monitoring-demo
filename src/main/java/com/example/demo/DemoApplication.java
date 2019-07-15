package com.example.demo;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@RestController
public class DemoApplication {

	public static void main(String[] args) throws Exception {
		Schedulers.enableMetrics();

		SpringApplication.run(DemoApplication.class, args);
	}

	final ReplayProcessor<String> latestChange = ReplayProcessor.cacheLast();

	public DemoApplication(WebClient.Builder webClientBuilder) {
		FluxSink<String> sink = latestChange.sink(FluxSink.OverflowStrategy.LATEST);

		WebClient webClient = webClientBuilder.build();

		webClient.get()
				.uri("https://stream.wikimedia.org/v2/stream/recentchange")
				.retrieve()
				.bodyToFlux(JsonNode.class)
				.name("recentchange")
				.metrics()
				.limitRate(2000)
				.onBackpressureLatest()
				.concatMap(change -> {
					return processChange(change)
							.name("processing")
							.metrics()
							.delayElement(Duration.ofMillis(ThreadLocalRandom.current().nextInt(50, 500)))
							.onErrorResume(IllegalStateException.class, __ -> Mono.empty());
				})
				.doOnNext(sink::next)
				// Avoid polluting the logs
				.sample(Duration.ofSeconds(1))
				.log()
				.retryWhen(it -> it.delayElements(Duration.ofSeconds(1)))
				.subscribe();
	}

	@GetMapping("/latestChange")
	public Mono<String> latestChange() {
		return latestChange.next().delayElement(Duration.ofMillis(10));
	}

	Mono<String> processChange(JsonNode change) {
		if (change.path("bot").asBoolean()) {
			// return Mono.error(new IllegalStateException("OMG! I don't know how to handle the bots!"));
		}
		return Mono.just("Change to '" + change.path("title").asText(null) + "' by '" + change.path("user").asText(null) + "'");
	}

}
