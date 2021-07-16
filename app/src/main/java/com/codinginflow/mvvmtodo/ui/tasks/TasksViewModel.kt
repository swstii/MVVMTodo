package com.codinginflow.mvvmtodo.ui.tasks

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.codinginflow.mvvmtodo.data.PrefrencesManager
import com.codinginflow.mvvmtodo.data.SortOrder
import com.codinginflow.mvvmtodo.data.Task
import com.codinginflow.mvvmtodo.data.TaskDao
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TasksViewModel @ViewModelInject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager : PrefrencesManager,
    @Assisted private val state : SavedStateHandle
) : ViewModel(){

    val searchQuery  = state.getLiveData("searchQuery", "")

    val preferencesFlow = preferencesManager.prefrencesFlow

    private val tasksEventChannel = Channel<TasksEvent>()

    val tasksEvent = tasksEventChannel.receiveAsFlow()

    private val taskFlow = combine(
        searchQuery.asFlow(),
        preferencesFlow
    ){query,filterPreferences ->
        Pair(query,filterPreferences)

    }.flatMapLatest {(query,filterPreferences) ->
        taskDao.getTasks(query,filterPreferences.sortOrder,filterPreferences.hideCompleted)
    }

    val tasks : LiveData<List<Task>> = taskFlow.asLiveData()

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }
    fun onHideCompleted(hideCompleted : Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

    fun onTaskSelected(task: Task) = viewModelScope.launch {
        tasksEventChannel.send(TasksEvent.NavigateToEditTaskScreen(task))
    }

    fun onTaskCheckedChanged(task: Task,isChecked :Boolean) = viewModelScope.launch {
        taskDao.update(task.copy(completed = isChecked))
    }

    fun  onTaskSwiped(task: Task) = viewModelScope.launch {
        taskDao.delete(task)
        tasksEventChannel.send(TasksEvent.ShowUndoDeleteTaskMessage(task))
    }

    fun onUndoDeleteClick(task: Task) = viewModelScope.launch {
        taskDao.insert(task)
    }

    fun onAddNewTaskClick() = viewModelScope.launch {

        tasksEventChannel.send(TasksEvent.NavigateToAddTaskScreen)

    }

    sealed class TasksEvent{
        data class ShowUndoDeleteTaskMessage(val task: Task) : TasksEvent()
        object NavigateToAddTaskScreen : TasksEvent()
        data class NavigateToEditTaskScreen( val task: Task) : TasksEvent()
    }
}
