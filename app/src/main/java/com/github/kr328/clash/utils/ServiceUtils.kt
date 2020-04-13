package com.github.kr328.clash.utils

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.startForegroundServiceCompat
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.service.ClashService
import com.github.kr328.clash.service.TunService
import com.github.kr328.clash.service.util.sendBroadcastSelf

