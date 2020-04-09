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
import com.ciscowebex.androidsdk.utils.Json
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody

enum class Service {

    Hydra, Region, Wdm, Kms, Locus, Conv, Metrics, CalliopeDiscorey, Common;

    fun get(vararg paths: String?): ServiceReqeust {
        return ServiceReqeust(this, Request.Builder().get()).to(*paths)
    }

    fun delete(vararg paths: String?): ServiceReqeust {
        return ServiceReqeust(this, Request.Builder().delete()).to(*paths)
    }

    @JvmOverloads
    fun post(o: Any? = null): ServiceReqeust {
        return ServiceReqeust(this, Request.Builder().post(toBody(o)))
    }

    @JvmOverloads
    fun put(o: Any? = null): ServiceReqeust {
        return ServiceReqeust(this, Request.Builder().put(toBody(o)))
    }

    @JvmOverloads
    fun patch(o: Any? = null): ServiceReqeust {
        return ServiceReqeust(this, Request.Builder().patch(toBody(o)))
    }

    private fun toBody(o: Any?): RequestBody {
        return when (o) {
            null -> RequestBody.create(null, byteArrayOf())
            is RequestBody -> o
            else -> RequestBody.create(MediaType.get("application/json; charset=utf-8"), Json.get().toJson(o))
        }
    }

    fun endpoint(device: Device?): String {
        return when (this) {
            Region -> "https://ds.ciscospark.com/v1"
            Wdm -> if (BuildConfig.INTEGRATION_TEST) "https://wdm-intb.ciscospark.com/wdm/api/v1" else "https://wdm-a.wbx2.com/wdm/api/v1"
            Hydra -> if (BuildConfig.INTEGRATION_TEST) "https://apialpha.ciscospark.com/v1" else "https://api.ciscospark.com/v1"
            Locus -> dynamicEndpoint(device, "https://locus-a.wbx2.com/locus/api/v1")
            Metrics -> dynamicEndpoint(device, "https://metrics-a.wbx2.com/metrics/api/v1")
            CalliopeDiscorey -> dynamicEndpoint(device, "https://calliope-a.wbx2.com/calliope/api/discovery/v1")
            Kms -> {
                val defaultValue = if (BuildConfig.INTEGRATION_TEST) "https://encryption-intb.ciscospark.com/encryption/api/v1" else "https://encryption-a.wbx2.com/encryption/api/v1"
                dynamicEndpoint(device, defaultValue)
            }
            Conv -> {
                val defaultValue = if (BuildConfig.INTEGRATION_TEST) "https://conversation-intb.ciscospark.com/conversation/api/v1" else "https://conv-a.wbx2.com/conversation/api/v1"
                dynamicEndpoint(device, defaultValue)
            }
            else -> ""
        }
    }

    private fun dynamicEndpoint(device: Device?, defaultValue: String): String {
        return device?.getServiceUrl(key()) ?: defaultValue
    }

    private fun key(): String {
        return when (this) {
            Kms -> "encryptionServiceUrl"
            Conv -> "conversationServiceUrl"
            CalliopeDiscorey -> "calliopeDiscoveryServiceUrl"
            else -> name + "ServiceUrl"
        }
    }
}
