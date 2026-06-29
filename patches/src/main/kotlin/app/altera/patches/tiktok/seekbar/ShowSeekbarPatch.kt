package app.altera.patches.tiktok.seekbar

import app.altera.patches.tiktok.settings.localizationPatch
import app.altera.patches.tiktok.settings.registerSettings
import app.altera.patches.tiktok.settings.settingsPatch
import app.altera.patches.tiktok.shared.COMPATIBILITY_TIKTOK
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/altera/extension/tiktok/seekbar/SeekbarPatch;"

@Suppress("unused")
val showSeekbarPatch = bytecodePatch(
    name = "Show seekbar",
    description = "Shows the native seekbar on videos where TikTok would normally hide it.",
    default = true,
) {
    dependsOn(settingsPatch, localizationPatch("tiktok/seekbar"))

    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        registerSettings(EXTENSION_CLASS_DESCRIPTOR)

        val targetAweme = VanillaLongFilterFingerprint.method

        ShouldShowProgressBarFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isEnabled()Z
                move-result v0
                if-eqz v0, :show_seekbar_original
                if-eqz p0, :show_seekbar_not_video
                invoke-static {p0}, ${targetAweme.definingClass}->${targetAweme.name}($AWEME_CLASS)Z
                move-result v0
                return v0
                :show_seekbar_not_video
                const/4 v0, 0x0
                return v0
            """,
            ExternalLabel("show_seekbar_original", ShouldShowProgressBarFingerprint.method.getInstruction(0))
        )

        SetSeekBarShowTypeFingerprint.method.apply {
            val typeRegister = implementation!!.registerCount - 1
            addInstructions(
                0,
                """
                    invoke-static {v$typeRegister}, $EXTENSION_CLASS_DESCRIPTOR->overrideSeekbarShowType(I)I
                    move-result v$typeRegister
                """,
            )
        }
    }
}
