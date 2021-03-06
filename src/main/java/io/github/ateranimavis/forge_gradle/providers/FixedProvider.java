package io.github.ateranimavis.forge_gradle.providers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import net.minecraftforge.gradle.common.mapping.MappingProviders;
import net.minecraftforge.gradle.common.mapping.info.IMappingInfo;
import net.minecraftforge.gradle.common.mapping.provider.IMappingProvider;

public class FixedProvider implements IMappingProvider {

    @Override
    public Set<String> getMappingChannels() {
        return Collections.singleton("example_fixed");
    }

    private static final Map<String, String> SOURCE_FOR_VERSION = new HashMap<>();

    static {
        SOURCE_FOR_VERSION.put("1.15.x", "mcp_snapshot-20210106-1.15.1");
        SOURCE_FOR_VERSION.put("1.16.x", "mcp_snapshot-20201028-1.16.3");
    }

    @Override
    public IMappingInfo getMappingInfo(Project project, String channel, String version) throws IOException {
        if (!SOURCE_FOR_VERSION.containsKey(version)) {
            throw new IllegalArgumentException("Invalid mapping version: " + channel + "_" + version);
        }

        return IMappingInfo.of(channel, version, MappingProviders.getInfo(project, SOURCE_FOR_VERSION.get(version)).get());
    }
}
