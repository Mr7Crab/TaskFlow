package com.example.taskflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.Task
import com.example.taskflow.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val allTasks: Flow<List<Task>> =
        repository.allTasks

    fun reorderTasks(taskIds: List<Long>) {
        viewModelScope.launch {
            repository.reorderTasks(taskIds)
        }
    }


    fun addTask(
        title: String,
        description: String,
        priority: Int,
        dueDate: Long?
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            repository.insert(
                Task(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate
                )
            )
        }
    }


    fun updateTask(
        task: Task,
        title: String,
        description: String,
        priority: Int,
        dueDate: Long?
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            repository.update(
                task.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate
                )
            )
        }
    }

    fun setTaskCompleted(
        task: Task,
        completed: Boolean
    ) {
        viewModelScope.launch {
            repository.update(
                task.copy(completed = completed)
            )
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(
                TaskViewModel::class.java
            )
        ) {
            return TaskViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}
