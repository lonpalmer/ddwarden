package com.lon

import com.fasterxml.jackson.databind.ObjectMapper
import com.lon.image.ImageProcessor
import com.lon.packfile.PackInfo
import spock.lang.Ignore
import spock.lang.Specification

class GenThumbsSepcification extends Specification {

    @Ignore
    def "Use the GenThumbs class to generate thumbnails"() {
        given:
        def packFile = new File("src/test/resources/FA_Assets_A_v3.13.dungeondraft_pack").getAbsolutePath()
        def packageInfo = new PackInfo(packFile, new ObjectMapper())
        def testThumbnailsPath = "/Users/lawrencepalmer/Downloads/test_thumbnails"

        // create the testThumbnailsPath if it doesn't exist
        new File(testThumbnailsPath).mkdirs()

        def imageProc = new ImageProcessor()

        when:
        packageInfo.scan()
        def buffer = packageInfo.getPackageBuffer()
        imageProc.makeThumbnailsForPack(packageInfo, buffer, testThumbnailsPath)

        then:
        noExceptionThrown()
    }
}
