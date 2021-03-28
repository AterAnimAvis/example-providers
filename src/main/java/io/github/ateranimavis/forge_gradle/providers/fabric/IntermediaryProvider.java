package io.github.ateranimavis.forge_gradle.providers.fabric;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.gradle.api.Project;
import io.github.ateranimavis.forge_gradle.providers.utils.ZipHelper;
import net.minecraftforge.gradle.common.mapping.detail.MappingDetails;
import net.minecraftforge.gradle.common.mapping.info.IMappingInfo;
import net.minecraftforge.gradle.common.mapping.provider.IMappingProvider;
import net.minecraftforge.gradle.common.util.HashStore;
import net.minecraftforge.gradle.common.util.MavenArtifactDownloader;
import net.minecraftforge.srgutils.IMappingFile;

import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.cacheMappings;
import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.commonHash;
import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.findRenames;
import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.fromCacheable;
import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.getMCPConfigZip;

/**
 * I haven't checked to ensure validity of the resulting jars
 */
public class IntermediaryProvider implements IMappingProvider {

    @Override
    public Set<String> getMappingChannels() {
        return Collections.singleton("example_intermediary");
    }

    @Override
    public IMappingInfo getMappingInfo(Project project, String channel, String version) throws IOException {
        String desc = "net.fabricmc:intermediary:" + version + ":v2";
        File intermediaryZip = MavenArtifactDownloader.manual(project, desc, false);

        File tsrgFile = findRenames(project, "obf_to_srg", IMappingFile.Format.TSRG, version, false);
        if (tsrgFile == null)
            throw new IllegalStateException("Could not create " + version + " intermediary mappings due to missing MCP's tsrg");

        File mcp = getMCPConfigZip(project, version);
        if (mcp == null) // TODO: handle when MCPConfig zip could not be downloaded
            throw new IllegalStateException("Could not create " + version + " official mappings due to missing MCPConfig zip");

        File mappings = cacheMappings(project, channel, version, "zip");
        HashStore cache = commonHash(project, mcp)
            .load(cacheMappings(project, channel, version, "zip.input"))
            .add("intermediary", intermediaryZip)
            .add("tsrg", tsrgFile)
            .add("version", version)
            .add("codever", "1");

        return fromCacheable(channel, version, cache, mappings, () -> {
            // Intermediary: [INT->OBF]
            IMappingFile intermediary = ZipHelper.tinyFromZip(intermediaryZip, "intermediary", "official");

            // SRG: [OBF->SRG]
            IMappingFile obf_to_srg = IMappingFile.load(tsrgFile);

            // Mapped: [SRG->INT]
            //   [INT->OBF] --chain--> [OBF->SRG] => [INT->SRG]
            //   [INT->SRG] -reverse->            => [SRG->INT]
            return MappingDetails.fromSrg(intermediary.chain(obf_to_srg).reverse());
        });
    }
}
