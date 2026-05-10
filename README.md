# 🏪 Create: Marketplace

![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Minecraft: 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)
![NeoForge: 21.1.228+](https://img.shields.io/badge/NeoForge-21.1.228+-orange.svg)

**Create: Marketplace** is a specialized expansion for the **Create** mod ecosystem on NeoForge 1.21.1. It bridges the gap between industrial automation and player commerce by providing a unified, server-wide advertising board for all **Create: Numismatics** vendors.

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

### 🌐 Localization & Compatibility
*   **Multilingual:** Full support for **English**, **Polish**, **German**, **Spanish**, and **French**.
*   **Soft Dependency:** Works perfectly with or without Xaero's Minimap installed.

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

## ⚙️ Configuration

Located in `config/create_marketplace-common.toml`:
*   `waypointSymbolMode`: Set how waypoints look on the map (`ICON`, `FIRST_LETTER`, `CUSTOM`).
*   `customWaypointSymbol`: Define a specific character for waypoints.
*   `useCardDurability`: Toggle whether Registration Cards are consumed when registering a shop.

---

## 🔨 Developers & Contributors

*   **Author:** MakotoPD
*   **AI Partner:** Antigravity (Advanced Agentic Coding)
*   **License:** GNU GPL v3

---
*Built with ❤️ for the Create community.*
