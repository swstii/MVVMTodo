package com.codinginflow.mvvmtodo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    fun getTasks(query:String, sortOrder : SortOrder, hideCompleted: Boolean) : Flow<List<Task>> =

        when(sortOrder){

            SortOrder.BY_NAME -> getTasksSortedByName(query,hideCompleted)
            SortOrder.BY_DATE -> getTasksSortedByDate(query,hideCompleted)

        }

    @Query("SELECT * FROM task_table WHERE (completed != :hideCompleted OR completed = 0) AND name LIKE '%' || :searchQuery || '%' ORDER BY  name ASC ")
    fun getTasksSortedByName(searchQuery : String , hideCompleted : Boolean): Flow<List<Task>>

    @Query("SELECT * FROM task_table WHERE (completed != :hideCompleted OR completed = 0) AND name LIKE '%' || :searchQuery || '%' ORDER BY  created ASC")
    fun getTasksSortedByDate(searchQuery : String , hideCompleted : Boolean): Flow<List<Task>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task : Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

}