package com.mobicom.budgetwise

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.database.*

class AddExpenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        val spinner: Spinner = findViewById(R.id.expenseSpinner)
        val etvName: EditText = findViewById(R.id.etvName)
        val etvPrice: EditText = findViewById(R.id.etvPrice)
        val etvDate: EditText = findViewById(R.id.etvDate)
        val btnAdd: Button = findViewById(R.id.btnAddExp)
        val btnCancel: Button = findViewById(R.id.btnCancel)

        val items = listOf(
            "Food", "Groceries", "Transportation", "Utilities", "Savings",
            "Entertainment", "Fitness & Health", "Shopping", "Other"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        etvDate.setText(dateFormat.format(calendar.time))

//        val userId = intent.getStringExtra("userId")
        val sharedPrefs = getSharedPreferences("BudgetWisePrefs", MODE_PRIVATE)
        val userId = sharedPrefs.getString("userId", null)
        val email = sharedPrefs.getString("email", null)


        // Handle data from ScanReceiptActivity
        handleScannedData(spinner, etvName, etvPrice, etvDate, items)

        etvDate.setOnClickListener {
            val existingDate = etvDate.text.toString()
            val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val receiptFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val tempCalendar = Calendar.getInstance()

            try {
                // Try parsing using display format first (e.g., from previous manual entry)
                val parsedDisplay = displayFormat.parse(existingDate)
                if (parsedDisplay != null) {
                    tempCalendar.time = parsedDisplay
                } else {
                    // Try parsing from receipt format (e.g., extracted date like 22/08/2018)
                    val parsedReceipt = receiptFormat.parse(existingDate)
                    if (parsedReceipt != null) {
                        tempCalendar.time = parsedReceipt
                    }
                }
            } catch (e: Exception) {
                // fallback to current date
            }

            val year = tempCalendar.get(Calendar.YEAR)
            val month = tempCalendar.get(Calendar.MONTH)
            val day = tempCalendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                tempCalendar.set(y, m, d)
                etvDate.setText(displayFormat.format(tempCalendar.time)) // display in readable format
            }, year, month, day).show()
        }


        btnAdd.setOnClickListener {
            val name = etvName.text.toString()
            val price = etvPrice.text.toString()
            val date = etvDate.text.toString()
            val category = spinner.selectedItem.toString()

            if (name.isEmpty() || price.isEmpty() || date.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val priceValue = price.toDouble()
                if (priceValue <= 0) {
                    Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId == null) {
                Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expense = hashMapOf(
                "name" to name,
                "date" to date,
                "price" to price,
                "category" to category
            )

            val db = FirebaseDatabase.getInstance().reference
                .child("Expenses")
                .child(userId)

            db.push().setValue(expense)
                .addOnSuccessListener {
                    Toast.makeText(this, "Saving expense for userId: $userId", Toast.LENGTH_LONG).show()

                    Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ExpenseHistoryActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add expense: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun handleScannedData(
        spinner: Spinner,
        etvName: EditText,
        etvPrice: EditText,
        etvDate: EditText,
        items: List<String>
    ) {
        val scannedName = intent.getStringExtra("name")
        val scannedPrice = intent.getStringExtra("price")
        val scannedDate = intent.getStringExtra("date")
        val scannedCategory = intent.getStringExtra("category")

        if (!scannedName.isNullOrEmpty() && scannedName != "Unknown Merchant") {
            etvName.setText(scannedName)
        }

        if (!scannedPrice.isNullOrEmpty() && scannedPrice != "0.00") {
            etvPrice.setText(scannedPrice)
        }

        if (!scannedDate.isNullOrEmpty()) {
            try {
                val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val parsedDate = inputFormat.parse(scannedDate)
                if (parsedDate != null) {
                    etvDate.setText(outputFormat.format(parsedDate))
                } else {
                    etvDate.setText(scannedDate) // fallback
                }
            } catch (e: Exception) {
                etvDate.setText(scannedDate) // fallback on parsing error
            }
        }

        if (!scannedCategory.isNullOrEmpty()) {
            val categoryIndex = items.indexOf(scannedCategory)
            if (categoryIndex != -1) {
                spinner.setSelection(categoryIndex)
            }
        }

        if (!scannedName.isNullOrEmpty() || !scannedPrice.isNullOrEmpty() || !scannedCategory.isNullOrEmpty()) {
            Toast.makeText(this, "Receipt data extracted successfully", Toast.LENGTH_SHORT).show()
        }
    }

}
