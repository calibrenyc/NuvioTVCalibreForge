package com.nuvio.tv.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionPolicyTest {
    @Test
    fun `higher version code is an update`() {
        assertTrue(UpdateVersionPolicy.isRemoteVersionCodeNewer(1079, 1078))
    }

    @Test
    fun `equal and lower version codes are not updates`() {
        assertFalse(UpdateVersionPolicy.isRemoteVersionCodeNewer(1078, 1078))
        assertFalse(UpdateVersionPolicy.isRemoteVersionCodeNewer(1077, 1078))
    }
}
