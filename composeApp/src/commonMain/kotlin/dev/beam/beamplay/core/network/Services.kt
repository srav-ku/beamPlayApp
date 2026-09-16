package dev.beam.beamplay.core.network

/**
 * Process-wide service locator, initialised once from `MainActivity.onCreate`
 * **before** `setContent { }`. Phase 8 moves this into an `Application`
 * subclass so it survives configuration changes; for now `android:configChanges`
 * in the manifest keeps the Activity alive across rotations.
 */
lateinit var services: ServiceContainer
    private set

/** Idempotent initialiser — safe to call more than once. */
fun initServices(isDebug: Boolean) {
    if (!::services.isInitialized) {
        services = ServiceContainer.create(isDebug = isDebug)
    }
}

/** Nullable accessor so other packages can fall back cleanly before init. */
val servicesOrNull: ServiceContainer?
    get() = if (::services.isInitialized) services else null