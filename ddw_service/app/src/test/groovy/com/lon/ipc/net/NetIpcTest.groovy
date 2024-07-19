package com.lon.ipc.net

import com.fasterxml.jackson.databind.ObjectMapper
import com.lon.App
import com.lon.ipc.IDdwMessage
import com.lon.ipc.StreamingMessageCoder
import com.lon.ipc.message.KeepAliveMessage
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue

@Ignore
class NetIpcTest extends Specification {

    @Shared
    App app

    def setupSpec() {
        app = DaggerAppNetTestComponent.create().app()
        Thread.ofPlatform().start(()->app.start())
    }

    @Ignore
    @SuppressWarnings('GroovyInfiniteLoopStatement')
    def "Test network ipc"() {
        given:
        def iterations = 5
        def messageCoder = new StreamingMessageCoder(new ObjectMapper())
        def socket = new Socket("localhost", 60123)
        def receivedMessages = new LinkedBlockingQueue<IDdwMessage>()
        def start = Instant.now()

        Thread.ofVirtual().start(()->{
            var inStream = socket.getInputStream()
            while(true) {
                int read
                while((read = inStream.read()) != -1) {
                    var rxMsg = messageCoder.decode(read)
                    rxMsg.ifPresent({ receivedMessages.add(it) })
                }
            }
        })

        def outStream = socket.getOutputStream()

        when:
        for(int i in 1..iterations) {
            outStream.write(messageCoder.encode(new KeepAliveMessage()))
            outStream.flush()
        }

        while(receivedMessages.size() < iterations && Duration.between(start, Instant.now()).toSeconds() < 5) {
            Thread.yield()
        }

        println("Total message time: " + Duration.between(start, Instant.now()).toMillis() + "ms")

        then:
        receivedMessages.size() == iterations
    }


}
