package io.github.ateranimavis.forge_gradle;

import javax.annotation.Nonnull;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import io.github.ateranimavis.forge_gradle.providers.ExampleSrgFileProvider;
import io.github.ateranimavis.forge_gradle.providers.FixedProvider;
import io.github.ateranimavis.forge_gradle.providers.overlay.OverlaidJavadocProvider;
import io.github.ateranimavis.forge_gradle.providers.overlay.OverlaidOfficialProvider;
import net.minecraftforge.gradle.mcp.mapping.MappingProviders;

@SuppressWarnings("unused")
public class ExampleProvidersPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        MappingProviders.register(
            new FixedProvider(),
            new ExampleSrgFileProvider(),
            new OverlaidOfficialProvider(),
            new OverlaidJavadocProvider()
        );
    }

}
