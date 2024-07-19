package com.lon.packfile

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore

class PackInfoTest extends Specification {

    def "Can we scan the file information from a pack file?"() {
        given:
        ObjectMapper jackson = new ObjectMapper()
        def packFilePath = new File("src/test/resources/FA_Assets_A_v3.13.dungeondraft_pack").getAbsolutePath()
        def packInfo = new PackInfo(packFilePath, jackson)

        when:
        def start = Instant.now()
        packInfo.scan()
        def duration = Duration.between(start, Instant.now())
        println("File count: ${packInfo.getFileCount()}")
        println("Scan time: ${duration.toMillis()}ms")

        then:
        noExceptionThrown()
        packInfo.getFileCount() > 0
        packInfo.getFileCount() == packInfo.getFiles().size()
    }


    @Ignore
    def "Test writing a pack file"() {
        given:
        ObjectMapper jackson = new ObjectMapper()
        def packFilePath = new File("src/test/resources/FA_Assets_A_v3.13.dungeondraft_pack").getAbsolutePath()
        def packInfo = new PackInfo(packFilePath, jackson)
        def testPath = "/tmp/test_pack_write"
        packInfo.scan()

        when:
        def start = Instant.now()
        packInfo.write()
        def duration = Duration.between(start, Instant.now())

        MoreFiles.deleteRecursively(Path.of(testPath), RecursiveDeleteOption.ALLOW_INSECURE)

        println("Write time: ${duration.toMillis()}ms")

        then:
        noExceptionThrown()
    }

    @Ignore
    def "Manual test to see how long it takes to decode a whole directory"() {
        given:
        var packDirectory = "/Users/lawrencepalmer/OneDrive/DungeonDraft"
        var queueLimit = new Semaphore(5)
        var packInfos = new ConcurrentLinkedQueue<PackInfo>()
        var individualPackTimes = new ConcurrentLinkedQueue<Duration>()
        var packFiles = new File(packDirectory).listFiles().findAll { it.name.endsWith(".dungeondraft_pack") }
        var tasks = new ArrayList<Thread>()
        var startTime = Instant.now()

        when:

        packFiles.each { packFile ->

            var task = Thread.ofVirtual().start(() -> {
                queueLimit.acquire()
                Instant packStartTime = Instant.now()
                var packInfo = new PackInfo(packFile.getAbsolutePath(), new ObjectMapper())
                packInfo.scan()
                packInfos.add(packInfo)
                individualPackTimes.add(Duration.between(packStartTime, Instant.now()))
                queueLimit.release()
            })
            tasks.add(task)
        }

        tasks.each { it.join() }

        var totalRunDuration = Duration.between(startTime, Instant.now())
        var averagePackTimeMs = individualPackTimes.stream().mapToLong { it.toMillis() }.average().getAsDouble()

        println("Total packs: ${packInfos.size()}")
        println("Total files: ${packInfos.stream().mapToInt { it.getFileCount() }.sum()}")
        println("Total run time: ${totalRunDuration.toMillis()}ms")
        println("Average pack time: ${averagePackTimeMs}ms")

        then:
        noExceptionThrown()


    }

}
