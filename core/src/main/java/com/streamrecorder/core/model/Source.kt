package com.streamrecorder.core.model

data class Source(
    val resolution: Int,
    val filesize: Long,
    val downloadlink: String,
    val deletiontime: Long,
    val recordedfileid: Long,
) {
    val filesizeMiB: Double get() = filesize / 1024.0 / 1024.0

    val filesizeFormatted: String get() {
        val mib = filesizeMiB
        return if (mib >= 1024) "%.2f GiB".format(mib / 1024)
        else "%.1f MiB".format(mib)
    }
}
