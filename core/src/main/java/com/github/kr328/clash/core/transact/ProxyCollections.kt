package com.github.kr328.clash.core.transact

import bridge.ProxyCollection
import bridge.ProxyGroupCollection
import bridge.ProxyGroupItem
import bridge.ProxyItem
import java.util.*

class ProxyCollectionImpl : LinkedList<ProxyItem?>(), ProxyCollection
class ProxyGroupCollectionImpl : LinkedList<ProxyGroupItem?>(), ProxyGroupCollection