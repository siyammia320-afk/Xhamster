package com.example

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast

class ProxyVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "DISCONNECT") {
            stopVpn()
            return START_NOT_STICKY
        } else {
            val server = intent?.getStringExtra("SERVER") ?: "0.0.0.0"
            startVpn(server)
            return START_STICKY
        }
    }

    private fun startVpn(server: String) {
        if (vpnInterface != null) return

        try {
            val builder = Builder()
            // We set a dummy local address for the VPN interface
            builder.addAddress("10.0.0.2", 32)
            // Route all traffic through the VPN
            builder.addRoute("0.0.0.0", 0)
            builder.setSession("ProxyConnect ($server)")
            
            // Establish the VPN interface. This shows the VPN key icon in the status bar.
            vpnInterface = builder.establish()

            // NOTE: For a real proxy tunneling (like SOCKS5 or HTTP), you would need to
            // read raw IP packets from vpnInterface!!.fileDescriptor, translate them,
            // and forward them to the remote proxy server. This typically requires
            // native C/C++ libraries (like tun2socks) or complex Java NIO implementations.
            
            Log.d("ProxyVpnService", "VPN Started and Interface Established")
        } catch (e: Exception) {
            Log.e("ProxyVpnService", "Failed to start VPN", e)
            stopSelf()
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.d("ProxyVpnService", "VPN Interface Closed")
        } catch (e: Exception) {
            Log.e("ProxyVpnService", "Failed to close VPN interface", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
