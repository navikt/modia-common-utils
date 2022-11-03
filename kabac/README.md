# KABAC (Kotlin Attribute Based Access Control)

Pakke for å bygge opp komplekse tilgangskontroll-regler basert på attributter for ulike ressurser i applikasjonen, f.eks bruker eller http-request.

## Konsepter

- **Policy:** En klasse som beskriver en eller annen form for tilgangskontroll.  
  Man lager typisk sett flere av disse i en applikasjon, f.eks for å sjekke om en bruker har en gitt rolle. 
- **Policy information point (PIP):** En klasse som beskriver hvordan et informasjon element kan hentes ut.   
  Man lager typisk sett flere av disse i en applikasjon, f.eks for å hente ut brukers id og roller.  
- **Policy decision point (PDP):** En klasse ansvarlig for å holde orden på hvilke *PIPer* som finnes, og etablering av evaluerings konteksten.  
  Det er uvanlig at man bruker noe annet enn standarden `no.nav.personoversikt.common.PolicyDecisionPointImpl`.
- **Policy enforcement point (PEP):** En klasse ansvarlig for å evaluere en policy gitt en evaluerings kontekst, og om ønskelig få en rapport av utførelsen generert.  
  Det er uvanlig at man bruker noe annet enn standarden `no.nav.personoversikt.common.PolicyEnforcementPointImpl`.

**Eksempel PIP**
```kotlin
object UserIdPIP : Kabac.PolicyInformationPoint<UserId> {
    override val key = Key<UserId>(UserIdPIP)
    
    override fun provide(ctx: EvaluationContext): UserId {
      return getUserIdFromSomewhere()  
    }
}

class UserRolesPIP(private val service: UserRolesService) : Kabac.PolicyInformationPoint<List<String>> {
    override val key = Companion.key
    companion object {
        override val key = Key<List<String>>(UserRolesPIP)  
    }

    override fun provide(ctx: EvaluationContext): List<String> {
        val userId = ctx.getValue(UserIdPIP)
        return service.getRolesFor(userId)
    }
}
```

**Eksempel Policy**
```kotlin
enum class DenyCauses : Decision.DenyCause {
    MISSING_ROLE  
}

object HasAccessPolicy : Kabac.Policy { 
    override val key = Key<Kabac.Policy>(HasAccessPolicy)  
  
    override fun evaluate(ctx: EvaluationContext): Decision {
        val userRoles = ctx.getValue(UserRolesPip)
        if (userRoles.contains("access-to-my-app")) {
            Decision.Permit()
        } else {
            Decision.Deny("User does not have the correct role", DenyCauses.MISSING_ROLE)
        }
    }
}
```

I `HasAccessPolicy` vil kallet til `ctx.getValue(UserRolesPip)` medføre at `UserRolesPip::provide` kalles, som igjen kaller `UserIdPIP::provide`.

Om man i forkant av evaluering vet `UserId` eller det er informasjon som man ønsker å sende inn (f.eks ressursId ved oppslag),
så kan dette gjøres i det man evaluerer en policy eller når man oppretter evalueringskonteksten:
```kotlin
val enforcementPoint: Kabac.PolicyEnforcementPoint

val decision: Decision = enforcementPoint.evaluatePolicy(
    policy = HasAccessPolicy,
    attributes = listOf(
        UserIdPIP.key.withValue(UserId("userid-string"))
    )
)

// Alternately
val ctx = enforcementPoint.createEvaluationContext(
  listOf(
    UserIdPIP.key.withValue(UserId("userid-string"))
  )
)

val otherDecision = enforcementPoint.evaluatePolicyWithContext(
  policy = HasAccessPolicy,
  ctx = ctx
)
```

Ved å opprette evalueringskonteksten manuelt oppnår man at denne kan gjenbrukes på tvers av evalueringen av ulike policies.  
**NB!!** En evalueringskontekst bør aldri gjenbrukes på tvers av ulike brukere da informasjon fra PIP'er er cachet i konteksten.


## Standard oppsett

```kotlin
val decisionPoint : Kabac.PolicyDecisionPoint = PolicyDecisionPointImpl().apply {
    install(UserIdPIP)
    install(UserRolesPIP)
}

val enforcementPoint: Kabac.PolicyEnforcementPoint = PolicyEnforcementPointImpl(
  bias = Decision.Type.DENY,
  policyDecisionPoint = decisionPoint
)

val (decision, report) = enforcementPoint.evaluatePoliciesWithReport(
  combiningAlgorithm = CombiningAlgorithm.firstApplicable,
  bias = Decision.Type.PERMIT,
  attributes = listOf(UserIdPIP.key.withValue(UserId("userId"))),
  policies = listOf(
    HasAccesPolicy,
    AnotherPolicy
  )
)
```

## Testing av policies

Ved å inkludere `test-jar` til kabac-modulen får man tilgang til `KabacTestUtils.PolicyTester` som forenkler testing av policies.

```xml
<dependency>
    <groupId>no.nav.personoversikt</groupId>
    <artifactId>kabac</artifactId>
    <version>????</version>
    <type>test-jar</type>
    <scope>test</scope>
</dependency>
```

Testing av en enkel policy:
```kotlin
val policy = KabacTestUtils.PolicyTester(HasAccessPolicy)
val userRolesService = mockk<UserRolesService>()

@Test
fun `permit if user has correct role`() {
    every { userRolesService.getRolesFor(UserId("userId")) } returns listOf("access-to-my-app")
    policy.assertPermit(
      UserIdPIP.key.withValue(UserId("userId")),
      UserRolesPIP(userRolesService)
    )
}

@Test
fun `eny if user is missing role`() {
  every { userRolesService.getRolesFor(UserId("userId")) } returns listOf("another-role")
  policy.assertDeny(
    UserIdPIP.key.withValue(UserId("userId")),
    UserRolesPIP(userRolesService)
  ).withMessage("User does not have the correct role")
}
```