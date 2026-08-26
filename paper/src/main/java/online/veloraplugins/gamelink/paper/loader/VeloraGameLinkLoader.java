package online.veloraplugins.gamelink.paper.loader;

import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import online.veloraplugins.engine.AbstractVeloraPluginLoader;

public final class VeloraGameLinkLoader extends AbstractVeloraPluginLoader {

    @Override
    protected String getPluginName() {
        return "VeloraGameLink";
    }

    @Override
    protected void loadDependencies(
        MavenLibraryResolver resolver
    ) {

        loadCoroutineDependencies(
            resolver
        );

        loadCloudDependencies(
            resolver
        );

        loadKotlinDependencies(
            resolver
        );

        loadOkaeriDependencies(
            resolver
        );

        loadRedisDependencies(
                resolver
        );
    }
}