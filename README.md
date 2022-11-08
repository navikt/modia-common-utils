# modia-common-utils

Felleskomponenter for ting på tvers i løsningene til team personoversikt

## Ta ibruk

Pakkene publiseres til [github package registry](https://github.com/orgs/navikt/packages?repo_name=modia-common-utils)(ghpr) og [jitpack.io](https://jitpack.io/#navikt/modia-common-utils).
For å bruke en modul må man derfor gjøre en av følgende;
- legge til ett personal-access-token som gir til gang til ghpr
- bruke en proxy mot ghpr som håndterer autentisering
- legge til jitpack.io som repository i ditt byggeverktøy

**Jitpack + maven:**
```xml
<dependencies>
    <dependency>
        <groupId>com.github.navikt.modia-common-utils</groupId>
        <artifactId>kotlin-utils</artifactId>
        <version>${modia-common-utils.version}</version>
    </dependency>
</dependencies>

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

**Jitpack + gradle:**
```kotlin
val modiaCommonVersion = "TODO()"

allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
dependencies {
    implementation "com.github.navikt.modia-common-utils:kotlin-utils:$modiaCommonVersion"
}
```

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan rettes mot:

[Team Personoversikt](https://github.com/navikt/info-team-personoversikt)
