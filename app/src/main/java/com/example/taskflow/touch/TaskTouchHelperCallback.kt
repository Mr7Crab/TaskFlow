
package com.example.taskflow.touch

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.adapter.TaskAdapter


class TaskTouchHelperCallback(
    private val adapter: TaskAdapter,
    private val onSwipe: (Int) -> Unit,
    private val onReorder: (List<Long>) -> Unit,
    private val canDrag: () -> Boolean
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
    ItemTouchHelper.START or ItemTouchHelper.END
) {



    override fun isLongPressDragEnabled(): Boolean = canDrag()

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {

        val dragFlags = if (canDrag()) {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        } else {
            0
        }

        val swipeFlags =
            ItemTouchHelper.START or ItemTouchHelper.END

        return makeMovementFlags(dragFlags, swipeFlags)
    }


    override fun isItemViewSwipeEnabled(): Boolean = true

    override fun onSelectedChanged(
        viewHolder: RecyclerView.ViewHolder?,
        actionState: Int
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            adapter.startDrag()
        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {

        if (!canDrag()) return false
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition

        if (from == RecyclerView.NO_POSITION ||
            to == RecyclerView.NO_POSITION) {
            return false
        }

        return adapter.moveTask(from, to)
    }

    override fun onSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int
    ) {
        val position = viewHolder.bindingAdapterPosition

        if (position != RecyclerView.NO_POSITION) {
            onSwipe(position)
        }
    }

    override fun clearView(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ) {
        super.clearView(recyclerView, viewHolder)

        val newOrder = adapter.finishDrag()

        if (newOrder != null) {
            onReorder(newOrder)
        }
    }
}
