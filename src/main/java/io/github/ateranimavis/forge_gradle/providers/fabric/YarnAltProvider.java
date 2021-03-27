package io.github.ateranimavis.forge_gradle.providers.fabric;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.gradle.api.Project;
import io.github.ateranimavis.forge_gradle.providers.utils.ZipHelper;
import net.minecraftforge.gradle.common.mapping.IMappingInfo;
import net.minecraftforge.gradle.common.mapping.detail.MappingDetails;
import net.minecraftforge.gradle.common.mapping.provider.CachingProvider;
import net.minecraftforge.gradle.common.util.HashStore;
import net.minecraftforge.gradle.common.util.MavenArtifactDownloader;
import net.minecraftforge.srgutils.IMappingFile;

/**
 * Note: this causes compile exceptions in the mapped jar
 */
public class YarnAltProvider extends CachingProvider {

    @Override
    public Collection<String> getMappingChannels() {
        return Collections.singleton("example_yarn_alt");
    }

    @Override
    public IMappingInfo getMappingInfo(Project project, String channel, String version) throws IOException {
        String mcVersion = extractMCVersion(version);

        File yarnZip = MavenArtifactDownloader.manual(project, "net.fabricmc:yarn:" + version + ":v2", false);
        File intermediaryZip = MavenArtifactDownloader.manual(project, "net.fabricmc:intermediary:" + mcVersion + ":v2", false);

        File tsrgFile = findRenames(project, "obf_to_srg", IMappingFile.Format.TSRG, mcVersion, false);
        if (tsrgFile == null)
            throw new IllegalStateException("Could not create " + version + " yarn mappings due to missing MCP's tsrg");

        File mcp = getMCPConfigZip(project, mcVersion);
        if (mcp == null)
            return null;

        File mappings = cacheMappings(project, channel, version, "zip");
        HashStore cache = commonHash(project, mcp)
            .load(cacheMappings(project, channel, version, "zip.input"))
            .add("tsrg", tsrgFile)
            .add("intermediary", intermediaryZip)
            .add("yarn", yarnZip)
            .add("version", version)
            .add("codever", "1");

        return fromCachable(channel, version, cache, mappings, () -> {
            // Intermediary: [INT->OBF]
            IMappingFile intermediary = ZipHelper.tinyFromZip(intermediaryZip, "intermediary", "official");

            // Yarn: [INT->MAP]
            IMappingFile yarn = ZipHelper.tinyFromZip(yarnZip, "intermediary", "named");

            // SRG: [OBF->SRG]
            IMappingFile obf_to_srg = IMappingFile.load(tsrgFile);

            // Mapped: [SRG->MAP]
            //   [INT->OBF] --chain--> [OBF->SRG] => [INT->SRG]
            //   [INT->SRG] -reverse->            => [SRG->INT]
            //   [SRG->INT] --chain--> [INT->MAP] => [SRG->MAP]
            return MappingDetails.fromSrg(intermediary.chain(obf_to_srg).reverse().chain(yarn));
        });
    }

    public static String extractMCVersion(String version) {
        int idx = version.lastIndexOf("+");
        return idx != -1 ? version.substring(0, idx) : version;
    }

    @Override
    public String toString() {
        return "Yarn Alt Provider";
    }

}
