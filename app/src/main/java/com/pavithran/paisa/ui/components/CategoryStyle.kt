package com.pavithran.paisa.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pavithran.paisa.ui.theme.CatBills
import com.pavithran.paisa.ui.theme.CatFood
import com.pavithran.paisa.ui.theme.CatGroceries
import com.pavithran.paisa.ui.theme.CatHealth
import com.pavithran.paisa.ui.theme.CatOther
import com.pavithran.paisa.ui.theme.CatPersonal
import com.pavithran.paisa.ui.theme.CatShopping
import com.pavithran.paisa.ui.theme.CatTransport

/** Colour and icon for a category, shared by the list rows and the summary ring. */
data class CategoryStyle(val color: Color, val icon: ImageVector)

fun styleFor(category: String): CategoryStyle = when (category) {
    "Food" -> CategoryStyle(CatFood, Icons.Filled.Restaurant)
    "Transport" -> CategoryStyle(CatTransport, Icons.Filled.DirectionsCar)
    "Groceries" -> CategoryStyle(CatGroceries, Icons.Filled.ShoppingBasket)
    "Shopping" -> CategoryStyle(CatShopping, Icons.Filled.ShoppingBag)
    "Health" -> CategoryStyle(CatHealth, Icons.Filled.LocalHospital)
    "Bills" -> CategoryStyle(CatBills, Icons.Filled.ReceiptLong)
    "Personal" -> CategoryStyle(CatPersonal, Icons.Filled.Spa)
    else -> CategoryStyle(CatOther, Icons.Filled.MoreHoriz)
}
