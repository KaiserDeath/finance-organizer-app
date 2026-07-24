package pe.moneyflow.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps a stored category/payment-method [iconKey] to a Material icon. */
fun iconForKey(iconKey: String): ImageVector = when (iconKey) {
    "food" -> Icons.Rounded.Fastfood
    "restaurant" -> Icons.Rounded.RestaurantMenu
    "transport" -> Icons.Rounded.DirectionsBus
    "fuel" -> Icons.Rounded.LocalGasStation
    "home" -> Icons.Rounded.Home
    "account_balance" -> Icons.Rounded.AccountBalance
    "bolt" -> Icons.Rounded.Bolt
    "wifi" -> Icons.Rounded.Wifi
    "phone" -> Icons.Rounded.Smartphone
    "movie" -> Icons.Rounded.Movie
    "sports_esports" -> Icons.Rounded.SportsEsports
    "shopping" -> Icons.Rounded.ShoppingCart
    "health" -> Icons.Rounded.MedicalServices
    "school" -> Icons.Rounded.School
    "subscriptions" -> Icons.Rounded.Subscriptions
    "shield" -> Icons.Rounded.Shield
    "flight" -> Icons.Rounded.Flight
    "pets" -> Icons.Rounded.Pets
    "receipt" -> Icons.Rounded.ReceiptLong
    "business" -> Icons.Rounded.BusinessCenter
    "trending_up" -> Icons.Rounded.TrendingUp
    "savings" -> Icons.Rounded.Savings
    "payments", "cash" -> Icons.Rounded.Payments
    "work" -> Icons.Rounded.Work
    "gift" -> Icons.Rounded.CardGiftcard
    "attach_money" -> Icons.Rounded.AttachMoney
    "card" -> Icons.Rounded.CreditCard
    "wallet" -> Icons.Rounded.AccountBalanceWallet
    else -> Icons.Rounded.Category
}
