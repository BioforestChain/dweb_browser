package info.bagen.libappmgr.schedule

interface UpdateTask {
    fun scheduleUpdate(interval: Long)
    fun cancle()
}
