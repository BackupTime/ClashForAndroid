package com.github.kr328.clash.pipeline

import com.github.kr328.clash.service.settings.BaseSettings

data class Pipeline<T>(val input: T, val settings: BaseSettings)