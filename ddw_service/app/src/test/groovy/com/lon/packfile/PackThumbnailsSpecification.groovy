package com.lon.packfile

import com.fasterxml.jackson.databind.ObjectMapper
import com.lon.image.ImageProcessor
import spock.lang.Ignore
import spock.lang.Specification

@Ignore
class PackThumbnailsSpecification extends Specification {

    @Ignore
    def "Scan a package file and make thumbnails for it"() {
        given:
        var packFile = new File("src/test/resources/FA_Assets_A_v3.13.dungeondraft_pack").getAbsolutePath()
        var packageInfo = new PackInfo(packFile, new ObjectMapper())
        var testThumbnailsPath = "/Users/lawrencepalmer/Downloads/test_thumbnails"

        // create the testThumbnailsPath if it doesn't exist
        new File(testThumbnailsPath).mkdirs()

        var imageProc = new ImageProcessor()


        when:
        packageInfo.scan()
        var buffer = packageInfo.getPackageBuffer()
        imageProc.makeThumbnailsForPack(packageInfo, buffer, testThumbnailsPath)

        then:
        noExceptionThrown()
    }
}
