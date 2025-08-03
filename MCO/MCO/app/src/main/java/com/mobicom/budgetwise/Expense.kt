package com.mobicom.budgetwise

data class ExpenseModel(
    var id: String = "",  // Firebase key for the expense
    var name: String = "",
    var date: String = "",
    var price: String = "",
    var category: String = ""
)