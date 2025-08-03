package com.mobicom.budgetwise

import com.mobicom.budgetwise.R

object IconMapper {
    fun getIconResource(category: String): Int {
        return when (category.lowercase()) {
            "food" -> R.drawable.baseline_fastfood_24
            "groceries" -> R.drawable.shopping_basket_24dp_137a68_fill0_wght400_grad0_opsz24
            "transportation" -> R.drawable.baseline_directions_car_24
            "utilities" -> R.drawable.electrical_services_24dp_137a68_fill0_wght400_grad0_opsz24
            "savings" -> R.drawable.savings_24dp_137a68_fill0_wght400_grad0_opsz24
            "entertainment" -> R.drawable.baseline_local_movies_24
            "fitness & health" -> R.drawable.baseline_fitness_center_24
            "shopping" -> R.drawable.shopping_cart_24dp_137a68_fill0_wght400_grad0_opsz24__1_
            else -> R.drawable.attach_money_24dp_137a68_fill0_wght400_grad0_opsz24 // fallback/default icon
        }
    }
}
