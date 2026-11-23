package com.example.taskflow.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao
) {

    val allTasks: Flow<List<Task>> =
        taskDao.getAllTasks()

    suspend fun insert(task: Task) {
        val nextOrder = taskDao.getNextSortOrder()
        taskDao.insertTask(
            task.copy(sortOrder = nextOrder)
        )
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun delete(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun reorderTasks(taskIds: List<Long>) {
        taskDao.reorderTasks(taskIds)
    }

}
