package app.altera.patches.tiktok.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.altera.patches.tiktok.shared.COMPATIBILITY_TIKTOK
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/altera/extension/tiktok/settings/TikTokActivityHook;"
private const val SETTINGS_REGISTRY_CLASS_DESCRIPTOR =
    "Lapp/altera/extension/tiktok/settings/SettingsRegistry;"

private fun Method.resourceLiterals() = implementation!!.instructions
    .filterIsInstance<WideLiteralInstruction>()
    .map { it.wideLiteral.toInt() }
    .filter { it ushr 24 == 0x7f }

private fun Method.resourceLiteral(type: Int? = null) =
    resourceLiterals().single { type == null || it.resourceType == type }

private val Int.resourceType
    get() = this ushr 16 and 0xff

private val Int.smaliLiteral
    get() = "0x${toUInt().toString(16)}"

private inline fun <reified T : Reference> Method.references(opcode: Opcode) =
    implementation!!.instructions.asSequence()
        .filter { it.opcode == opcode }
        .mapNotNull { (it as? ReferenceInstruction)?.reference as? T }

private fun BytecodePatchContext.findMutableMethod(
    filter: Method.() -> Boolean
): MutableMethod {
    val method = buildList {
        classDefForEach { classDef -> addAll(classDef.methods.filter(filter)) }
    }.single()

    return mutableClassDefBy(method.definingClass).methods.single {
        it.name == method.name &&
            it.parameterTypes == method.parameterTypes &&
            it.returnType == method.returnType
    }
}

internal fun BytecodePatchContext.registerSettings(classDescriptor: String) {
    mutableClassDefBy(SETTINGS_REGISTRY_CLASS_DESCRIPTOR).methods.single {
        it.name == "registerSettings" && it.parameterTypes.isEmpty()
    }.apply {
        addInstruction(
            implementation!!.instructions.indexOfFirst { it.opcode == Opcode.RETURN_VOID },
            "invoke-static {}, $classDescriptor->registerSettings()V"
        )
    }
}

