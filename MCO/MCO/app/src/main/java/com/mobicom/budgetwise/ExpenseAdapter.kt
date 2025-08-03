package com.mobicom.budgetwise

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.RecyclerView
import com.mobicom.budgetwise.databinding.ExpItemLayoutBinding

class ExpenseAdapter(
    private val fullList: ArrayList<ExpenseModel>,
    private val viewExpenseLauncher: ActivityResultLauncher<Intent>,
    private val userId: String,
    private val onLongClick: (ExpenseModel) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    companion object {
        const val KEY_NAME = "KEY_NAME"
        const val KEY_PRICE = "KEY_PRICE"
        const val KEY_DATE = "KEY_DATE"
        const val KEY_CATEGORY = "KEY_CATEGORY"
        const val KEY_ID = "KEY_ID"
        const val KEY_USER_ID = "KEY_USER_ID"
    }

    private var displayedList: List<ExpenseModel> = ArrayList(fullList)

    inner class ExpenseViewHolder(val binding: ExpItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: ExpenseModel) {
            binding.expenseName.text = expense.name
            binding.expenseDate.text = expense.date
            binding.expenseAmount.text = "₱${expense.price}"

            // ✅ Handle icon for category (make sure IconMapper exists)
            binding.iconImageView.setImageResource(IconMapper.getIconResource(expense.category))

            // ✅ On click: open ViewExpenseActivity with all details
            binding.root.setOnClickListener {
                val intent = Intent(binding.root.context, ViewExpenseActivity::class.java).apply {
                    putExtra(KEY_NAME, expense.name)
                    putExtra(KEY_PRICE, expense.price)
                    putExtra(KEY_DATE, expense.date)
                    putExtra(KEY_CATEGORY, expense.category)
                    putExtra(KEY_ID, expense.id) // Firebase key
                    putExtra(KEY_USER_ID, userId) // Logged-in userId
                }
                viewExpenseLauncher.launch(intent)

            }

            binding.root.setOnLongClickListener {
                onLongClick(expense)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ExpItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(displayedList[position])
    }

    override fun getItemCount(): Int = displayedList.size

    fun setExpenseList(newList: List<ExpenseModel>) {
        displayedList = newList
        notifyDataSetChanged()
    }

    fun resetToFullList() {
        displayedList = fullList
        notifyDataSetChanged()
    }
}
