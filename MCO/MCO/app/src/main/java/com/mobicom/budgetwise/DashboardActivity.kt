package com.mobicom.budgetwise

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.ValueFormatter

class DashboardActivity : BaseActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val categoryTotals = mutableMapOf(
        "Food" to 0f,
        "Groceries" to 0f,
        "Transportation" to 0f,
        "Utilities" to 0f,
        "Savings" to 0f,
        "Entertainment" to 0f,
        "Fitness/Health" to 0f,
        "Shopping" to 0f
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        pieChart = findViewById(R.id.pieChart)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
//        val email = intent.getStringExtra("email")
//        val userId = intent.getStringExtra("userId")
        val sharedPrefs = getSharedPreferences("BudgetWisePrefs", MODE_PRIVATE)
        val userId = sharedPrefs.getString("userId", null)
        val email = sharedPrefs.getString("email", null)

        Log.d("DebugUserId", "Received userId: $userId")


        if (email == null || userId == null) {
            Toast.makeText(this, "Not logged in: Missing email or userId", Toast.LENGTH_LONG).show()
        } else {
            val username = email.substringBefore("@")
            findViewById<TextView>(R.id.tvUser).text = "$username"
//            updateWelcomeText()
            fetchExpenses(userId)
        }


//        val userId = auth.currentUser?.uid
//        if (userId != null) {
//            updateWelcomeText()
//            fetchExpenses(userId)
//        } else {
//            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
//        }
    }

    private fun updateWelcomeText() {
        val email = auth.currentUser?.email
        val username = email?.substringBefore("@") ?: "User"
        findViewById<TextView>(R.id.tvUser).text = "Welcome, $username"
    }

    private fun fetchExpenses(userId: String) {
        val expensesRef = database.child("Expenses").child(userId)

        expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Reset totals before populating
                categoryTotals.keys.forEach { categoryTotals[it] = 0f }

                snapshot.children.forEach { expenseSnap ->
                    val category = expenseSnap.child("category").value?.toString()?.lowercase()
                        ?: return@forEach
                    val price = expenseSnap.child("price").value?.toString()?.toFloatOrNull()
                        ?: return@forEach

                    Log.d("ExpenseDebug", "Category: $category | Price: $price")

                    when (category) {
                        "food" -> categoryTotals["Food"] = categoryTotals["Food"]!! + price
                        "groceries" -> categoryTotals["Groceries"] = categoryTotals["Groceries"]!! + price
                        "transportation" -> categoryTotals["Transportation"] = categoryTotals["Transportation"]!! + price
                        "utilities" -> categoryTotals["Utilities"] = categoryTotals["Utilities"]!! + price
                        "savings" -> categoryTotals["Savings"] = categoryTotals["Savings"]!! + price
                        "entertainment" -> categoryTotals["Entertainment"] = categoryTotals["Entertainment"]!! + price
                        "fitness & health" -> categoryTotals["Fitness/Health"] = categoryTotals["Fitness/Health"]!! + price
                        "shopping" -> categoryTotals["Shopping"] = categoryTotals["Shopping"]!! + price
                        "other" -> categoryTotals["Other"] = categoryTotals["Other"]?.plus(price) ?: price
                    }
                }

                updateTextViews()
                updatePieChart()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DashboardActivity, "Failed to load expenses", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateTextViews() {
        findViewById<TextView>(R.id.tvPrice1).text = "₱%.2f".format(categoryTotals["Food"])
        findViewById<TextView>(R.id.tvPrice2).text = "₱%.2f".format(categoryTotals["Groceries"])
        findViewById<TextView>(R.id.tvPrice3).text = "₱%.2f".format(categoryTotals["Transportation"])
        findViewById<TextView>(R.id.tvPrice4).text = "₱%.2f".format(categoryTotals["Utilities"])
        findViewById<TextView>(R.id.tvPrice5).text = "₱%.2f".format(categoryTotals["Savings"])
        findViewById<TextView>(R.id.tvPrice6).text = "₱%.2f".format(categoryTotals["Entertainment"])
        findViewById<TextView>(R.id.tvPrice7).text = "₱%.2f".format(categoryTotals["Fitness/Health"])
        findViewById<TextView>(R.id.tvPrice8).text = "₱%.2f".format(categoryTotals["Shopping"])
    }

    private fun updatePieChart() {
        val categoryColorMap = mapOf(
            "Food" to Color.parseColor("#F44336"),
            "Groceries" to Color.parseColor("#1c7bc7"),
            "Transportation" to Color.parseColor("#FC478A"),
            "Utilities" to Color.parseColor("#03A9F4"),
            "Savings" to Color.parseColor("#9C27B0"),
            "Entertainment" to Color.parseColor("#009688"),
            "Fitness/Health" to Color.parseColor("#3F51B5"),
            "Shopping" to Color.parseColor("#4CAF50"),
            "Other" to Color.parseColor("#FF9800")
        )

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        for ((category, value) in categoryTotals) {
            if (value > 0) {
                entries.add(PieEntry(value, category))
                colors.add(categoryColorMap[category] ?: Color.GRAY)
            }
        }

        if (entries.isEmpty()) {
            entries.add(PieEntry(1f, "")) // dummy entry
            colors.add(Color.LTGRAY)     // default gray
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (entries.size == 1 && entries[0].label == "") "" else String.format("%.1f%%", value)
                }
            })
        }

        pieChart.apply {
            data = pieData
            setUsePercentValues(true)
            centerText = if (entries.size == 1 && entries[0].label == "") "No Data" else "Financial Allocation"
            setDrawEntryLabels(false)
            description.isEnabled = false
            legend.isEnabled = false
            invalidate()
        }
    }

    override fun getCurrentNavItemId(): Int = R.id.nav_dashboard
}
