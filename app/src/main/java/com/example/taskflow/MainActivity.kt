package com.example.taskflow

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.adapter.TaskAdapter
import com.example.taskflow.data.Task
import com.example.taskflow.data.TaskDatabase
import com.example.taskflow.data.TaskRepository
import com.example.taskflow.viewmodel.TaskViewModel
import com.example.taskflow.viewmodel.TaskViewModelFactory
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.taskflow.touch.TaskTouchHelperCallback
import com.google.android.material.snackbar.Snackbar
import android.app.DatePickerDialog
import android.widget.ArrayAdapter
import android.widget.Spinner
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import android.view.View
import android.widget.AdapterView



class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var viewModel: TaskViewModel

    private val pendingDeletes = mutableSetOf<Long>()
    private var latestDbTasks: List<Task> = emptyList()

    private var selectedFilter = 0

// 0 = All
// 1 = Active
// 2 = Completed



    private fun refreshVisibleTasks() {

        // Exclude tasks awaiting swipe-delete confirmation
        val availableTasks = latestDbTasks.filterNot {
            it.id in pendingDeletes
        }

        // Apply the currently selected filter
        val visibleTasks = when (selectedFilter) {
            1 -> availableTasks.filter { !it.completed }
            2 -> availableTasks.filter { it.completed }
            else -> availableTasks
        }

        taskAdapter.submitTasks(visibleTasks)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Create the database and repository
        val database = TaskDatabase.getDatabase(
            applicationContext
        )

        val repository = TaskRepository(
            database.taskDao()
        )

        // 2. Obtain our ViewModel
        val factory = TaskViewModelFactory(repository)

        viewModel = ViewModelProvider(
            this, factory
        )[TaskViewModel::class.java]

        // 3. Set up RecyclerView
        recyclerView = findViewById(
            R.id.recyclerViewTasks
        )

        taskAdapter = TaskAdapter(
            onEdit = { task ->
                showTaskDialog(task)
            },

            onDelete = { task ->
                showDeleteConfirmation(task)
            },

            onComplete = { task, completed ->
                viewModel.setTaskCompleted(
                    task, completed
                )
            }
        )

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter = taskAdapter

        val filterSpinner: Spinner =
            findViewById(R.id.spinnerTaskFilter)

        val filterOptions = arrayOf(
            "All Tasks",
            "Active Tasks",
            "Completed Tasks"
        )

        val filterAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            filterOptions
        )

        filterAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        filterSpinner.adapter = filterAdapter

        filterSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedFilter = position
                    refreshVisibleTasks()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    selectedFilter = 0
                    refreshVisibleTasks()
                }
            }


        val touchCallback = TaskTouchHelperCallback(
            adapter = taskAdapter,

            onSwipe = { position ->
                handleSwipeDelete(position)
            },

            onReorder = { taskIds ->
                viewModel.reorderTasks(taskIds)
            },

            canDrag = {
                selectedFilter == 0 && pendingDeletes.isEmpty()
            }
        )


        val itemTouchHelper = ItemTouchHelper(touchCallback)

        itemTouchHelper.attachToRecyclerView(recyclerView)


        // 4. Observe database changes


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.allTasks.collect { tasks ->

                    latestDbTasks = tasks

                    pendingDeletes.removeAll { id ->
                        tasks.none { it.id == id }
                    }

                    refreshVisibleTasks()
                }
            }
        }



        // 5. Set up Add button
        val addButton: Button =
            findViewById(R.id.buttonAddTask)

        addButton.setOnClickListener {
            showTaskDialog()
        }

    }


    private fun handleSwipeDelete(position: Int) {

        val task = taskAdapter.getTaskAt(position) ?: return

        // Hide the item immediately
        pendingDeletes.add(task.id)
        taskAdapter.removeAt(position)

        val snackbar = Snackbar.make(
            recyclerView,
            "Task deleted: ${task.title}",
            Snackbar.LENGTH_LONG
        )

        snackbar.setAction("UNDO") {

            // Cancel deletion
            pendingDeletes.remove(task.id)

            // Restore the task from the Room snapshot
            refreshVisibleTasks()
        }

        snackbar.addCallback(object : Snackbar.Callback() {

            override fun onDismissed(
                transientBottomBar: Snackbar?,
                event: Int
            ) {
                super.onDismissed(transientBottomBar, event)

                if (event != DISMISS_EVENT_ACTION) {
                    // Snackbar expired or was dismissed
                    // Permanently delete the task from Room
                    viewModel.deleteTask(task)
                }
            }
        })

        snackbar.show()
    }

    // Add or edit task

    private fun showTaskDialog(task: Task? = null) {

        val isEditing = task != null

        // Keep the selected deadline in memory
        var selectedDueDate: Long? = task?.dueDate

        // Main vertical container for the dialog
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            val padding =
                (20 * resources.displayMetrics.density).toInt()

            setPadding(padding, padding, padding, 0)
        }

        // Task title
        val titleInput = EditText(this).apply {
            hint = "Task title"
            setSingleLine(true)
            setText(task?.title ?: "")
        }

        // Optional description
        val descriptionInput = EditText(this).apply {
            hint = "Description (optional)"
            minLines = 2
            setText(task?.description ?: "")
        }

        // Priority selection
        val prioritySpinner = Spinner(this)

        val priorityOptions = arrayOf(
            "Low",
            "Medium",
            "High"
        )

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            priorityOptions
        )

        priorityAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        prioritySpinner.adapter = priorityAdapter

        // Use existing priority when editing
        prioritySpinner.setSelection(
            (task?.priority ?: 1).coerceIn(0, 2)
        )

        // Due-date selection button
        val dateButton = Button(this)

        fun updateDateButton() {
            dateButton.text = if (selectedDueDate != null) {
                "Due: " + DateFormat.getDateInstance(
                    DateFormat.MEDIUM
                ).format(Date(selectedDueDate!!))
            } else {
                "Select Due Date"
            }
        }

        updateDateButton()

        dateButton.setOnClickListener {

            val calendar = Calendar.getInstance()

            selectedDueDate?.let {
                calendar.timeInMillis = it
            }

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->

                    val selectedCalendar =
                        Calendar.getInstance().apply {
                            clear()
                            set(year, month, dayOfMonth)
                        }

                    selectedDueDate =
                        selectedCalendar.timeInMillis

                    updateDateButton()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePicker.show()
        }

        // Allow removing an existing deadline
        val clearDateButton = Button(this).apply {
            text = "Clear Due Date"
        }

        clearDateButton.setOnClickListener {
            selectedDueDate = null
            updateDateButton()
        }

        // Add components to the dialog
        container.addView(titleInput)
        container.addView(descriptionInput)
        container.addView(prioritySpinner)
        container.addView(dateButton)
        container.addView(clearDateButton)

        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle(
                if (isEditing) "Edit Task"
                else "Add Task"
            )
            .setView(container)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val title =
                    titleInput.text.toString().trim()

                val description =
                    descriptionInput.text.toString().trim()

                val priority =
                    prioritySpinner.selectedItemPosition

                if (title.isBlank()) {
                    titleInput.error = "Title cannot be empty"
                    return@setOnClickListener
                }

                if (task == null) {

                    // Add new task
                    viewModel.addTask(
                        title = title,
                        description = description,
                        priority = priority,
                        dueDate = selectedDueDate
                    )

                    Toast.makeText(
                        this,
                        "Task added",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    // Update existing task
                    viewModel.updateTask(
                        task = task,
                        title = title,
                        description = description,
                        priority = priority,
                        dueDate = selectedDueDate
                    )

                    Toast.makeText(
                        this,
                        "Task updated",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // Delete task after confirmation
    private fun showDeleteConfirmation(task: Task) {

        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage(
                "Are you sure you want to delete '${task.title}'?"
            )
            .setPositiveButton("Delete") { _, _ ->

                viewModel.deleteTask(task)

                Toast.makeText(
                    this,
                    "Task deleted",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

