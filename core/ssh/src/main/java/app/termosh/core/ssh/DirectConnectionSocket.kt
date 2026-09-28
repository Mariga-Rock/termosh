package app.termosh.core.ssh

import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

internal class DirectConnectionSocket(
    private val inputProvider: () -> InputStream,
    private val outputProvider: () -> OutputStream,
    private val closeAction: () -> Unit,
) : Socket() {

    override fun getInputStream(): InputStream = inputProvider()
    override fun getOutputStream(): OutputStream = outputProvider()

    override fun close() {
        try { super.close() } catch (_: Throwable) {}
        try { closeAction() } catch (_: Throwable) {}
    }

    override fun isConnected(): Boolean = true
    override fun isClosed(): Boolean = false
    override fun isBound(): Boolean = true
    override fun isInputShutdown(): Boolean = false
    override fun isOutputShutdown(): Boolean = false

    override fun connect(endpoint: SocketAddress?) {}
    override fun connect(endpoint: SocketAddress?, timeout: Int) {}

    override fun setSoTimeout(timeout: Int) {}
    override fun getSoTimeout(): Int = 0

    override fun setTcpNoDelay(on: Boolean) {}
    override fun getTcpNoDelay(): Boolean = true

    override fun setKeepAlive(on: Boolean) {}
    override fun getKeepAlive(): Boolean = false

    override fun setReuseAddress(on: Boolean) {}
    override fun getReuseAddress(): Boolean = false

    override fun setOOBInline(on: Boolean) {}
    override fun getOOBInline(): Boolean = false

    override fun setSendBufferSize(size: Int) {}
    override fun getSendBufferSize(): Int = 0

    override fun setReceiveBufferSize(size: Int) {}
    override fun getReceiveBufferSize(): Int = 0

    override fun setSoLinger(on: Boolean, linger: Int) {}
    override fun getSoLinger(): Int = -1

    override fun setTrafficClass(tc: Int) {}
    override fun getTrafficClass(): Int = 0

    override fun shutdownInput() {}
    override fun shutdownOutput() {}

    override fun getLocalAddress(): InetAddress = InetAddress.getLoopbackAddress()
    override fun getLocalPort(): Int = 0
    override fun getLocalSocketAddress(): SocketAddress =
        InetSocketAddress(InetAddress.getLoopbackAddress(), 0)
    override fun getInetAddress(): InetAddress = InetAddress.getLoopbackAddress()
    override fun getPort(): Int = 0
    override fun getRemoteSocketAddress(): SocketAddress? = null
}
