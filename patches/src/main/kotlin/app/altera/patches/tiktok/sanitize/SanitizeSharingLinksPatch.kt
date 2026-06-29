package app.altera.patches.tiktok.sanitize

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.altera.patches.tiktok.settings.localizationPatch
import app.altera.patches.tiktok.settings.registerSettings
import app.altera.patches.tiktok.settings.settingsPatch
import app.altera.patches.tiktok.shared.COMPATIBILITY_TIKTOK

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/altera/extension/tiktok/sanitize/ShareUrlSanitizer;"

@Suppress("unused")
val sanitizeSharingLinksPatch = bytecodePatch(
    name = "Sanitize sharing links",
    description = "Removes tracking parameters from shared links.",
    default = true,
) {
    dependsOn(settingsPatch, localizationPatch("tiktok/sanitize"))

    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        registerSettings(EXTENSION_CLASS_DESCRIPTOR)
        ShareUrlTrackerFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->stripAllQueryParams(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
                return-object p1
            """,
        )
    }
}
