package com.example.taskflow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction

@Dao
interface TaskDao {

    @Query(
        "UPDATE tasks SET sortOrder = :position WHERE id = :taskId"
    )
    suspend fun updateSortOrder(
        taskId: Long,
        position: Int
    )

    @Transaction
    suspend fun reorderTasks(taskIds: List<Long>) {
        taskIds.forEachIndexed { index, taskId ->
            updateSortOrder(taskId, index)
        }
    }

    @Query(
        "SELECT * FROM tasks ORDER BY sortOrder ASC, id ASC"
    )
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query(
        "SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tasks"
    )
    suspend fun getNextSortOrder(): Int

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}
