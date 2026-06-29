package app.altera.patches.tiktok.settings

import app.morphe.patcher.patch.resourcePatch
import app.altera.patches.tiktok.shared.COMPATIBILITY_TIKTOK
import org.w3c.dom.Element
import org.w3c.dom.NodeList

private object SettingsLocalizationFiles

private const val SETTINGS_ICON_RESOURCE_NAME = "altera_settings_icon"

internal var settingsIconResourceId = -1

private fun NodeList.elements() = (0 until length).mapNotNull { item(it) as? Element }

private val Element.resourceId
    get() = getAttribute("id").removePrefix("0x").toLong(16).toInt()

internal fun localizationPatch(
    resourceDirectory: String,
    includeSettingsIcon: Boolean = false
) = resourcePatch {
    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        if (includeSettingsIcon) {
            SettingsLocalizationFiles::class.java
                .getResourceAsStream("/$resourceDirectory/$SETTINGS_ICON_RESOURCE_NAME.png")!!
                .use { input ->
                    get("res/raw").apply { mkdirs() }
                        .resolve("$SETTINGS_ICON_RESOURCE_NAME.png")
                        .outputStream()
                        .use { output -> input.copyTo(output) }
                }

            document("res/values/public.xml").use { targetDocument ->
                val targetResources = targetDocument.documentElement
                val rawResources = targetResources.childNodes.elements()
                    .filter { it.getAttribute("type") == "raw" }

                settingsIconResourceId = rawResources
                    .firstOrNull { it.getAttribute("name") == SETTINGS_ICON_RESOURCE_NAME }
                    ?.resourceId
                    ?: (rawResources.maxOf { it.resourceId } + 1).also { resourceId ->
                        targetResources.appendChild(
                            targetDocument.createElement("public").apply {
                                setAttribute("type", "raw")
                                setAttribute("name", SETTINGS_ICON_RESOURCE_NAME)
                                setAttribute("id", "0x${resourceId.toUInt().toString(16)}")
                            }
                        )
                    }
            }
        }

        mapOf(
            "en" to "values",
            "ru" to "values-ru",
        ).forEach { (language, valuesDirectory) ->
            SettingsLocalizationFiles::class.java
                .getResourceAsStream("/$resourceDirectory/strings-$language.xml")!!
                .use { input ->
                    document("res/$valuesDirectory/strings.xml").use { targetDocument ->
                        document(input).use { sourceDocument ->
                            val targetResources = targetDocument.documentElement
                            sourceDocument.documentElement.childNodes.elements().forEach {
                                targetResources.appendChild(
                                    targetDocument.importNode(it, true)
                                )
                            }
                        }
                    }
                }
        }
    }
}
