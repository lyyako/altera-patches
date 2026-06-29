package app.altera.patches.tiktok.seekbar

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal const val AWEME_CLASS = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;"
private val TARGET_CLASS_STRINGS = setOf("homepage_hot", "FeedRecommendFragment")

private fun isTargetClass(classDef: ClassDef): Boolean =
    TARGET_CLASS_STRINGS.all { target ->
        classDef.methods.any { method ->
            method.implementation?.instructions?.any {
                ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == target
            } == true
        }
    }

internal object VanillaLongFilterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(AWEME_CLASS),
    custom = { method, classDef -> isTargetClass(classDef) && method.implementation!!.instructions.count() > 20 },
)

internal object ShouldShowProgressBarFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(AWEME_CLASS),
    custom = { method, classDef -> isTargetClass(classDef) && method.implementation!!.instructions.count() <= 20 },
)

internal object SetSeekBarShowTypeFingerprint : Fingerprint(
    name = "setSeekBarShowType",
    returnType = "V",
    parameters = listOf("I"),
    strings = listOf("seekbar show type change, change to:"),
)
