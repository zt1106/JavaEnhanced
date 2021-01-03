package cc.zengtian.je.feature

import javax.swing.JComponent

interface PluginFeature<Settings> {
    fun getSettingsUI(): JComponent
    fun getSettings(): Settings
}