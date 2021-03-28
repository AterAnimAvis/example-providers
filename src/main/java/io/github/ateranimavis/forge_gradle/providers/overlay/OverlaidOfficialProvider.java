package io.github.ateranimavis.forge_gradle.providers.overlay;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;
import net.minecraftforge.gradle.common.mapping.MappingProviders;
import net.minecraftforge.gradle.common.mapping.detail.IMappingDetail;
import net.minecraftforge.gradle.common.mapping.info.IMappingInfo;
import net.minecraftforge.gradle.common.mapping.provider.IMappingProvider;
import net.minecraftforge.gradle.common.mapping.provider.OfficialMappingProvider;
import net.minecraftforge.gradle.common.util.HashStore;

import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.cacheMappings;
import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.commonHash;
import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.fromCacheable;

/**
 * An example {@link IMappingProvider} that produces mappings based on {@link OfficialMappingProvider} with overlaid information.
 */
public class OverlaidOfficialProvider implements IMappingProvider {

    @Override
    public Set<String> getMappingChannels() {
        return Collections.singleton("example_official");
    }

    /**
     * <p>
     * Supports Versions in the following formats: <br> [VERSION]-[JAVADOC-VERSION] <br> => Javadocs [JAVADOC-VERSION] applied to (official, [VERSION]) <br>
     * </p>
     * <p>
     * Example: <br> 1.16.4-rev3 => Javadocs rev3 applied on-top of (official, 1.16.4) mappings<br> 1.16.4-20201209.230658-rev3 => Javadocs rev3 applied on-top of (official, 1.16.4-20201209.230658) mappings<br>
     * </p>
     */
    @Override
    public IMappingInfo getMappingInfo(Project project, String channel, String versionIn) throws IOException {
        String[] parts = splitVersions(versionIn);

        String channelVersion = parts[0];
        String version = parts[1];

        IMappingInfo official = MappingProviders.getInfo(project, OfficialMappingProvider.OFFICIAL_CHANNEL, channelVersion);

        String classes = "" +
            "searge,name,side,desc\n" +
            "net/minecraft/client/Minecraft,net/minecraft/client/Minecraft,0,\"Who's Craft? - Doc Version {VERSION}\"\n"
                .replace("{VERSION}", version);

        String fields = "" +
            "searge,name,side,desc\n" +
            "field_71432_P,theAbyss,0,\"If you stare into the abyss, the abyss stares back\"\n";

        String methods = "" +
            "searge,name,side,desc\n" +
            "func_71410_x,getHighlander,0,\"There can be only one\"\n";

        String params = "" +
            "param,name,side\n" +
            "p_i45547_1_,configurationIn,0\n";

        File mappings = cacheMappings(project, channel, versionIn, "zip");
        HashStore cache = commonHash(project)
            .load(cacheMappings(project, channel, versionIn, "zip.input"))
            .add("official", official.get())
            .add("version", version)
            .add("codever", "1");

        return fromCacheable(channel, versionIn, cache, mappings, () -> {
            IMappingDetail detail = official.getDetails();

            Map<String, IMappingDetail.INode> classNodes = apply(classes, detail.getClasses());
            Map<String, IMappingDetail.INode> fieldNodes = apply(fields, detail.getFields());
            Map<String, IMappingDetail.INode> methodNodes = apply(methods, detail.getMethods());
            Map<String, IMappingDetail.INode> paramNodes = apply(params, detail.getParameters());

            return IMappingDetail.of(classNodes, fieldNodes, methodNodes, paramNodes);
        });
    }

    protected String[] splitVersions(String version) {
        int idx = version.lastIndexOf('-');

        if (idx == -1 || version.length() == idx) {
            throw new IllegalArgumentException("Invalid mapping version: " + version);
        }

        return new String[] {
            version.substring(0, idx),
            version.substring(idx + 1)
        };
    }

    private static Map<String, IMappingDetail.INode> apply(String data, Map<String, IMappingDetail.INode> input) throws IOException {
        HashMap<String, IMappingDetail.INode> nodes = new HashMap<>(input);

        try (NamedCsvReader csv = NamedCsvReader.builder().build(data)) {
            boolean hasDesc = csv.getHeader().contains("desc");

            for (NamedCsvRow row : csv) {
                String mapped = row.getField("name");
                String desc = hasDesc ? row.getField("desc") : "";

                nodes.compute(row.getField("searge"), (srg, old) ->
                    mapped.isEmpty()
                        ? IMappingDetail.INode.or(srg, old)
                        .withJavadoc(desc)
                        : IMappingDetail.INode.or(srg, old)
                            .withMapping(mapped)
                            .withJavadoc(desc)
                );
            }
        }

        return nodes;
    }

    @Override
    public String toString() {
        return "Example Official Overlay";
    }
}
