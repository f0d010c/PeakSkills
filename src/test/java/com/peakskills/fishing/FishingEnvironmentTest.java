package com.peakskills.fishing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FishingEnvironmentTest {
    @Test
    void classifiesDepthOnlyWhenDepthAndWaterVolumeQualify() {
        assertEquals(FishingDepth.SHALLOW, FishingEnvironment.classifyDepth(2, 75));
        assertEquals(FishingDepth.RIVERBED, FishingEnvironment.classifyDepth(3, 20));
        assertEquals(FishingDepth.DEEP_WATER, FishingEnvironment.classifyDepth(6, 35));
        assertEquals(FishingDepth.ABYSSAL, FishingEnvironment.classifyDepth(13, 50));
        assertEquals(FishingDepth.ANCIENT, FishingEnvironment.classifyDepth(25, 60));
        assertEquals(FishingDepth.SHALLOW, FishingEnvironment.classifyDepth(32, 5));
    }
}
