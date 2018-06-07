// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex

import com.intellij.codeInspection.InspectionEP
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.scope.packageSet.NamedScope
import com.intellij.util.Consumer
import gnu.trove.THashSet

open class InspectionProfileModifiableModel(val source: InspectionProfileImpl) : InspectionProfileImpl(source.name, source.myToolSupplier, source.profileManager, null) {
  private var modified = false

  init {
    myUninitializedSettings.putAll(source.myUninitializedSettings)
    isProjectLevel = source.isProjectLevel
    myLockedProfile = source.myLockedProfile
    copyFrom(source)
  }

  fun isChanged(): Boolean = modified || source.myLockedProfile != myLockedProfile

  fun setModified(value: Boolean) {
    modified = value
  }

  override fun copyToolsConfigurations(project: Project?) {
    copyToolsConfigurations(source, project)
  }

  override fun createTools(project: Project?): List<InspectionToolWrapper<InspectionProfileEntry, InspectionEP>> = source.getDefaultStates(project).map { it.tool }

  private fun copyToolsConfigurations(profile: InspectionProfileImpl, project: Project?) {
    try {
      for (toolList in profile.myTools.values) {
        val tools = myTools[toolList.shortName]!!
        val defaultState = toolList.defaultState
        tools.setDefaultState(copyToolSettings(defaultState.tool), defaultState.isEnabled, defaultState.level)
        tools.removeAllScopes()
        val nonDefaultToolStates = toolList.nonDefaultTools
        if (nonDefaultToolStates != null) {
          for (state in nonDefaultToolStates) {
            val toolWrapper = copyToolSettings(state.tool)
            val scope = state.getScope(project)
            if (scope == null) {
              tools.addTool(state.scopeName, toolWrapper, state.isEnabled, state.level)
            }
            else {
              tools.addTool(scope, toolWrapper, state.isEnabled, state.level)
            }
          }
        }
        tools.isEnabled = toolList.isEnabled
      }
    }
    catch (e: WriteExternalException) {
      LOG.error(e)
    }
    catch (e: InvalidDataException) {
      LOG.error(e)
    }
  }

  override fun getChangedToolNames(): MutableSet<String> {
    if (myChangedToolNames == null) {
      synchronized(myLock) {
        if (myChangedToolNames == null) {
          initInspectionTools(null)
          val names = myTools.keys
          val map = THashSet<String>(names.size)
          for (toolId in names) {
            if (!toolSettingsAreEqual(toolId, source, this)) {
              map.add(toolId)
            }
          }
          myChangedToolNames = map
          return map
        }
      }
    }
    return myChangedToolNames!!
  }


  private fun toolSettingsAreEqual(toolName: String, profile1: InspectionProfileImpl, profile2: InspectionProfileImpl): Boolean {
    val toolList1 = profile1.myTools[toolName]
    val toolList2 = profile2.myTools[toolName]
    return Comparing.equal<Tools>(toolList1, toolList2)
  }

  fun isProperSetting(toolId: String): Boolean {
    val tools = source.getToolsOrNull(toolId, null)
    val currentTools = myTools.get(toolId)
    return tools != currentTools
  }

  fun isProperSetting(toolId: String, scope: NamedScope, project: Project): Boolean {
    val baseDefaultWrapper = source.getToolsOrNull(toolId, null)?.defaultState?.tool
    val actualWrapper = myTools[toolId]?.tools?.first { s -> scope == s.getScope(project) }?.tool
    return baseDefaultWrapper != null && actualWrapper != null && ScopeToolState.areSettingsEqual(baseDefaultWrapper, actualWrapper)
  }

  fun resetToBase(project: Project?) {
    initInspectionTools(project)

    copyToolsConfigurations(source, project)
    myChangedToolNames = null
    myUninitializedSettings.clear()
  }

  fun resetToBase(toolId: String, scope: NamedScope, project: Project?) {
    val baseDefaultWrapper = source.getToolsOrNull(toolId, null)?.defaultState?.tool!!
    val state = myTools[toolId]?.tools?.first { s -> scope == s.getScope(project) }!!
    state.tool = copyToolSettings(baseDefaultWrapper)
  }

  //invoke when isChanged() == true
  fun commit() {
    source.commit(this)
    modified = false
  }

  fun resetToEmpty(project: Project) {
    initInspectionTools(project)
    for (toolWrapper in getInspectionTools(null)) {
      setToolEnabled(toolWrapper.shortName, false, project, fireEvents = false)
    }
  }

  private fun InspectionProfileImpl.commit(model: InspectionProfileImpl) {
    name = model.name
    description = model.description
    isProjectLevel = model.isProjectLevel
    myLockedProfile = model.myLockedProfile
    myChangedToolNames = model.myChangedToolNames
    myTools = model.myTools
    profileManager = model.profileManager
  }

  fun disableTool(toolShortName: String, element: PsiElement) {
    getTools(toolShortName, element.project).disableTool(element)
  }

  override fun profileChanged() {
    super.profileChanged()
    myChangedToolNames = null
  }

  override fun toString(): String = "$name (copy)"
}

fun modifyAndCommitProjectProfile(project: Project, action: Consumer<InspectionProfileModifiableModel>) {
  ProjectInspectionProfileManager.getInstance(project).currentProfile.edit { action.consume(this) }
}

inline fun InspectionProfileImpl.edit(task: InspectionProfileModifiableModel.() -> Unit) {
  val model = InspectionProfileModifiableModel(this)
  model.task()
  model.commit()
  profileManager.fireProfileChanged(this)
}
