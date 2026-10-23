package com.admin.vpnshare.shizuku

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.IBinder
import android.os.ParcelFileDescriptor
import java.lang.reflect.Method
import java.net.InetAddress

val TEST_NETWORK_DNS_SERVERS: List<InetAddress>
    get() = listOf("2001:4860:4860::8888", "8.8.8.8", "1.1.1.1").map(InetAddress::getByName)

fun buildLinkAddress(address: InetAddress, prefixLength: Int): LinkAddress {
    val constructor = LinkAddress::class.java
        .getDeclaredConstructor(InetAddress::class.java, Int::class.javaPrimitiveType)
    constructor.isAccessible = true
    return constructor.newInstance(address, prefixLength)
}

fun resolveTransportTest(): Int = runCatching {
    NetworkCapabilities::class.java.getField("TRANSPORT_TEST").getInt(null)
}.getOrDefault(TRANSPORT_TEST_AOSP_FALLBACK)

const val TRANSPORT_TEST_AOSP_FALLBACK = 7

class TestNetworkApi(private val context: Context) {
    private val managerClass: Class<*>? = runCatching {
        Class.forName("android.net.TestNetworkManager")
    }.getOrNull()

    @SuppressLint("WrongConstant")
    private val manager: Any? = runCatching {
        context.getSystemService("test_network")
    }.getOrNull()

    val isAvailable: Boolean get() = managerClass != null && manager != null

    fun createTunInterface(addresses: List<LinkAddress>): TunHandle {
        val instance = manager ?: error("createTunInterface: test_network service unavailable")
        val owner = managerClass ?: error("createTunInterface: TestNetworkManager class absent")
        val created = invokeCollectionOverload(owner, instance, addresses)
            ?: invokeArrayOverload(owner, instance, addresses)
            ?: error("createTunInterface: no known overload accepted LinkAddresses $addresses")
        return TunHandle(created)
    }

    private fun invokeCollectionOverload(
        owner: Class<*>,
        instance: Any,
        addresses: List<LinkAddress>,
    ): Any? = runCatching {
        val method = owner.getMethod("createTunInterface", Collection::class.java)
        method.invoke(instance, addresses)
    }.getOrNull()

    private fun invokeArrayOverload(
        owner: Class<*>,
        instance: Any,
        addresses: List<LinkAddress>,
    ): Any? = runCatching {
        val arrayType = Class.forName("[Landroid.net.LinkAddress;")
        val method = owner.getMethod("createTunInterface", arrayType)
        val argument = java.lang.reflect.Array.newInstance(LinkAddress::class.java, addresses.size)
        addresses.forEachIndexed { index, address ->
            java.lang.reflect.Array.set(argument, index, address)
        }
        method.invoke(instance, argument)
    }.getOrNull()

    fun setupTestNetwork(interfaceName: String, dnsServers: List<InetAddress>, binder: IBinder) {
        val instance = manager ?: error("setupTestNetwork: test_network service unavailable")
        val owner = managerClass ?: error("setupTestNetwork: TestNetworkManager class absent")
        val properties = LinkProperties().apply {
            setInterfaceName(interfaceName)
            val addDnsServer = LinkProperties::class.java
                .getMethod("addDnsServer", InetAddress::class.java)
            dnsServers.forEach { addDnsServer.invoke(this, it) }
        }
        if (setupWithLinkProperties(owner, instance, properties to binder)) return
        val method = owner.getMethod("setupTestNetwork", String::class.java, IBinder::class.java)
        method.invoke(instance, interfaceName, binder)
    }

    private fun setupWithLinkProperties(
        owner: Class<*>,
        instance: Any,
        request: Pair<LinkProperties, IBinder>,
    ): Boolean = runCatching {
        val method = owner.getMethod(
            "setupTestNetwork",
            LinkProperties::class.java,
            Boolean::class.javaPrimitiveType,
            IBinder::class.java,
        )
        method.invoke(instance, request.first, true, request.second)
    }.isSuccess

    fun teardownTestNetwork(network: Network) {
        val instance = manager ?: return
        val owner = managerClass ?: return
        val method = owner.getMethod("teardownTestNetwork", Network::class.java)
        method.invoke(instance, network)
    }
}

class TunHandle(private val delegate: Any) {
    val interfaceName: String
        get() {
            val method = delegate.javaClass.getMethod("getInterfaceName")
            return method.invoke(delegate) as String
        }
    val fileDescriptor: ParcelFileDescriptor
        get() {
            val method = delegate.javaClass.getMethod("getFileDescriptor")
            return method.invoke(delegate) as ParcelFileDescriptor
        }
}

class TetheringPreferenceApi(private val context: Context) {
    private fun resolveMethod(): Method {
        val owner = Class.forName("android.net.TetheringManager")
        return owner.getMethod("setPreferTestNetworks", Boolean::class.javaPrimitiveType)
    }

    private fun tetheringManager(): Any =
        context.getSystemService("tethering")
            ?: error("setPreferTestNetworks: tethering service unavailable")

    fun setPreferTestNetworks(prefer: Boolean) {
        resolveMethod().invoke(tetheringManager(), prefer)
    }

    fun resolutionFailure(): String? = runCatching { resolveMethod() }.fold(
        onSuccess = { null },
        onFailure = { failure -> "${failure.javaClass.simpleName}: ${failure.message}" },
    )
}

fun Context.connectivityManager(): ConnectivityManager =
    getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
