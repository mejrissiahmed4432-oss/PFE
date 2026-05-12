package com.example.aiservice.model;

import java.util.List;
import java.util.Map;

public class EquipmentSuggestionRequest {
    
    private List<CartItem> cartItems;

    public static class CartItem {
        private String name;
        private Map<String, String> selectedSpecs;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Map<String, String> getSelectedSpecs() { return selectedSpecs; }
        public void setSelectedSpecs(Map<String, String> selectedSpecs) { this.selectedSpecs = selectedSpecs; }
    }

    public List<CartItem> getCartItems() { return cartItems; }
    public void setCartItems(List<CartItem> cartItems) { this.cartItems = cartItems; }
}
