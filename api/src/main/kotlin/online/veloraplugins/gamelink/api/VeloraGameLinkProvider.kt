package online.veloraplugins.gamelink.api

object VeloraGameLinkProvider {

    private var api: VeloraGameLinkApi? = null

    fun get(): VeloraGameLinkApi {
        return requireNotNull(api) {
            "VeloraGameLink API is not available."
        }
    }

    fun isAvailable(): Boolean {
        return api != null
    }

    fun register(instance: VeloraGameLinkApi) {
        check(api == null) {
            "VeloraGameLink API is already registered."
        }

        api = instance
    }

    fun unregister() {
        api = null
    }
}