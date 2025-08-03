package com.mobicom.budgetwise

class ExpenseDataHelper {
    companion object {
        private val expenseList = arrayListOf(
            ExpenseModel("Lunch at Jollibee", "Jul 10, 2025", "150.0", "Food"),
            ExpenseModel("Grocery - Milk & Eggs", "Jul 9, 2025", "320.5", "Groceries"),
            ExpenseModel("Netflix Subscription", "Jul 1, 2025", "550.0", "Entertainment")
        )

        fun getExpenses(): ArrayList<ExpenseModel> {
            return expenseList
        }

        fun addExpense(expense: ExpenseModel) {
            expenseList.add(expense)
        }

        fun updateExpense(index: Int, updated: ExpenseModel) {
            expenseList[index] = updated
        }
    }
}
