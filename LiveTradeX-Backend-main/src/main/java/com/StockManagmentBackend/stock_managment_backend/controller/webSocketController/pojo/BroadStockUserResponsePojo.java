package com.StockManagmentBackend.stock_managment_backend.controller.webSocketController.pojo;

import com.StockManagmentBackend.stock_managment_backend.controller.stocksController.pojo.ListAllStocksItemPojo;

import java.util.List;

public class BroadStockUserResponsePojo {

    private long userCount;
    private List<ListAllStocksItemPojo> listAllStocks;

    // Default constructor for JSON deserialization
    public BroadStockUserResponsePojo() {}

    // Parameterized constructor
    public BroadStockUserResponsePojo(long userCount, List<ListAllStocksItemPojo> listAllStocks) {
        this.userCount = userCount;
        this.listAllStocks = listAllStocks;
    }

    public long getUserCount() {
        return userCount;
    }

    public void setUserCount(long userCount) {
        this.userCount = userCount;
    }

    public List<ListAllStocksItemPojo> getListAllStocks() {
        return listAllStocks;
    }

    public void setListAllStocks(List<ListAllStocksItemPojo> listAllStocks) {
        this.listAllStocks = listAllStocks;
    }
}
