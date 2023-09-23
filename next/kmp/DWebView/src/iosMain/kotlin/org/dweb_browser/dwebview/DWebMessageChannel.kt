package org.dweb_browser.dwebview

class DWebMessageChannel internal constructor(
    override val port1: DWebMessagePort, override val port2: DWebMessagePort
) : IMessageChannel {}