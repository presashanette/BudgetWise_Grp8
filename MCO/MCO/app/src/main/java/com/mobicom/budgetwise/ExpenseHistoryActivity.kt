package com.mobicom.budgetwise

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ExpenseHistoryActivity : BaseActivity() {

    private lateinit var adapter: ExpenseAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var tvNoExpenses: TextView
    private lateinit var noExpIcon: ImageView
    private val data = ArrayList<ExpenseModel>()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exp_history)

        val sharedPrefs = getSharedPreferences("BudgetWisePrefs", MODE_PRIVATE)
        val userId = sharedPrefs.getString("userId", null)
        val email = sharedPrefs.getString("email", null)

//        userId = getUserIdFromPrefs()
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerView)
        tvNoExpenses = findViewById(R.id.tvNoExpenses)
        noExpIcon = findViewById(R.id.no_exp_icon)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            adapter.notifyDataSetChanged()
        }

        adapter = ExpenseAdapter(
            data,
            launcher,
            userId!!,
            onLongClick = { expense -> showEditDeleteDialog(expense) }
        )

        recyclerView.adapter = adapter

        val searchView = findViewById<SearchView>(R.id.searchView)
        val filterSpinner = findViewById<Spinner>(R.id.filterSpinner)

        val categories = listOf("All", "Food", "Groceries", "Transportation", "Utilities", "Savings", "Entertainment", "Fitness & Health", "Shopping")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterExpenses(newText, filterSpinner.selectedItem.toString())
                return true
            }
        })

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterExpenses(searchView.query.toString(), categories[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        dbRef = FirebaseDatabase.getInstance().reference.child("Expenses").child(userId!!)
        loadExpenses()
    }

    private fun loadExpenses() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                data.clear()
                for (expenseSnap in snapshot.children) {
                    val expense = expenseSnap.getValue(ExpenseModel::class.java)
                    if (expense != null) {
                        expense.id = expenseSnap.key ?: ""
                        data.add(expense)
                    }
                }

                if (data.isEmpty()) {
                    tvNoExpenses.visibility = View.VISIBLE
                    noExpIcon.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvNoExpenses.visibility = View.GONE
                    noExpIcon.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }

                adapter.resetToFullList()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ExpenseHistoryActivity, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterExpenses(searchQuery: String?, selectedCategory: String) {
        val filtered = data.filter {
            val matchesQuery = searchQuery.isNullOrBlank() || it.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)
            matchesQuery && matchesCategory
        }
        adapter.setExpenseList(filtered)
    }

    override fun getCurrentNavItemId(): Int = R.id.nav_expenses

    private fun showEditDeleteDialog(expense: ExpenseModel) {
        val options = arrayOf("Edit", "Delete")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Choose an action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Edit
                        val intent = Intent(this, EditExpenseActivity::class.java).apply {
                            putExtra("expenseId", expense.id)
                            putExtra("userId", userId)
                            putExtra(ExpenseAdapter.KEY_NAME, expense.name)
                            putExtra(ExpenseAdapter.KEY_PRICE, expense.price)
                            putExtra(ExpenseAdapter.KEY_DATE, expense.date)
                            putExtra(ExpenseAdapter.KEY_CATEGORY, expense.category)
                        }
                        startActivity(intent)
                    }
                    1 -> { // Delete
                        dbRef.child(expense.id).removeValue().addOnSuccessListener {
                            Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Failed to delete: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
