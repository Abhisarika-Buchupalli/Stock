package com.StockManagmentBackend.stock_managment_backend.controller.webSocketController;

import com.StockManagmentBackend.stock_managment_backend.controller.stocksController.pojo.ListAllStocksItemPojo;
import com.StockManagmentBackend.stock_managment_backend.controller.webSocketController.pojo.BroadStockUserResponsePojo;
import com.StockManagmentBackend.stock_managment_backend.entity.StocksItem;
import com.StockManagmentBackend.stock_managment_backend.entity.UserStocksItem;
import com.StockManagmentBackend.stock_managment_backend.repo.StocksRepo;
import com.StockManagmentBackend.stock_managment_backend.repo.UserDetailsRepo;
import com.StockManagmentBackend.stock_managment_backend.repo.UserStocksRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Controller
@EnableScheduling
public class WebSocketController {

    private final Random random = new Random();

    @Autowired
    private UserDetailsRepo userDetailsRepo;

    @Autowired
    private StocksRepo stocksRepo;

    @Autowired
    private UserStocksRepo userStocksRepo;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    /**
     * Broadcast available stocks and user count to all connected clients
     */
    public void broadcastStockAndUserUpdates() {
        long userCount = userDetailsRepo.count();

        List<UserStocksItem> allPurchasedUserList = userStocksRepo.findAll();
        List<StocksItem> allStocksList = stocksRepo.findAll();

        // Track purchased quantities per stock
        Map<Long, Long> quantityOfStocksPurchasedMap = new HashMap<>();
        for (UserStocksItem item : allPurchasedUserList) {
            quantityOfStocksPurchasedMap.merge(item.getStockId(), item.getNoOfStocks(), Long::sum);
        }

        // Track unique users who purchased each stock
        Map<Long, Set<String>> cntOfUserPurchasedPerStockMap = new HashMap<>();
        for (UserStocksItem item : allPurchasedUserList) {
            cntOfUserPurchasedPerStockMap
                    .computeIfAbsent(item.getStockId(), k -> new HashSet<>())
                    .add(item.getEmail());
        }

        List<ListAllStocksItemPojo> finalList = new ArrayList<>();
        for (StocksItem stock : allStocksList) {
            // Only include stocks with remaining quantity > 0
            if (stock.getRemQuantity() == null || stock.getRemQuantity() <= 0) continue;

            ListAllStocksItemPojo obj = new ListAllStocksItemPojo();
            obj.setId(stock.getId());
            obj.setName(stock.getName());
            obj.setOriginalPrice(stock.getOriginalPrice());
            obj.setLowestPrice(stock.getLowestPrice());
            obj.setHighestPrice(stock.getHighestPrice());
            obj.setCurPrice(stock.getCurPrice());
            obj.setPercentage(stock.getPercentage());
            obj.setRemQuantity(stock.getRemQuantity());
            obj.setInitialQuantity(stock.getInitialQuantity());
            obj.setPrevPercentages(stock.getPrevPercentages() != null ? stock.getPrevPercentages() : new ArrayList<>());

            Long purchasedQuantity = quantityOfStocksPurchasedMap.getOrDefault(stock.getId(), 0L);
            obj.setPurchasedQuantity(purchasedQuantity);

            Long totalInvestedUserCnt = (long) cntOfUserPurchasedPerStockMap.getOrDefault(stock.getId(), Collections.emptySet()).size();
            obj.setTotalInvestedUserCnt(totalInvestedUserCnt);

            finalList.add(obj);
        }

        BroadStockUserResponsePojo finalObj = new BroadStockUserResponsePojo(userCount, finalList);
        simpMessagingTemplate.convertAndSend("/stockUpdates", finalObj);
    }

    /**
     * Periodically update stock prices and percentages every 20 seconds
     */
    @Scheduled(fixedRate = 20000)
    public void refreshGeneralStockWithSchedule() {
        List<StocksItem> allStocksList = stocksRepo.findAll();

        for (StocksItem stock : allStocksList) {
            Long newPrice = stock.getOriginalPrice();

            double randomNumber = -10.0 + (20.0 * random.nextDouble());
            BigDecimal roundedNumber = BigDecimal.valueOf(randomNumber).setScale(2, RoundingMode.HALF_UP);
            Long priceChange = (long) ((stock.getOriginalPrice() * roundedNumber.doubleValue()) / 100);
            newPrice += priceChange;

            if (newPrice > stock.getHighestPrice()) stock.setHighestPrice(newPrice);
            if (newPrice < stock.getLowestPrice()) stock.setLowestPrice(newPrice);

            stock.setCurPrice(newPrice);
            stock.setPercentage(roundedNumber);

            List<BigDecimal> updatedPerList = stock.getPrevPercentages() != null ? stock.getPrevPercentages() : new ArrayList<>();
            if (updatedPerList.size() >= 10) updatedPerList.remove(0); // keep last 10
            updatedPerList.add(roundedNumber);
            stock.setPrevPercentages(updatedPerList);
        }

        try {
            stocksRepo.saveAll(allStocksList);
        } catch (Exception e) {
            System.out.println("Error refreshing stock details: " + e.getMessage());
        }

        broadcastStockAndUserUpdates();
    }
}
