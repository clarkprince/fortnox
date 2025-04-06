package com.dto;

public class PartDTO {
    private String name;
    private String reference;
    private String description;
    private String price;
    private Integer minQuantity;
    private Boolean isTracked;
    private Boolean isSerializable;
    private Category category;
    private Tax tax;
    private String status;
    private String type;

    public static class Category {
        private Integer id;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }

    public static class Tax {
        private Integer id;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }

    public Boolean getIsTracked() {
        return isTracked;
    }

    public void setIsTracked(Boolean isTracked) {
        this.isTracked = isTracked;
    }

    public Boolean getIsSerializable() {
        return isSerializable;
    }

    public void setIsSerializable(Boolean isSerializable) {
        this.isSerializable = isSerializable;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Tax getTax() {
        return tax;
    }

    public void setTax(Tax tax) {
        this.tax = tax;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
