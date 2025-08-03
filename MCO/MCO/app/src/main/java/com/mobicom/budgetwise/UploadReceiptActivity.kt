package com.mobicom.budgetwise


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class UploadReceiptActivity : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("BudgetWisePrefs", MODE_PRIVATE)
        val userId = sharedPrefs.getString("userId", null)
        val email = sharedPrefs.getString("email", null)

        // Directly start image picker
        openImagePicker()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                val image = InputImage.fromFilePath(this, imageUri)
                extractTextFromImage(image)
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun extractTextFromImage(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text

                val merchantName = parseMerchant(extractedText)
                val date = parseDate(extractedText)
                val amount = parseAmount(extractedText)
                val category = parseCategory(extractedText)

                val userId = intent.getStringExtra("userId")  // Get userId from UploadReceiptActivity's intent
                val intent = Intent(this, AddExpenseActivity::class.java)
                intent.putExtra("userId", userId)             // Pass it along to AddExpenseActivity
                intent.putExtra("name", merchantName)
                intent.putExtra("date", date)
                intent.putExtra("price", amount)
                intent.putExtra("category", category)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Text recognition failed", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun parseMerchant(text: String): String {
        val addressKeywords = listOf("street", "blvd", "road", "ave", "barangay", "zone", "city", "metro", "ph", "philippines", "zip", "contact", "email")

        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (line in lines) {
            // Skip line if it looks like an address
            val isAddress = addressKeywords.any { keyword -> line.contains(keyword, ignoreCase = true) }

            if (!isAddress) {
                // Clean and return
                return line.replace(Regex("""[^A-Za-z0-9\s&]"""), "").trim()
            }
        }

        // Fallback
        return lines.firstOrNull()?.replace(Regex("""[^A-Za-z0-9\s&]"""), "")?.trim() ?: "Unknown Merchant"
    }


    private fun parseDate(text: String): String {
        val dateRegexes = listOf(
            // Existing patterns with separators
            Regex("""\b\d{2}/\d{2}/\d{4}\b"""),     // 22/08/2018
            Regex("""\b\d{4}/\d{2}/\d{2}\b"""),     // 2018/08/22
            Regex("""\b\d{2}\.\d{2}\.\d{4}\b"""),   // 22.08.2018
            Regex("""\b\d{4}\.\d{2}\.\d{2}\b"""),   // 2018.08.22
            Regex("""\b\d{2}-\d{2}-\d{4}\b"""),     // 22-08-2018
            Regex("""\b\d{4}-\d{2}-\d{2}\b"""),     // 2018-08-22

            // NEW: Patterns WITHOUT separators (for OCR text)
            Regex("""\b\d{8}\b"""),                 // 22082018 or 20180822
        )

        val lines = text.lines()

        Log.d("DateParser", "Parsing text: $text")

        for (line in lines) {
            Log.d("DateParser", "Checking line: $line")
            for (regex in dateRegexes) {
                val match = regex.find(line)
                if (match != null) {
                    Log.d("DateParser", "Found date match: ${match.value}")
                    return formatDate(match.value)
                }
            }
        }

        Log.d("DateParser", "No date found, using current date")
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    }



    private fun formatDate(dateString: String): String {
        val inputFormats = listOf(
            // Existing formats with separators
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
            SimpleDateFormat("MM.dd.yyyy", Locale.getDefault()),
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()),
            SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),

            // NEW: Formats for dates WITHOUT separators
            SimpleDateFormat("ddMMyyyy", Locale.getDefault()),  // 22082018
            SimpleDateFormat("MMddyyyy", Locale.getDefault()),  // 08222018
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()),  // 20180822
        )

        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        for (format in inputFormats) {
            try {
                val date = format.parse(dateString)
                if (date != null) {
                    Log.d("DateParser", "Successfully parsed $dateString as ${outputFormat.format(date)}")
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                continue
            }
        }

        Log.d("DateParser", "Failed to parse date: $dateString")
        return dateString
    }

    private fun parseAmount(text: String): String {
        // Patterns that match lines with ₱ or common total-related keywords
        val amountPatterns = listOf(
            Regex("""(?:take out total|amount due|total amount to be paid|total|subtotal)[:\s]*₱?\s*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2}))""", RegexOption.IGNORE_CASE),
            Regex("""₱\s*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2}))"""),
            Regex("""(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2}))""") // fallback: any amount-looking number
        )

        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "").replace(" ", "")
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    return "%.2f".format(amount)
                }
            }
        }

        return "0.00"
    }





    private fun parseCategory(text: String): String {
        val lower = text.lowercase()

        return when {
            // Food-related keywords
            "restaurant" in lower || "cafe" in lower || "coffee" in lower ||
                    "pizza" in lower || "burger" in lower || "food" in lower ||
                    "dining" in lower || "bar" in lower || "grill" in lower -> "Food"

            // Grocery-related keywords
            "supermarket" in lower || "grocery" in lower || "market" in lower ||
                    "store" in lower || "mart" in lower || "shop" in lower -> "Groceries"

            // Transportation-related keywords
            "taxi" in lower || "uber" in lower || "lyft" in lower || "bus" in lower ||
                    "train" in lower || "transport" in lower || "gas" in lower ||
                    "fuel" in lower || "parking" in lower -> "Transportation"

            // Utilities-related keywords
            "electric" in lower || "water" in lower || "gas" in lower || "internet" in lower ||
                    "phone" in lower || "utility" in lower || "bill" in lower -> "Utilities"

            // Entertainment-related keywords
            "movie" in lower || "cinema" in lower || "theater" in lower || "game" in lower ||
                    "entertainment" in lower || "fun" in lower -> "Entertainment"

            // Health-related keywords
            "pharmacy" in lower || "hospital" in lower || "clinic" in lower || "doctor" in lower ||
                    "health" in lower || "medical" in lower || "fitness" in lower || "gym" in lower -> "Fitness & Health"

            // Shopping-related keywords
            "clothing" in lower || "shoes" in lower || "fashion" in lower || "retail" in lower ||
                    "shopping" in lower || "mall" in lower -> "Shopping"

            else -> "Other"
        }
    }

}