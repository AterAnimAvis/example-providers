package io.github.ateranimavis.forge_gradle.providers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import io.github.ateranimavis.forge_gradle.providers.utils.ZipHelper;
import net.minecraftforge.gradle.common.mapping.IMappingDetail;
import net.minecraftforge.gradle.common.mapping.IMappingInfo;
import net.minecraftforge.gradle.common.mapping.MappingProviders;
import net.minecraftforge.gradle.common.mapping.detail.MappingDetail;
import net.minecraftforge.gradle.common.mapping.detail.MappingDetails;
import net.minecraftforge.gradle.common.mapping.detail.Node;
import net.minecraftforge.gradle.common.mapping.provider.CachingProvider;
import net.minecraftforge.gradle.common.util.HashStore;
import net.minecraftforge.gradle.common.util.MavenArtifactDownloader;
import net.minecraftforge.srgutils.IMappingFile;

/**
 * Note: this causes compile exceptions in the mapped jar
 */
public class YarnProvider extends CachingProvider {

    @Override
    public Collection<String> getMappingChannels() {
        return Collections.singleton("yarn");
    }

    @Override
    public IMappingInfo getMappingInfo(Project project, String channel, String version) throws IOException {
        String desc = "net.fabricmc:yarn:" + version + ":v2";
        File yarnZip = MavenArtifactDownloader.manual(project, desc, false);

        IMappingInfo intermediaryInfo = MappingProviders.getInfo(project, "intermediary", extractMCVersion(version));

        File mappings = cacheMappings(project, channel, version, "zip");
        HashStore cache = commonHash(project)
            .load(cacheMappings(project, channel, version, "zip.input"))
            .add("intermediary", intermediaryInfo.get())
            .add("yarn", yarnZip)
            .add("version", version)
            .add("codever", "1");

        return fromCachable(channel, version, cache, mappings, () -> {
            IMappingFile yarn = ZipHelper.tinyFromZip(yarnZip, "intermediary", "named");

            IMappingDetail intermediary = intermediaryInfo.getDetails();

            IMappingDetail overlay = MappingDetails.fromSrg(yarn);

            Map<String, IMappingDetail.INode> classNodes = chain(intermediary.getClasses(), overlay.getClasses());
            Map<String, IMappingDetail.INode> fieldNodes = chain(intermediary.getFields(), overlay.getFields());
            Map<String, IMappingDetail.INode> methodNodes = chain(intermediary.getMethods(), overlay.getMethods());
            Map<String, IMappingDetail.INode> paramNodes = chain(intermediary.getParameters(), overlay.getParameters());

            return MappingDetail.of(classNodes, fieldNodes, methodNodes, paramNodes);
        });
    }

    private static Map<String, IMappingDetail.INode> chain(Map<String, IMappingDetail.INode> original, Map<String, IMappingDetail.INode> overlay) {
        Map<String, IMappingDetail.INode> nodes = new HashMap<>(original);

        original.forEach((srg, s2i) -> {
            String intermediary = s2i.getMapped();

            if (!overlay.containsKey(intermediary)) return;

            IMappingDetail.INode i2m = overlay.get(intermediary);
            nodes.compute(srg, (_srg, old) ->
                Node.or(srg, old)
                    .withMapping(i2m.getMapped())
                    .withJavadoc(i2m.getJavadoc())
            );
        });

        return nodes;
    }

    public static String extractMCVersion(String version) {
        int idx = version.lastIndexOf("+");
        return idx != -1 ? version.substring(0, idx) : version;
    }

    @Override
    public String toString() {
        return "Yarn Provider";
    }
}