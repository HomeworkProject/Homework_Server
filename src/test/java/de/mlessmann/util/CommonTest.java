package de.mlessmann.util;

import de.mlessmann.common.Common;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Life4YourGames on 05.07.16.
 */
public class CommonTest {

    @Test
    public void negateInt() throws Exception {

        Assert.assertEquals(-2000, Common.negateInt(2000));

    }

    @Test
    public void getFirstVersion() throws Exception {

        Assert.assertEquals("4.5.0.102.2", Common.getFirstVersion("iaehfubezgwhwieivwi v4.5.0.102.2 fjeiwj"));

    }

    @Test
    public void compareVersions() throws Exception {

        Assert.assertEquals(0, Common.compareVersions("2.7.8", "2.7.8"));

        Assert.assertEquals(0, Common.compareVersions("2.7.8.0", "2.7.8"));

        Assert.assertEquals(0, Common.compareVersions("2.7.8", "2.7.8.0"));

        int a = 1;
        int b = Common.compareVersions("2.7.7", "2.7.8");
        Assert.assertEquals(a, b);

        Assert.assertEquals(-1, Common.compareVersions("2.7.7", "2.7.6"));

    }

}