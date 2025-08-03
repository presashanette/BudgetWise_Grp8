package com.mobicom.budgetwise

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class EditExpenseActivity : AppCompatActivity() {

    private var expenseId = ""
    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_exp)

        val sharedPrefs = getSharedPreferences("BudgetWisePrefs", MODE_PRIVATE)
        val userId = sharedPrefs.getString("userId", null)
        val email = sharedPrefs.getString("email", null)

        // Retrieve data from intent
        expenseId = intent.getStringExtra("expenseId") ?: ""
//        userId = intent.getStringExtra("userId") ?: ""
        val name = intent.getStringExtra(ExpenseAdapter.KEY_NAME) ?: ""
        val price = intent.getStringExtra(ExpenseAdapter.KEY_PRICE) ?: ""
        val date = intent.getStringExtra(ExpenseAdapter.KEY_DATE) ?: ""
        val category = intent.getStringExtra(ExpenseAdapter.KEY_CATEGORY) ?: ""

        // UI elements
        val spinner: Spinner = findViewById(R.id.expenseSpinner)
        val etvName: EditText = findViewById(R.id.etvName)
        val etvPrice: EditText = findViewById(R.id.etvPrice)
        val etvDate: EditText = findViewById(R.id.etvDate)
        val btnSave: Button = findViewById(R.id.btnSaveExp)
        val btnCancel: Button = findViewById(R.id.btnCancel)

        val items = listOf("Food", "Groceries", "Transportation", "Utilities", "Savings", "Entertainment", "Fitness & Health", "Shopping", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Pre-fill fields
        etvName.setText(name)
        etvPrice.setText(price)
        etvDate.setText(date)
        val categoryIndex = items.indexOf(category)
        if (categoryIndex >= 0) spinner.setSelection(categoryIndex)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        etvDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                calendar.set(y, m, d)
                etvDate.setText(dateFormat.format(calendar.time))
            }, year, month, day).show()
        }

        // ✅ Save changes to Firebase
        btnSave.setOnClickListener {
            val updatedExpense = mapOf(
                "name" to etvName.text.toString(),
                "price" to etvPrice.text.toString(),
                "date" to etvDate.text.toString(),
                "category" to spinner.selectedItem.toString()
            )

            if (!userId.isNullOrEmpty() && expenseId.isNotEmpty()) {
                FirebaseDatabase.getInstance().reference
                    .child("Expenses")
                    .child(userId!!)
                    .child(expenseId)
                    .updateChildren(updatedExpense)
                    .addOnSuccessListener {
                        val intent = Intent(this, ExpenseHistoryActivity::class.java)
                        intent.putExtra("userId", userId)
                        startActivity(intent)
                        finish()
                    }
            }
        }

        btnCancel.setOnClickListener {
            val intent = Intent(this, ExpenseHistoryActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }
    }
}