@Suppress("unused")
val settingsPatch = bytecodePatch(
    name = "Settings",
    description = "Adds Altera settings to TikTok.",
    default = true
) {
    dependsOn(
        localizationPatch(
            resourceDirectory = "tiktok/settings",
            includeSettingsIcon = true
        )
    )

    compatibleWith(COMPATIBILITY_TIKTOK)

    extendWith("extensions/extension.mpe")

    execute {
        val stateClass = OpenDebugCellVmDefaultStateFingerprint.method
            .references<TypeReference>(Opcode.NEW_INSTANCE)
            .first()
            .type
        val openDebugField = SupportGroupDefaultStateFingerprint.method
            .references<FieldReference>(Opcode.SGET_OBJECT)
            .first { it.name == "SECTION_HEADER" }
        val supportClass = mutableClassDefBy(openDebugField.definingClass)
        val supportConstructor = supportClass.methods.single {
            it.name == "<init>" &&
                it.parameterTypes.map(CharSequence::toString) == listOf(
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "I",
                    "Z",
                    "I"
                )
        }
        val alteraCategoryField = ImmutableField(
            supportClass.type,
            "ALTERA_SECTION_HEADER",
            supportClass.type,
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value,
            null,
            null,
            null
        ).toMutable()
        supportClass.staticFields.add(alteraCategoryField)
        supportClass.methods.single { it.name == "<clinit>" }.apply {
            addInstructions(
                implementation!!.instructions.indexOfFirst { it.opcode == Opcode.RETURN_VOID },
                """
                    new-instance v0, ${supportClass.type}
                    const-string v1, "ALTERA_SECTION_HEADER"
                    const-string v2, "altera_settings_group"
                    const-string v3, "sectionAltera"
                    const-string v4, "sectionSupport"
                    const/4 v5, 0x0
                    const/4 v6, 0x0
                    const/4 v7, 0x1
                    const/16 v8, 0x10
                    invoke-direct/range {v0 .. v8}, $supportConstructor
                    sput-object v0, $alteraCategoryField
                """
            )
        }

        SettingsComposeRowsFingerprint.method.apply {
            val index = implementation!!.instructions.indexOfLast { it.isSettingsListSortInvoke() }
            val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

            addInstructions(
                index + 2,
                """
                    new-instance v0, Ljava/util/ArrayList;
                    invoke-direct {v0, v$register}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
                    sget-object v1, $alteraCategoryField
                    invoke-virtual/range {p0 .. p0}, ${FragmentRequireContextFingerprint.method}
                    move-result-object v2
                    invoke-static {v1, v2}, $EXTENSION_CLASS_DESCRIPTOR->setSettingsCategory(Ljava/lang/Object;Landroid/content/Context;)V
                    const/4 v2, 0x0
                    invoke-virtual {v0, v2, v1}, Ljava/util/ArrayList;->add(ILjava/lang/Object;)V
                    sget-object v1, ${openDebugField.definingClass}->OPEN_DEBUG:${openDebugField.type}
                    const/4 v2, 0x1
                    invoke-virtual {v0, v2, v1}, Ljava/util/ArrayList;->add(ILjava/lang/Object;)V
                    move-object v$register, v0
                """
            )
        }

        findMutableMethod {
            returnType == "V" &&
                parameterTypes.size == 5 &&
                parameterTypes[0] == openDebugField.type &&
                parameterTypes[1] == "Z" &&
                parameterTypes[2] == "Z" &&
                parameterTypes[4] == "I" &&
                resourceLiterals().count() == 1
        }.apply {
            val resourceIndex = implementation!!.instructions.indexOfFirst {
                (it as? WideLiteralInstruction)?.wideLiteral?.toInt()?.let { literal ->
                    literal ushr 24 == 0x7f
                } == true
            }
            val titleCallIndex = implementation!!.instructions.withIndex()
                .drop(resourceIndex + 1)
                .first {
                    ((it.value as? ReferenceInstruction)?.reference as? MethodReference)?.let { method ->
                        method.returnType == "Ljava/lang/String;" &&
                            method.parameterTypes.firstOrNull() == "I"
                    } == true
                }.index
            val titleRegister = getInstruction<OneRegisterInstruction>(titleCallIndex + 1).registerA

            addInstructionsWithLabels(
                resourceIndex,
                """
                    invoke-static/range {p0 .. p0}, $EXTENSION_CLASS_DESCRIPTOR->getSettingsCategoryTitle(Ljava/lang/Object;)Ljava/lang/String;
                    move-result-object v$titleRegister
                    if-nez v$titleRegister, :altera_category_title
                """,
                ExternalLabel(
                    "altera_category_title",
                    getInstruction(titleCallIndex + 2)
                )
            )

            addInstructionsWithLabels(
                0,
                """
                    sget-object v0, ${openDebugField.definingClass}->OPEN_DEBUG:${openDebugField.type}
                    if-ne p0, v0, :keep_original_top_rounding
                    const/16 p1, 0x1
                """,
                ExternalLabel("keep_original_top_rounding", getInstruction(0))
            )
        }

        findMutableMethod {
            returnType == "V" &&
                parameterTypes.size == 5 &&
                parameterTypes[0] == stateClass &&
                parameterTypes[1] == "Z" &&
                implementation?.instructions?.any {
                    it.opcode == Opcode.INVOKE_VIRTUAL &&
                        ((it as? ReferenceInstruction)?.reference as? MethodReference)?.toString() ==
                        "Landroid/content/Context;->getString(I)Ljava/lang/String;"
                } == true
        }.apply {
            val index = implementation!!.instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_VIRTUAL &&
                    ((it as? ReferenceInstruction)?.reference as? MethodReference)?.toString() ==
                    "Landroid/content/Context;->getString(I)Ljava/lang/String;"
            }
            val titleRegister = getInstruction<OneRegisterInstruction>(index + 1).registerA
            addInstructions(
                index + 2,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getSettingsTitle()Ljava/lang/String;
                    move-result-object v$titleRegister
                """
            )
        }

        mutableClassDefBy(stateClass).apply {
            val constructor = methods.first { it.name == "<init>" }
            val iconType = constructor.references<FieldReference>(Opcode.SGET_OBJECT).first().type
            val iconFields = fields.filter { it.type == iconType }
            val iconConstructor = classDefBy(iconType).methods.single {
                it.name == "<init>" &&
                    it.parameterTypes.map(CharSequence::toString) == listOf("I")
            }
            constructor.apply {
                addInstructions(
                    implementation!!.instructions.indexOfFirst { it.opcode == Opcode.RETURN_VOID },
                    """
                        const v0, ${settingsIconResourceId.smaliLiteral}
                        new-instance p1, $iconType
                        invoke-direct {p1, v0}, $iconConstructor
                        iput-object p1, p0, $stateClass->${iconFields[0].name}:${iconFields[0].type}
                        iput-object p1, p0, $stateClass->${iconFields[1].name}:${iconFields[1].type}
                    """
                )
            }
        }

        val pageLayoutId = ThemeSettingPageLayoutFingerprint.method.resourceLiteral()
        val layoutResourceType = pageLayoutId.resourceType
        val categoryLayoutId =
            DividerCellLayoutFingerprint.method.resourceLiteral(layoutResourceType)
        val switchCellResources = SwitchCellLayoutFingerprint.method.resourceLiterals()
        val switchLayoutId = switchCellResources.single { it.resourceType == layoutResourceType }
        val rightIconLayoutId =
            RightIconCellLayoutFingerprint.method.resourceLiteral(layoutResourceType)
        val cardBackgroundAttributeId =
            switchCellResources.single { it.resourceType != layoutResourceType }
        val pageBackgroundAttributeId = ThemeSettingPageSetupFingerprint.method
            .resourceLiteral(cardBackgroundAttributeId.resourceType)

        val navigationMethodReferences = ThemeSettingPageSetupFingerprint.method
            .references<MethodReference>(Opcode.INVOKE_VIRTUAL)
            .filter { it.definingClass == "Lcom/bytedance/tux/navigation/TuxNavBar;" }
            .toList()
        val setNavigationActionsMethod = navigationMethodReferences.single {
            it.name == "setNavActions" && it.parameterTypes.size == 1
        }
        val navigationActionsType = setNavigationActionsMethod.parameterTypes.single().toString()
        val setNavigationBackgroundMethod = navigationMethodReferences.single {
            it.name == "setNavBackground" &&
                it.parameterTypes.map(CharSequence::toString) == listOf("I")
        }
        val setNavigationSeparatorVisibilityMethod = navigationMethodReferences.single {
            it.returnType == "V" &&
                it.parameterTypes.map(CharSequence::toString) == listOf("Z")
        }
        val navigationBuilderMethod = ThemeSettingPageSetupFingerprint.method
            .references<MethodReference>(Opcode.INVOKE_STATIC)
            .single {
                it.parameterTypes.size == 3 &&
                    it.parameterTypes[0].toString() == navigationActionsType &&
                    it.parameterTypes[1].toString() == "Ljava/lang/String;" &&
                    it.parameterTypes[2].toString() == "Lkotlin/jvm/functions/Function0;"
            }
        val nativeNavigationBuilderMethod =
            classDefBy(navigationBuilderMethod.definingClass).methods.single {
                it.returnType == "V" &&
                    it.parameterTypes.map(CharSequence::toString) == listOf(
                        navigationActionsType,
                        "Ljava/lang/String;",
                        "Landroid/app/Activity;"
                    )
            }
        val navigationActionsConstructor = classDefBy(navigationActionsType).methods.single {
            it.name == "<init>" && it.parameterTypes.isEmpty()
        }

        val configureNavigationMethod = mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR).methods.single {
            it.name == "configureNavigation" &&
                it.parameterTypes.map(CharSequence::toString) == listOf(
                    "Landroid/app/Activity;",
                    "Landroid/view/View;",
                    "I"
                )
        }
        configureNavigationMethod.removeInstructions(
            0,
            configureNavigationMethod.implementation!!.instructions.size
        )
        configureNavigationMethod.addInstructions(
            0,
            """
                check-cast p1, ${setNavigationActionsMethod.definingClass}
                new-instance v0, $navigationActionsType
                invoke-direct {v0}, $navigationActionsConstructor
                invoke-virtual {p1, p2}, $setNavigationBackgroundMethod
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getSettingsTitle()Ljava/lang/String;
                move-result-object p2
                invoke-static {v0, p2, p0}, $nativeNavigationBuilderMethod
                invoke-virtual {p1, v0}, $setNavigationActionsMethod
                const/4 p2, 0x0
                invoke-virtual {p1, p2}, $setNavigationSeparatorVisibilityMethod
                return-void
            """
        )

        AdPersonalizationActivityOnCreateFingerprint.method.apply {
            val index =
                implementation!!.instructions.indexOfFirst { it.opcode == Opcode.INVOKE_SUPER } + 1
            addInstructionsWithLabels(
                index,
                """
                    const v0, ${pageLayoutId.smaliLiteral}
                    const v1, ${categoryLayoutId.smaliLiteral}
                    const v2, ${switchLayoutId.smaliLiteral}
                    const v3, ${rightIconLayoutId.smaliLiteral}
                    const v4, ${pageBackgroundAttributeId.smaliLiteral}
                    const v5, ${cardBackgroundAttributeId.smaliLiteral}
                    invoke-static/range {v0 .. v5}, $EXTENSION_CLASS_DESCRIPTOR->setResourceIds(IIIIII)V
                    invoke-static/range {p0 .. p0}, $EXTENSION_CLASS_DESCRIPTOR->initialize(Landroid/app/Activity;)Z
                    move-result v0
                    if-eqz v0, :ignore_altera_settings
                    return-void
                """,
                ExternalLabel("ignore_altera_settings", getInstruction(index))
            )
        }

        AdPersonalizationActivityOnResumeFingerprint.method.apply {
            val index =
                implementation!!.instructions.indexOfFirst { it.opcode == Opcode.INVOKE_SUPER } + 1
            addInstructionsWithLabels(
                index,
                """
                    invoke-static/range {p0 .. p0}, $EXTENSION_CLASS_DESCRIPTOR->isSettingsActivity(Landroid/app/Activity;)Z
                    move-result v0
                    if-eqz v0, :resume_original_activity
                    return-void
                """,
                ExternalLabel("resume_original_activity", getInstruction(index))
            )
        }

        val clickMethod = findMutableMethod {
            returnType == "Ljava/lang/Object;" &&
                implementation?.instructions?.any {
                    it.opcode == Opcode.IGET_OBJECT &&
                        (((it as? ReferenceInstruction)?.reference as? FieldReference)?.let { field ->
                            field.definingClass == stateClass &&
                                field.type == "Lkotlin/jvm/functions/Function1;"
                        } == true)
                } == true
        }
        val contextField = clickMethod.implementation!!.instructions.withIndex().firstNotNullOf {
            (((it.value as? ReferenceInstruction)?.reference as? FieldReference)
                ?.takeIf { field ->
                    it.value.opcode == Opcode.IGET_OBJECT &&
                        field.definingClass == clickMethod.definingClass &&
                        clickMethod.implementation!!.instructions.drop(it.index + 1).take(2).any {
                            it.opcode == Opcode.CHECK_CAST &&
                                ((it as? ReferenceInstruction)?.reference as? TypeReference)?.type ==
                                "Landroid/content/Context;"
                        }
                })
        }

        val unitField = clickMethod.implementation!!.instructions.firstNotNullOf {
            ((it as? ReferenceInstruction)?.reference as? FieldReference)
                ?.takeIf { field -> it.opcode == Opcode.SGET_OBJECT && field.type == "Lkotlin/Unit;" }
        }

        clickMethod.addInstructions(
            0,
            """
                iget-object v0, p0, ${clickMethod.definingClass}->${contextField.name}:${contextField.type}
                check-cast v0, Landroid/content/Context;
                invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->startSettingsActivity(Landroid/content/Context;)V
                sget-object v0, ${unitField.definingClass}->${unitField.name}:${unitField.type}
                return-object v0
            """
        )
    }
}
