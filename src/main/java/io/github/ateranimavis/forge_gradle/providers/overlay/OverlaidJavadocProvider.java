package io.github.ateranimavis.forge_gradle.providers.overlay;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;
import net.minecraftforge.gradle.common.util.HashStore;
import net.minecraftforge.gradle.common.mapping.IMappingDetail;
import net.minecraftforge.gradle.common.mapping.IMappingInfo;
import net.minecraftforge.gradle.common.mapping.IMappingProvider;
import net.minecraftforge.gradle.common.mapping.MappingProviders;
import net.minecraftforge.gradle.common.mapping.detail.MappingDetail;
import net.minecraftforge.gradle.common.mapping.detail.Node;
import net.minecraftforge.gradle.common.mapping.provider.CachingProvider;
import net.minecraftforge.gradle.common.mapping.provider.OfficialMappingProvider;

/**
 * An example {@link IMappingProvider} that produces mappings based on {@link OfficialMappingProvider} with overlaid information.
 */
public class OverlaidJavadocProvider extends CachingProvider {

    @Override
    public Collection<String> getMappingChannels() {
        return Collections.singleton("example_javadoc");
    }

    /**
     * <p>
     * Supports Versions in the following formats: <br>
     * [CHANNEL]-[VERSION]-[JAVADOC_REVISION] <br>
     *   => Javadocs [VERSION]-[JAVADOC_REVISION] applied to ([CHANNEL], [VERSION]) <br>
     * [CHANNEL]-[VERSION]-[JAVADOC_REVISION]-[MAPPING-VERSION] <br>
     *   => Javadocs [VERSION]-[JAVADOC_REVISION] applied to ([CHANNEL], [MAPPING-VERSION]) <br>
     * </p>
     * <p>
     * Example: <br>
     * official-1.16.4-1.0.0 => Javadocs 1.16.4-1.0.0 applied on-top of (official, 1.16.4) mappings<br>
     * official-1.16.4-1.0.0-1.16.4-20201209.230658 => Javadocs 1.16.4-1.0.0 applied on-top of (official, 1.16.4-20201209.230658) mappings<br>
     * </p>
     */
    @Override
    public IMappingInfo getMappingInfo(Project project, String channel, String version) throws IOException {
        String[] parts = getVersion(version);

        String javadocVersion = parts[0];
        String sourceChannel = parts[1];
        String sourceVersion = parts[2];

        IMappingInfo source = MappingProviders.getInfo(project, sourceChannel, sourceVersion);

        String classes = getClassesForVersion(javadocVersion);
        String fields = getFieldsForVersion(javadocVersion);
        String methods = getMethodsForVersion(javadocVersion);
        String params = getParamsForVersion(javadocVersion);

        File mappings = cacheMappings(project, channel, version, "zip");
        HashStore cache = commonHash(project)
            .load(cacheMappings(project, channel, version, "zip.input"))
            .add("version", javadocVersion)
            .add("source", source.get())
            .add("codever", "1");

        return fromCachable(channel, version, cache, mappings, () -> {
            IMappingDetail detail = source.getDetails();

            Map<String, IMappingDetail.INode> classNodes = apply(classes, detail.getClasses());
            Map<String, IMappingDetail.INode> fieldNodes = apply(fields, detail.getFields());
            Map<String, IMappingDetail.INode> methodNodes = apply(methods, detail.getMethods());
            Map<String, IMappingDetail.INode> paramNodes = apply(params, detail.getParameters());

            return MappingDetail.of(classNodes, fieldNodes, methodNodes, paramNodes);
        });
    }

    private String getClassesForVersion(String version) {
        return "" +
            "searge,desc\n" +
            "net/minecraft/client/Minecraft,\"Who's Craft? - Doc Version {VERSION}\"\n".replace("{VERSION}", version);
    }

    private String getFieldsForVersion(String version) {
        return "" +
            "searge,desc\n" +
            "field_71432_P,\"If you stare into the abyss, the abyss stares back - Doc Version {VERSION}\"\n".replace("{VERSION}", version);
    }

    private String getMethodsForVersion(String version) {
        return "" +
            "searge,desc\n" +
            "func_71410_x,\"There can be only one - Doc Version {VERSION}\"\n".replace("{VERSION}", version);
    }

    private String getParamsForVersion(String version) {
        return "" +
            "searge,desc\n" +
            "p_i45547_1_,\"The Input Configuration - Doc Version {VERSION}\"\n".replace("{VERSION}", version);
    }

    protected String[] getVersion(String mapping) {
        String[] parts = mapping.split("-", 4);

        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid mapping version: " + mapping);
        }

        String source_channel = parts[0];
        String javadoc_version = parts[1] + "-" + parts[2];
        String source_version = parts.length == 4 ? parts[3] : parts[1];

        return new String[] {
            javadoc_version,
            source_channel,
            source_version
        };
    }

    private static Map<String, IMappingDetail.INode> apply(String data, Map<String, IMappingDetail.INode> source) throws IOException {
        Map<String, IMappingDetail.INode> nodes = new HashMap<>(source);

        try (NamedCsvReader csv = NamedCsvReader.builder().build(data)) {
            for (NamedCsvRow row : csv) {
                nodes.compute(row.getField("searge"), (srg, old) ->
                    Node.or(srg, old).withJavadoc(row.getField("desc"))
                );
            }
        }

        return nodes;
    }

    @Override
    public String toString() {
        return "Example Javadoc *any* Overlay";
    }
}
