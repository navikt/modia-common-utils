package no.nav.personoversikt.kabac

object KabacException {
    class MissingPolicyInformationPointException(message: String) : IllegalStateException(message)
    class CyclicDependenciesException(message: String) : IllegalStateException(message)
}
