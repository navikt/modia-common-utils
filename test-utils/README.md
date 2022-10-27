# Test Utils

Inneholder Junit4 `Rules` og Junit5 `Extensions` som kan være nyttige ved skriving av tester. 

## TestEnvironment

Oppsett for å sette opp miljøvariabler (`System.properties`) ved kjøring av tester.

Før hver test settes properties, og etter hver test blir de satt tilbake til sine originale verdier før testen startet.

Funksjonaliteten er tilgjengeliggjort vha `TestEnvironment.withEnvironment`. 
Om det er properties som skal gjelde for alle testene kan man derimot bruke `TestEnvironmentRule` eller `TestEnvironmentExtension`.
```kotlin
@Test
fun dotest() {
    val env = mapOf("key" to "value", "more" to "other")
    TestEnvironment.withEnvironment(env) {
        val something = service.fetchData(System.getProperty("more"))
    }
}
```

### Junit 4

```kotlin
@JvmField
@Rule
val environment = TestEnvironmentRule(
        "key" to "value",
        "more" to "other"
)

@Test
fun dotest() {
    val something = service.fetchData(System.getProperty("more"))
}
```

### Junit 5

```kotlin
@JvmField
@RegisterExtension
val environment = TestEnvironmentExtension()

@Test
fun dotest() {
    val something = service.fetchData(System.getProperty("more"))
}
```

## Snapshot testing

Forenklet måte å sammenligne to versjoner av objekter, og sikre at refaktoreringer ikke endrer på hvordan objektene ser ut.

En sammenligning kan gjøres på følgende måte i en test:

```kotlin
@RegisterExtension
val snapshot = SnapshotExtension()

@Test
fun test() {
    snapshot.assertMatches(someObjectToCheck)
}
```
Ved første kjøring vil testen feil siden det ikke eksisterer ett snapshot for denne testen. 
Kjører man testen igjen vil den rapportere ok.

Om objektet skal endres kan man enten slette snapshot-filen, eller kalle `snapshot.updateSnapshot(someObjectToCheck)` og deretter kjøre testene på nytt.

Basert på hva som skal sammenlignes kan man spesifisere `SnapshotRunner.Fileformat` som bestemmer hvordan objektene skal serialiseres før sammenligning.
Innebygd er tre format; `JsonSnapshotFormat.typed` (default brukt), `JsonSnapshotFormat.plain` og `TextSnapshotFormat`


**NB!!** Det er ikke anbefalt å bruke flere ulike `SnapshotRule` eller `SnapshotExtention` innenfor en test-metode.
Dette kan føre til at reglene skriver over hverandres filer.

### Junit 4

```kotlin
@JvmField
@Rule
val snapshot = SnapshotRule() // Vil bruke typedJson som default

@JvmField
@Rule
val textSnapshot = SnapshotRule(TextSnapshotFormat)

@Test
fun dotest() {
    val something = service.fetchData()
    snapshot.assertMatches(something)
}

@Test
fun doOtherTest() {
    val something = service.fetchData()
    textSnapshot.assertMatches(something)
}
```

### Junit 5

```kotlin
@JvmField
@RegisterExtension
val snapshot = SnapshotExtension()

@JvmField
@RegisterExtension
val textSnapshot = SnapshotRule(TextSnapshotFormat)

@Test
fun dotest() {
    val something = service.fetchData()
    snapshot.assertMatches(something)
}

@Test
fun doOtherTest() {
    val something = service.fetchData()
    textSnapshot.assertMatches(something)
}
```

Alternativt kan man registere extensions vha `@ExtendWith` i Junit5, se [AnnotatedSnapshotExtensionTest](src/test/kotlin/no/nav/personoversikt/test/snapshot/AnnotatedSnapshotExtensionTest.kt).

**NB!!** Om man ønsker å andre snapshot-format må man lage sub-classer for å spesifisere formatet før klassen sendes til JUnit 