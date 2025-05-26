package com.javaadv.Model;

import java.util.List;

public class CategoryResponse {
    private int status;
    private String message;
    private CategoryData data;

    // Getters and setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CategoryData getData() {
        return data;
    }

    public void setData(CategoryData data) {
        this.data = data;
    }

    // Nested class CategoryData
    public static class CategoryData {
        private int pageNo;
        private int pageSize;
        private int totalPage;
        private List<Category> items;

        // Getters and setters
        public int getPageNo() {
            return pageNo;
        }

        public void setPageNo(int pageNo) {
            this.pageNo = pageNo;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getTotalPage() {
            return totalPage;
        }

        public void setTotalPage(int totalPage) {
            this.totalPage = totalPage;
        }

        public List<Category> getItems() {
            return items;
        }

        public void setItems(List<Category> items) {
            this.items = items;
        }
    }
}