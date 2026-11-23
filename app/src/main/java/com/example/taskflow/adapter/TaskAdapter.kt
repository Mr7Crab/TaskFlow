
package com.example.taskflow.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.R
import com.example.taskflow.data.Task

import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import java.util.Date

class TaskAdapter(
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onComplete: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val tasks = mutableListOf<Task>()
    private var isDragging = false
    private var orderChanged = false

    class TaskViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {


        val priority: TextView =
            itemView.findViewById(R.id.textTaskPriority)

        val dueDate: TextView =
            itemView.findViewById(R.id.textTaskDueDate)

        val checkBox: CheckBox =
            itemView.findViewById(R.id.checkBoxTask)

        val title: TextView =
            itemView.findViewById(R.id.textTaskTitle)

        val description: TextView =
            itemView.findViewById(R.id.textTaskDescription)

        val editButton: Button =
            itemView.findViewById(R.id.buttonEditTask)

        val deleteButton: Button =
            itemView.findViewById(R.id.buttonDeleteTask)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaskViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)

        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TaskViewHolder,
        position: Int
    ) {
        val task = tasks[position]

        holder.title.text = task.title
        holder.description.text = task.description

        holder.priority.text = when (task.priority) {
            0 -> "Low"
            1 -> "Medium"
            2 -> "High"
            else -> "Medium"
        }

        val priorityColor = when (task.priority) {
            0 -> android.R.color.holo_green_dark
            1 -> android.R.color.holo_orange_dark
            2 -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }

        holder.priority.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                priorityColor
            )
        )

        if (task.dueDate != null) {

            val formattedDate =
                DateFormat.getDateFormat(
                    holder.itemView.context
                ).format(Date(task.dueDate))

            holder.dueDate.text = formattedDate
            holder.dueDate.visibility = View.VISIBLE

        } else {
            holder.dueDate.text = ""
            holder.dueDate.visibility = View.GONE
        }



        holder.description.visibility =
            if (task.description.isBlank()) View.GONE
            else View.VISIBLE

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = task.completed

        updateTitleStyle(holder.title, task.completed)

        holder.checkBox.setOnCheckedChangeListener { _, checked ->
            val index = holder.bindingAdapterPosition

            if (index != RecyclerView.NO_POSITION) {
                onComplete(tasks[index], checked)
            }
        }

        holder.editButton.setOnClickListener {
            val index = holder.bindingAdapterPosition

            if (index != RecyclerView.NO_POSITION) {
                onEdit(tasks[index])
            }
        }

        holder.deleteButton.setOnClickListener {
            val index = holder.bindingAdapterPosition

            if (index != RecyclerView.NO_POSITION) {
                onDelete(tasks[index])
            }
        }
    }

    private fun updateTitleStyle(
        title: TextView,
        completed: Boolean
    ) {
        title.paintFlags =
            if (completed) {
                title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                title.paintFlags and
                        Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
    }

    override fun getItemCount(): Int = tasks.size

    // Receive new task lists from Room
    fun submitTasks(newTasks: List<Task>) {
        if (isDragging) return

        val oldTasks = tasks.toList()

        val result = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {

                override fun getOldListSize() =
                    oldTasks.size

                override fun getNewListSize() =
                    newTasks.size

                override fun areItemsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return oldTasks[oldItemPosition].id ==
                            newTasks[newItemPosition].id
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return oldTasks[oldItemPosition] ==
                            newTasks[newItemPosition]
                }
            }
        )

        tasks.clear()
        tasks.addAll(newTasks)
        result.dispatchUpdatesTo(this)
    }

    fun getTaskAt(position: Int): Task? {
        return tasks.getOrNull(position)
    }

    // Immediately remove a swiped row
    fun removeAt(position: Int) {
        if (position !in tasks.indices) return

        tasks.removeAt(position)
        notifyItemRemoved(position)
    }

    // Called when the user begins dragging
    fun startDrag() {
        isDragging = true
        orderChanged = false
    }

    // Move an item while dragging
    fun moveTask(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition !in tasks.indices ||
            toPosition !in tasks.indices) {
            return false
        }

        if (fromPosition == toPosition) return false

        val movedTask = tasks.removeAt(fromPosition)
        tasks.add(toPosition, movedTask)

        notifyItemMoved(fromPosition, toPosition)

        orderChanged = true
        return true
    }

    // Return the new ID order after dropping an item
    fun finishDrag(): List<Long>? {
        isDragging = false

        if (!orderChanged) return null

        orderChanged = false
        return tasks.map { it.id }
    }
}
