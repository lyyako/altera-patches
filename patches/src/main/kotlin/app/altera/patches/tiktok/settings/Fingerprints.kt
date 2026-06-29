package app.altera.patches.tiktok.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val AD_PERSONALIZATION_ACTIVITY =
    "Lcom/bytedance/ies/ugc/aweme/commercialize/compliance/personalization/AdPersonalizationActivity;"
private const val THEME_SETTING_PAGE =
    "Lcom/ss/android/ugc/aweme/setting/page/theme/ThemeSettingPage;"

internal fun Instruction.isSettingsListSortInvoke(): Boolean {
    if (opcode != Opcode.INVOKE_STATIC) return false

    val reference = (this as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return reference.returnType == "Ljava/util/List;" &&
        reference.parameterTypes.size == 2 &&
        reference.parameterTypes[0] == "Ljava/util/Comparator;" &&
        reference.parameterTypes[1] == "Ljava/lang/Iterable;"
}

private fun activityLifecycleFingerprint(
    name: String,
    parameters: List<String> = emptyList()
) = Fingerprint(
    definingClass = AD_PERSONALIZATION_ACTIVITY,
    name = name,
    returnType = "V",
    parameters = parameters,
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_SUPER,
            name = name
        )
    )
)

internal val AdPersonalizationActivityOnCreateFingerprint = activityLifecycleFingerprint(
    "onCreate",
    listOf("Landroid/os/Bundle;")
)
internal val AdPersonalizationActivityOnResumeFingerprint =
    activityLifecycleFingerprint("onResume")

internal object SupportGroupDefaultStateFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/group/support/SupportGroupVM;",
    name = "defaultState"
)

internal object OpenDebugCellVmDefaultStateFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/group/support/cells/OpenDebugCellVM;",
    name = "defaultState"
)

internal object SettingsComposeRowsFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/SettingsComposeRvmpFragment;",
    returnType = "V",
    custom = { method, _ ->
        method.parameterTypes.lastOrNull() == "I" &&
            method.parameterTypes.contains("Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/SettingsRvmpComposeViewModel;") &&
            method.parameterTypes.contains("Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/group/support/SupportGroupVM;") &&
            method.implementation?.instructions?.any { it.isSettingsListSortInvoke() } == true
    }
)

internal object ThemeSettingPageLayoutFingerprint : Fingerprint(
    definingClass = THEME_SETTING_PAGE,
    returnType = "I",
    parameters = emptyList(),
)

internal object ThemeSettingPageSetupFingerprint : Fingerprint(
    definingClass = THEME_SETTING_PAGE,
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = "Lcom/bytedance/tux/navigation/TuxNavBar;",
            name = "setNavBackground"
        )
    )
)

internal object FragmentRequireContextFingerprint : Fingerprint(
    definingClass = "Landroidx/fragment/app/Fragment;",
    name = "requireContext",
    returnType = "Landroid/content/Context;",
    parameters = emptyList(),
)

private fun cellLayoutFingerprint(
    definingClass: String,
    setsBackgroundColor: Boolean = false
) = Fingerprint(
    custom = { method, classDef ->
        classDef.type == definingClass &&
            method.returnType == "Landroid/view/View;" &&
            method.parameterTypes == listOf("Landroid/view/ViewGroup;") &&
            method.implementation?.let { implementation ->
                !setsBackgroundColor || implementation.instructions.any {
                    it.opcode == Opcode.INVOKE_VIRTUAL &&
                        ((it as? ReferenceInstruction)?.reference as? MethodReference)?.toString() ==
                        "Landroid/view/View;->setBackgroundColor(I)V"
                }
            } == true
    }
)

internal val DividerCellLayoutFingerprint =
    cellLayoutFingerprint("Lcom/ss/android/ugc/aweme/cell/DividerCell;")
internal val SwitchCellLayoutFingerprint = cellLayoutFingerprint(
    "Lcom/ss/android/ugc/aweme/setting/page/base/SwitchCell;",
    setsBackgroundColor = true
)
internal val RightIconCellLayoutFingerprint = cellLayoutFingerprint(
    "Lcom/ss/android/ugc/aweme/setting/page/base/RightIconCell;",
    setsBackgroundColor = true
)
