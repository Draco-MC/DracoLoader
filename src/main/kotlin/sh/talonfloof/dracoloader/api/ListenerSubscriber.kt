package sh.talonfloof.dracoloader.api

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ListenerSubscriber(val value: EnvironmentType = EnvironmentType.COMMON)
