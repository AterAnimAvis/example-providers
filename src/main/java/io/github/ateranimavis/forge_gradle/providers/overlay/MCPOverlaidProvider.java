package io.github.ateranimavis.forge_gradle.providers.overlay;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import net.minecraftforge.gradle.common.mapping.IMappingProvider;
import net.minecraftforge.gradle.common.util.HashStore;
import net.minecraftforge.gradle.common.mapping.IMappingDetail;
import net.minecraftforge.gradle.common.mapping.IMappingInfo;
import net.minecraftforge.gradle.common.mapping.MappingProviders;
import net.minecraftforge.gradle.common.mapping.detail.MappingDetail;
import net.minecraftforge.gradle.common.mapping.detail.Node;
import net.minecraftforge.gradle.common.mapping.provider.OfficialMappingProvider;

import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.*;

public class MCPOverlaidProvider implements IMappingProvider {
    @Override
    public Collection<String> getMappingChannels() {
        return Collections.singleton("example_official_mcp");
    }

    @Override
    public IMappingInfo getMappingInfo(Project project, String channel, String version) throws IOException {
        String[] parts = splitVersion(version);
        String official_version = parts[0];
        String mcp_channel = parts[1];
        String mcp_version = parts[2];

        IMappingInfo official = MappingProviders.getInfo(project, OfficialMappingProvider.OFFICIAL_CHANNEL, official_version);
        IMappingInfo mcp = MappingProviders.getInfo(project, mcp_channel, mcp_version);

        File mappings = cacheMappings(project, channel, version, "zip");
        HashStore cache = commonHash(project)
            .load(cacheMappings(project, channel, version, "zip.input"))
            .add("official", official.get())
            .add("mcp_mappings", mcp.get())
            .add("version", version)
            .add("codever", "1");

        return fromCachable(channel, version, cache, mappings, () -> {
            IMappingDetail detail = official.getDetails();
            IMappingDetail overlay = mcp.getDetails();

            Map<String, IMappingDetail.INode> classNodes = merge(overlay.getClasses(), detail.getClasses(), false);
            Map<String, IMappingDetail.INode> fieldNodes = merge(overlay.getFields(), detail.getFields(), false);
            Map<String, IMappingDetail.INode> methodNodes = merge(overlay.getMethods(), detail.getMethods(), false);
            Map<String, IMappingDetail.INode> paramNodes = merge(overlay.getParameters(), detail.getParameters(), true);

            return MappingDetail.of(classNodes, fieldNodes, methodNodes, paramNodes);
        });
    }

    private static Map<String, IMappingDetail.INode> merge(Map<String, IMappingDetail.INode> overlay, Map<String, IMappingDetail.INode> original, boolean fully) {
        Map<String, IMappingDetail.INode> nodes = new HashMap<>(original);
        Map<String, IMappingDetail.INode> data = fully ? overlay : original;

        data.forEach((srg, orig) -> {
            if (!overlay.containsKey(srg)) return;

            IMappingDetail.INode node = overlay.get(srg);
            nodes.compute(srg, (_k1, old) ->
                Node.or(srg, old)                   // Potentially create a new node, this should only occur if `fully`
                    .withMapping(orig.getMapped())  // `orig` will either be from `overlay` if `fully` or `official` if not
                    .withJavadoc(node.getJavadoc()) // `node` will definitely be from `overlay`
            );
        });

        return nodes;
    }

    /**
     * @return an array of `official_version`, `mcp_channel`, `mcp_version`
     */
    public static String[] splitVersion(String version) {
        String[] parts = version.split("~", 3);

        if (parts.length == 1) parts = version.split("-", 3);

        if (parts.length == 2) {
            if (!parts[1].contains("-")) parts[1] = parts[1] + "-" + OfficialMappingProvider.getMinecraftVersion(parts[0]);

            return new String[] { parts[0], "snapshot", parts[1] };
        }

        if (parts.length == 3) {
            if (!parts[2].contains("-")) parts[2] = parts[2] + "-" + OfficialMappingProvider.getMinecraftVersion(parts[0]);

            return parts;
        }

        throw new IllegalArgumentException("Invalid mapping version: " + version);
    }

    @Override
    public String toString() {
        return "Official + MCP Javadocs + Parameters";
    }
}
