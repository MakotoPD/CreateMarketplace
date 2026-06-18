# 🏪 Create: Marketplace

![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Minecraft: 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)
![NeoForge: 21.1.228+](https://img.shields.io/badge/NeoForge-21.1.228+-orange.svg)

**Create: Marketplace** is a specialized expansion for the **Create** mod ecosystem on NeoForge 1.21.1. It bridges the gap between industrial automation and player commerce by providing a unified, server-wide advertising board for all **Create: Numismatics** vendors — plus a dedicated **Server Vendor** block for admin-run shops with unlimited stock.

---

## ✨ Features

### 🛒 Global Market Interface
*   **Centralized Hub:** Access all player shops from a single, beautiful GUI.
*   **Live Search:** Find specific items, shops, or owners instantly with the built-in search bar.
*   **Favorites System:** Mark your favorite shops with a star (★) to keep them at the top of your list.
*   **Grouped Listings:** Offers are logically grouped by owner and shop name for easy browsing.

### 📍 Seamless Navigation
*   **Xaero's Minimap Integration:** One-click navigation! Sent waypoints directly to your minimap to find shops in the world.
*   **Customizable Icons:** Choose between a shopping basket icon, the shop's first letter, or a custom symbol for your waypoints via config.

### 💳 Registration System
*   **Registration Card:** A dedicated tool used to link your Numismatics Vendors to the global board.
*   **Dynamic Names:** Give your shops unique names and add multiple items to the same shop listing.
*   **Configurable Durability:** Server owners can decide if registration cards are consumed upon use or have infinite durability.

### 🏧 Server Vendor (Admin Shop)
*   **Unlimited Stock:** A server-run shop block that buys and/or sells without any physical inventory, automation, or restocking — perfect for admin shops, spawn markets, and economy sinks/faucets.
*   **Buy & Sell:** Configure a sell price (players buy from the vendor) and/or a buy price (players sell to the vendor); each direction can be toggled independently.
*   **Flexible Currency:** Accept **Create: Numismatics** coins (any denomination — prices are valued in *spurs*) or **any item** as a custom currency.
*   **In-World Preview:** The traded item floats and spins above the block, with a floating name hologram and a price icon + quantity label so players can read the deal at a glance.
*   **Market Integration:** Server Vendor offers show up on the **Global Market** right alongside player shops.

### 🌐 Localization & Compatibility
*   **Multilingual:** Full support for **English**, **Polish**, **German**, **Spanish**, and **French**.
*   **Soft Dependency:** Works perfectly with or without Xaero's Minimap installed.
*   **Addon Support:** Compatible with [Create: Tradeworks](https://modrinth.com/mod/create-tradeworks) — register shops on Shelves, Metal Shelves, Side Shelves, and Inverted Table Cloths.

---

## 🛠 Requirements

*   **Minecraft:** 1.21.1
*   **NeoForge:** 21.1.228 or higher
*   **Dependencies:**
    *   [Create](https://modrinth.com/mod/create) (v0.6.1+)
    *   [Create: Numismatics](https://modrinth.com/mod/create-numismatics) (v1.0.0+)
*   **Optional Integration:**
    *   [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) & [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map)

---

## 📖 Getting Started

1.  **Craft a Registration Card:** Your gateway to the marketplace.
2.  **Set up your Vendor:** Use *Create: Numismatics* to set your items and prices.
3.  **Register:** Right-click your Vendor with the Registration Card to name and publish your shop.
4.  **Open Market:** Use the card (Right-click air) or the default hotkey (**`M`**) to browse the global market.
5.  **Manage:** Use the "My Shops" button within the market to delete or track your own listings.

---

## 🏧 Server Vendor

The **Server Vendor** is an admin-oriented shop block that trades items on behalf of the server. Unlike a regular *Numismatics* vendor, it has **no inventory** — it can sell and buy items endlessly, making it ideal for spawn shops, admin stores, and controlling the server economy.

### How it works

Each Server Vendor is configured with up to three item "templates":

| Template | Meaning |
| --- | --- |
| **Trade item** | What the vendor deals in. Its **stack count = the quantity exchanged per transaction** (e.g. a stack of 4 means players trade 4 at a time). |
| **Buy price** | What a player **pays** to buy the trade item from the vendor. |
| **Sell price** | What a player **receives** when selling the trade item to the vendor. |

The **Buy** and **Sell** directions can each be enabled or disabled, so a vendor can be buy-only, sell-only, or both.

### Pricing & currency

The price stack works in one of two modes:

*   **Numismatics coins (coinage mode):** When the price is a Numismatics coin, its value is read in **spurs** — the stack count is multiplied by the coin's denomination (e.g. `5 × cog = 5 × 64 = 320 spurs`). Players can pay with **any mix of coins** that adds up to the required spurs, and the vendor automatically makes change.
*   **Custom item currency:** Any non-coin item can be used as currency. The price is then simply counted by **item quantity**.

### Setting one up

1.  **Place** a Server Vendor block.
2.  **Open the admin screen** (operators or /createmarketplace adminmode) and drop in the trade item, the buy price, and/or the sell price, then toggle which directions are active.
3.  Players **right-click** the block to open the trade screen, pick a quantity, and confirm — coins/items are pulled straight from their inventory and the goods are handed over (or vice-versa for selling).
4.  The offer automatically appears on the **Global Market** so players can find it from anywhere.

---

## ⚙️ Configuration

Located in `config/create_marketplace-common.toml`:
*   `waypointSymbolMode`: Set how waypoints look on the map (`ICON`, `FIRST_LETTER`, `CUSTOM`).
*   `customWaypointSymbol`: Define a specific character for waypoints.
*   `useCardDurability`: Toggle whether Registration Cards are consumed when registering a shop.

---

## 🔌 Developers & API

Create: Marketplace provides a simple API for other modders to add support for their custom shop blocks.

### Dependency
To use the API, add the Modrinth Maven to your `build.gradle` and then add the dependency:

```gradle
repositories {
    maven {
        name = "Modrinth"
        url = "https://api.modrinth.com/maven"
    }
}

dependencies {
    // Replace 0.2.5 with the version you want to use
    implementation "maven.modrinth:create-marketplace:0.2.5"
}
```

### Adding Custom Shop Support
Implement `IShopHandler` and register it to support custom blocks:

```java
MarketApi.registerHandler(new MyCustomShopHandler());
```

### Market Events (NeoForge)
You can listen to market activities using the NeoForge event bus:

```java
@SubscribeEvent
public void onOfferRegistering(MarketOfferEvent.Register event) {
    // Cancel registration if needed
    if (isPlayerBanned(event.getPlayer())) {
        event.setCanceled(true);
    }
}

@SubscribeEvent
public void onOfferRegistered(MarketOfferEvent.Registered event) {
    System.out.println("New shop registered: " + event.getOffer().shopName());
}
```

### Querying the Market
Access the global list of offers directly:

```java
List<MarketOffer> allOffers = MarketApi.getOffers(server);
```

---

## 🔨 Developers & Contributors

*   **Author:** MakotoPD
*   **License:** GNU GPL v3

---
*Built with ❤️ for the Create community.*
