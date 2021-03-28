package io.github.ateranimavis.forge_gradle.providers;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Set;

import org.apache.tools.ant.util.ReaderInputStream;
import org.gradle.api.Project;
import net.minecraftforge.gradle.common.mapping.detail.MappingDetails;
import net.minecraftforge.gradle.common.mapping.info.IMappingInfo;
import net.minecraftforge.gradle.common.mapping.provider.IMappingProvider;
import net.minecraftforge.gradle.common.util.HashStore;
import net.minecraftforge.srgutils.IMappingFile;

import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.cacheMappings;
import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.commonHash;
import static net.minecraftforge.gradle.common.mapping.util.CacheUtils.fromCacheable;

public class ExampleSrgFileProvider implements IMappingProvider {

    @Override
    public Set<String> getMappingChannels() {
        return Collections.singleton("example_srg");
    }

    @Override
    public IMappingInfo getMappingInfo(Project project, String channel, String version) throws IOException {
        // Anything not in the mappings will end up as SRG named
        String mappings = "" +
            "tsrg2 left right\n" +
            "net/minecraft/client/Minecraft net/minecraft/client/Minecraft\n" +
            "\tfield_71432_P instance\n" +
            "\tfunc_71410_x ()Lnet/minecraft/client/Minecraft; getInstance\n";

        File destination = cacheMappings(project, channel, version, "zip");
        HashStore cache = commonHash(project)
            .load(cacheMappings(project, channel, version, "zip.input"))
            .add("mappings", mappings)
            .add("codever", "1");

        return fromCacheable(channel, version, cache, destination, () ->
            MappingDetails.fromSrg(IMappingFile.load(new ReaderInputStream(new StringReader(mappings))))
        );
    }
}
