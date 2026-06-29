package app.altera.patches.tiktok.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal val COMPATIBILITY_TIKTOK = Compatibility(
    name = "TikTok",
    packageName = "com.zhiliaoapp.musically",
    apkFileType = ApkFileType.APK,
    appIconColor = 0xFF0050,
    targets = listOf(
        AppTarget(
            version = "45.7.3"
        )
    )
)
