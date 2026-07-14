package io.duckling.contestpulse.data.remote.custom

import java.net.InetAddress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSourceAddressTest {
    @Test
    fun privateAndLocalAddresses_areRejected() {
        listOf("127.0.0.1", "10.0.0.1", "172.16.0.1", "192.168.1.1", "169.254.169.254", "::1", "fc00::1")
            .forEach { value ->
                assertFalse(value, InetAddress.getByName(value).isPublicInternetAddress())
            }
    }

    @Test
    fun publicAddresses_areAccepted() {
        assertTrue(InetAddress.getByName("1.1.1.1").isPublicInternetAddress())
        assertTrue(InetAddress.getByName("2606:4700:4700::1111").isPublicInternetAddress())
    }
}
