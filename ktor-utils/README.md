# Ktor Utils

Inneholder små funksjoner, moduler og plugins for forenkling av oppsett av ktor apper


## Metrics

**Setup:**
```kotlin
application {
    install(Metrics.Plugin) {
        contextpath = "path-to-app-if-any (optional)"
        // Configuration of MicrometerMetrics exposed as well
        meterBinders = emptyList() // e.g disable all standard metrics
    }
}
```

**Register a metric:**
```kotlin
val counter = Metrics.Registry.counter("my-counter")
val timer = Metrics.Registry.timer("my-timer")
val gauge = Metrics.Registry.gauge("my-gauge")
```
