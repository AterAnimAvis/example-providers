package io.github.ateranimavis.forge_gradle.providers.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;

public class ZipHelper {

    public static IMappingFile tinyFromZip(File zip, String a, String b) throws IOException {
        return fromZip(zip, "mappings/mappings.tiny", a, b);
    }

    public static IMappingFile fromZip(File zip, String file, String a, String b) throws IOException {
        byte[] data = Utils.getZipData(zip, file);
        return INamedMappingFile.load(new ByteArrayInputStream(data)).getMap(a, b);
    }

}
