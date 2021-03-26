package io.github.ateranimavis.forge_gradle.providers.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MCPOverlaidProviderTest {

    @Test
    public void testVersionSplit() {
        assertArrayEquals(new String[] { "1.16.5-20201209.230658", "snapshot", "20210309-1.16.5" }, MCPOverlaidProvider.splitVersion("1.16.5-20201209.230658~20210309"));
        assertArrayEquals(new String[] { "1.16.5", "snapshot", "20210309-1.16.5" }, MCPOverlaidProvider.splitVersion("1.16.5-20210309"));
        assertArrayEquals(new String[] { "1.16.5", "snapshot", "20210309-1.16.5" }, MCPOverlaidProvider.splitVersion("1.16.5~20210309-1.16.5"));
        assertArrayEquals(new String[] { "1.16.5", "snapshot", "20210309-1.16.5" }, MCPOverlaidProvider.splitVersion("1.16.5~snapshot~20210309-1.16.5"));
        assertArrayEquals(new String[] { "1.16.5", "snapshot_nodoc", "20210309-1.16.5" }, MCPOverlaidProvider.splitVersion("1.16.5~snapshot_nodoc~20210309-1.16.5"));
        assertArrayEquals(new String[] { "1.16.5", "snapshot", "20210309-1.16.5" }, MCPOverlaidProvider.splitVersion("1.16.5~20210309"));
        assertArrayEquals(new String[] { "1.16.5", "snapshot", "20210309-1.16.4" }, MCPOverlaidProvider.splitVersion("1.16.5~20210309-1.16.4"));
        assertArrayEquals(new String[] { "1.16.5", "snapshot_nodoc", "20210309-1.16.5" }, MCPOverlaidProvider.splitVersion("1.16.5~snapshot_nodoc~20210309-1.16.5"));
        assertArrayEquals(new String[] { "1.16.5-20201209.230658", "snapshot", "20210309-1.16.5" }, MCPOverlaidProvider.splitVersion("1.16.5-20201209.230658~20210309"));
        assertArrayEquals(new String[] { "1.16.5-20201209.230658", "snapshot_nodoc", "20210309-1.16.5" }, MCPOverlaidProvider.splitVersion("1.16.5-20201209.230658~snapshot_nodoc~20210309-1.16.5"));
    }

}
