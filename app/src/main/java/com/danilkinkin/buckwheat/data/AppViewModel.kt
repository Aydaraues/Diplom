package com.danilkinkin.buckwheat.data

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.danilkinkin.buckwheat.data.entities.Storage
import com.danilkinkin.buckwheat.di.DatabaseRepository
import com.danilkinkin.buckwheat.effects.ConfettiController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class SystemBarState (
    val statusBarColor: Color,
    val statusBarDarkIcons: Boolean,
    val navigationBarDarkIcons: Boolean,
    val navigationBarColor: Color,
)

data class PathState (
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    val callback: (result: Map<String, Any?>) -> Unit = {},
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val db: DatabaseRepository,
) : ViewModel() {
    private val storage = db.storageDao()

    var snackbarHostState = SnackbarHostState()
        private set

    var confettiController = ConfettiController()
        private set

    var lockSwipeable: MutableState<Boolean> = mutableStateOf(false)

    var showSystemKeyboard: MutableState<Boolean> = mutableStateOf(false)

    var statusBarStack: MutableList<() -> SystemBarState> = emptyList<() -> SystemBarState>().toMutableList()

    var sheetStates: MutableLiveData<Map<String, PathState>> = MutableLiveData(emptyMap())

    var isDebug: MutableLiveData<Boolean> = MutableLiveData(try {
        storage.get("isDebug").value.toBoolean()
    } catch (e: Exception) {
        false
    })

    var showRestBudgetCardByDefault: MutableLiveData<Boolean> = MutableLiveData(try {
        storage.get("showRestBudgetCardByDefault").value.toBoolean()
    } catch (e: Exception) {
        true
    })

    fun setShowRestBudgetCardByDefault(showByDefault: Boolean) {
        storage.set(Storage("showRestBudgetCardByDefault", showByDefault.toString()))

        showRestBudgetCardByDefault.value = showByDefault
    }

    fun setIsDebug(debug: Boolean) {
        storage.set(Storage("isDebug", debug.toString()))

        isDebug.value = debug
    }

    fun openSheet(state: PathState) {
        sheetStates.value = sheetStates.value!!.plus(Pair(state.name, state))
    }

    fun closeSheet(name: String) {
        sheetStates.value = sheetStates.value!!.minus(name)
    }

    fun getBooleanValue(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            val value = storage.get(key).value.toBoolean()
            value
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun setBooleanValue(key: String, value: Boolean) {
        storage.set(Storage(key, value.toString()))
    }
}