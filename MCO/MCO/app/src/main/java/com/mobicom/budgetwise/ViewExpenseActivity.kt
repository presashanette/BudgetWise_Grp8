package com.mobicom.budgetwise

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class ViewExpenseActivity : AppCompatActivity() {

    private var expenseId = ""
    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_exp)

        val sharedPrefs = getSharedPreferences("BudgetWisePrefs", MODE_PRIVATE)
        val userId = sharedPrefs.getString("userId", null)
        val email = sharedPrefs.getString("email", null)

        // Get data from intent
        val name = intent.getStringExtra(ExpenseAdapter.KEY_NAME) ?: ""
        val price = intent.getStringExtra(ExpenseAdapter.KEY_PRICE) ?: ""
        val date = intent.getStringExtra(ExpenseAdapter.KEY_DATE) ?: ""
        val category = intent.getStringExtra(ExpenseAdapter.KEY_CATEGORY) ?: ""
        expenseId = intent.getStringExtra(ExpenseAdapter.KEY_ID) ?: ""
//        userId = intent.getStringExtra(ExpenseAdapter.KEY_USER_ID) ?: ""

        // Set UI
        findViewById<TextView>(R.id.exp_categ).text = category.uppercase()
        findViewById<TextView>(R.id.exp_name).text = name
        findViewById<TextView>(R.id.exp_price).text = "₱$price"
        findViewById<TextView>(R.id.exp_date).text = date
        findViewById<ImageView>(R.id.iconImageView).setImageResource(IconMapper.getIconResource(category))

        // ✅ Back button: return to ExpenseHistoryActivity
        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            val intent = Intent(this, ExpenseHistoryActivity::class.java)
//            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        // ✅ Edit button: open EditExpenseActivity with expense details
        findViewById<ImageView>(R.id.editBtn).setOnClickListener {
            val intent = Intent(this, EditExpenseActivity::class.java)
            intent.putExtra("expenseId", expenseId)
//            intent.putExtra("userId", userId)
            intent.putExtra(ExpenseAdapter.KEY_NAME, name)
            intent.putExtra(ExpenseAdapter.KEY_PRICE, price)
            intent.putExtra(ExpenseAdapter.KEY_DATE, date)
            intent.putExtra(ExpenseAdapter.KEY_CATEGORY, category)
            startActivity(intent)
            finish()
        }

        // ✅ Delete button: remove from Firebase under correct userId node
        findViewById<Button>(R.id.deleteBtn).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete") { _, _ ->
                    if (!userId.isNullOrEmpty() && expenseId.isNotEmpty()) {
                        FirebaseDatabase.getInstance().reference
                            .child("Expenses")
                            .child(userId)
                            .child(expenseId)
                            .removeValue()
                            .addOnSuccessListener {
                                val intent = Intent(this, ExpenseHistoryActivity::class.java)
//                                intent.putExtra("userId", userId)
                                startActivity(intent)
                                finish()
                            }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
