# Typeanalyzer
 
I tilfeller hvor man har brukt Java klasser, eller `Map<String, Any?>` kan det være tungt å vite 
om felter kan ha `null` verdier, eller om hvilke felter man kan komme til å støte på.

For å hjelpe til med å finne ut av dette kan man bruke pakken for å analysere objektene slik, 
og deretter bygge en data-modell som stemmer overens med hva man returnerte.

**Bruk:**
```kotlin
val serviceAnalyzer = Typeanalyzer()
val domainAnalyzer = Typeanalyzer()

fun somemethod(): Map<String, Any?> {
    val data: Map<String, Any?> = service.getSomeUntypedData()
    serviceAnalyzer.capture(data)
    return data
    
    // Alternativt;
    // return service.getSomeUntypedData()
    //    .also { serviceAnalyzer.capture(it) }
}

fun someOtherMethod(): SomeDomainObject {
    val data: SomeDomainObject = service.getJavaObject()
    domainAnalyzer.capture(data)
    return data

    // Alternativt;
    // return service.getSomeUntypedData()
    //    .also { domainAnalyzer.capture(it) }
}
```

**NB** Det er ikke anbefalt å gjenbrukt `Typeanalyzer` på tvers av objekter som man vet skal være ulike.
Dette vil sannsynligvisvis føre til at feil kastes, eller at alle felter blir satt med `nullable: true`.

`Typeanalyzer` bygger opp en indre modell av hvordan dataene ser ut, hvilke felter som kan være `null` og så videre ([eksempel](src/test/resources/snapshots/no.nav.personoversikt.typeanalyzer.TypeanalyzerSnapshotTest_should create be able to create simple analysis$typeanalyzer-0.json)).

Den indre modellen kan hentes ut vha `analyzer.report()` som returnerer den nåværende og mest oppdaterte modellen.
Samme modell kan også brukes til å generere pseudo-kode for kotlin og typescript vha `analyzer.print`.

```kotlin
val analyzer = Typeanalyzer()
// Do some capturing
val kotlinCode = analyzer.print(KotlinFormat)
val tsCode = analyzer.print(TypescriptFormat)
```
[Kotlin eksempel](src/test/resources/snapshots/no.nav.personoversikt.typeanalyzer.TypeanalyzerSnapshotTest_pretty%20print$typeanalyzer-0.txt)
[Typescript eksempel](src/test/resources/snapshots/no.nav.personoversikt.typeanalyzer.TypeanalyzerSnapshotTest_pretty%20print$typeanalyzer-1.txt)

**Forklaring:**

"Nullability" blir avslappet for felter når `Typeanalyzer` støter på ett felt eller verdi med verdien `null`
```kotlin
val analyzer = Typeanalyzer()
analyzer.capture(listOf("value1"))  // ListCapture[PrimitiveCapture[type = CaptureType.TEXT, nullable = false]]
analyzer.capture(listOf(null))      // ListCapture[PrimitiveCapture[type = CaptureType.TEXT, nullable = true]]
```

"Type" blir spesifisert mer om det er mulig og analyseren ikke har sett en annen spesifik type for feltet.
```kotlin
val analyzer = Typeanalyzer()
analyzer.capture(listOf(null))      // ListCapture[NullCapture[]]
analyzer.capture(listOf("value1"))  // ListCapture[PrimitiveCapture[type = CaptureType.TEXT, nullable = true]]
```

I forrige eksempel var det observert en `null` verdi i listen, og feltet ble derfor markert som `nullable`.
Om listen kan vi derimot ikke ulede noe typeinformasjon, og feltet settes til en spesialt type `UnknownCapture`.
```kotlin
val analyzer = Typeanalyzer()
analyzer.capture(listOf())      // ListCapture[UnknownCapture[]]
analyzer.capture(listOf("value1"))  // ListCapture[PrimitiveCapture[type = CaptureType.TEXT, nullable = false]]
analyzer.capture(listOf(null))  // ListCapture[PrimitiveCapture[type = CaptureType.TEXT, nullable = true]]
```