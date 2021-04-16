package io.github.ateranimavis.forge_gradle;

import java.util.Objects;
import javax.annotation.Nonnull;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import io.github.ateranimavis.forge_gradle.providers.ExampleSrgFileProvider;
import io.github.ateranimavis.forge_gradle.providers.FixedProvider;
import io.github.ateranimavis.forge_gradle.providers.fabric.IntermediaryProvider;
import io.github.ateranimavis.forge_gradle.providers.fabric.YarnAltProvider;
import io.github.ateranimavis.forge_gradle.providers.fabric.YarnProvider;
import io.github.ateranimavis.forge_gradle.providers.overlay.MCPOverlaidProvider;
import io.github.ateranimavis.forge_gradle.providers.overlay.OverlaidJavadocProvider;
import io.github.ateranimavis.forge_gradle.providers.overlay.OverlaidOfficialProvider;
import net.minecraftforge.gradle.common.mapping.MappingProviders;

@SuppressWarnings("unused")
public class ExampleProvidersPlugin implements Plugin<Project> {

    private static final String FABRIC_MC_MAVEN = "FabricMC Maven";

    private static final String[] FORGE_PLUGINS = new String[] {
        "net.minecraftforge.gradle",
        "net.minecraftforge.gradle.patcher",

        // Uncommon Aliases but still exist in FG4
        "net.minecraftforge.gradle.forgedev",
        "net.minecraftforge.gradle.forgedev.patcher"
    };

    @Override
    public void apply(@Nonnull Project project) {
        // Why use PluginManager#withPlugin? It's so we're not dependent on the plugin apply order
        for (String plugin : FORGE_PLUGINS)
            project.getPluginManager().withPlugin(plugin, _plugin -> apply());

        project.afterEvaluate(p -> {
            if (project.getRepositories().stream().noneMatch(it -> Objects.equals(it.getName(), FABRIC_MC_MAVEN))) {
                project.getRepositories().maven(e -> {
                    e.setName(FABRIC_MC_MAVEN);
                    e.setUrl("https://maven.fabricmc.net/");
                    e.metadataSources(m -> {
                        m.gradleMetadata();
                        m.mavenPom();
                        m.artifact();
                    });
                    e.mavenContent(action -> action.includeGroup("net.fabricmc"));
                });
            }
        });
    }

    private void apply() {
        // Why have a name? It's used in debugging and makes it fairly to register new IMappingProviders without
        //   worrying about how many times `apply` gets called.

        MappingProviders.register("example:intermediary", new IntermediaryProvider());
        MappingProviders.register("example:yarn",         new YarnProvider());          //TODO: Causes compilation Exceptions in resulting Mapped Code
        MappingProviders.register("example:yarn_alt",     new YarnAltProvider());       //TODO: Causes compilation Exceptions in resulting Mapped Code
        MappingProviders.register("example:overlaid",     new MCPOverlaidProvider());
        MappingProviders.register("example:fixed",        new FixedProvider());
        MappingProviders.register("example:srg",          new ExampleSrgFileProvider());
        MappingProviders.register("example:official",     new OverlaidOfficialProvider());
        MappingProviders.register("example:javadoc",      new OverlaidJavadocProvider());
    }

}
