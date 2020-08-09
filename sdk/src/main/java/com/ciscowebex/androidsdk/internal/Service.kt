/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal

import com.ciscowebex.androidsdk.BuildConfig
import java.util.*

enum class Service {

    Hydra, Region, U2C, Wdm, Kms, Locus, Conv, Metrics, CalliopeDiscorey;

    fun homed(device: Device? = null): ServiceReqeust {
        return ServiceReqeust(this.baseUrl(device))
    }

    fun global(): ServiceReqeust {
        return ServiceReqeust(this.baseUrl())
    }

    fun baseUrl(device: Device? = null): String {
        return when (this) {
            Region -> "https://ds.ciscospark.com/v1"
            U2C -> if (BuildConfig.INTEGRATION_TEST) "https://u2c-intb.ciscospark.com/u2c/api/v1" else "https://u2c.wbx2.com/u2c/api/v1"
            Wdm -> if (BuildConfig.INTEGRATION_TEST) "https://wdm-intb.ciscospark.com/wdm/api/v1" else "https://wdm-a.wbx2.com/wdm/api/v1"
            Hydra -> if (BuildConfig.INTEGRATION_TEST) "https://apialpha.ciscospark.com/v1" else "https://api.ciscospark.com/v1"
            Locus -> baseUrl(device, "https://locus-a.wbx2.com/locus/api/v1")
            Metrics -> baseUrl(device, "https://metrics-a.wbx2.com/metrics/api/v1")
            CalliopeDiscorey -> baseUrl(device, "https://calliope-a.wbx2.com/calliope/api/discovery/v1")
            Kms -> {
                val defaultValue = if (BuildConfig.INTEGRATION_TEST) "https://encryption-intb.ciscospark.com/encryption/api/v1" else "https://encryption-a.wbx2.com/encryption/api/v1"
                baseUrl(device, defaultValue)
            }
            Conv -> {
                val defaultValue = if (BuildConfig.INTEGRATION_TEST) "https://conversation-intb.ciscospark.com/conversation/api/v1" else "https://conv-a.wbx2.com/conversation/api/v1"
                baseUrl(device, defaultValue)
            }
        }
    }

    private fun baseUrl(device: Device?, defaultValue: String): String {
        return device?.getServiceUrl(key()) ?: defaultValue
    }

    private fun key(): String {
        return when (this) {
            Kms -> "encryption"
            Conv -> "conversation"
            CalliopeDiscorey -> "calliopeDiscovery"
            else -> name.toLowerCase(Locale.US)
        }
    }
}
