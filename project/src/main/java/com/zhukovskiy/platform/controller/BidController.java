package com.zhukovskiy.platform.controller;

import com.zhukovskiy.platform.dto.BidDto;
import com.zhukovskiy.platform.model.Order;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.security.SecurityUtils;
import com.zhukovskiy.platform.service.BidService;
import com.zhukovskiy.platform.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;
    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    /**
     * Форма подачи заявки на заказ
     */
    @GetMapping("/create/{orderId}")
    public String showCreateBidForm(@PathVariable Long orderId, Model model) {
        Order order = orderService.getOrderById(orderId);
        model.addAttribute("order", order);
        model.addAttribute("bid", new BidDto());
        return "bids/create-form";
    }

    /**
     * Подача заявки на заказ
     */
    @PostMapping("/create/{orderId}")
    public String createBid(@PathVariable Long orderId,
                            @Valid @ModelAttribute("bid") BidDto bidDto,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("order", orderService.getOrderById(orderId));
            return "bids/create-form";
        }

        try {
            User currentUser = securityUtils.getCurrentUser();
            Order order = orderService.getOrderById(orderId);
            bidService.createBid(order, currentUser, bidDto);
            redirectAttributes.addFlashAttribute("success", "Заявка успешно подана!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при подаче заявки: " + e.getMessage());
        }

        return "redirect:/orders/" + orderId;
    }

    /**
     * Выбор заявки (для заказчика)
     */
    @PostMapping("/{bidId}/select")
    public String selectBid(@PathVariable Long bidId, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            bidService.selectBid(bidId, currentUser);
            redirectAttributes.addFlashAttribute("success", "Специалист выбран! Заказ передан в работу.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/bids";
    }

    /**
     * Мои заявки (для специалиста)
     */
    @GetMapping("/my")
    public String myBids(Model model) {
        User currentUser = securityUtils.getCurrentUser();
        model.addAttribute("bids", bidService.getBidsBySpecialist(currentUser));
        return "bids/my-bids";
    }

    /**
     * Заявки на заказ (для заказчика)
     */
    @GetMapping("/order/{orderId}")
    public String orderBids(@PathVariable Long orderId, Model model) {
        Order order = orderService.getOrderById(orderId);
        User currentUser = securityUtils.getCurrentUser();

        // Проверяем, что текущий пользователь - заказчик
        if (!order.getCustomer().getId().equals(currentUser.getId())) {
            return "redirect:/orders/my";
        }

        model.addAttribute("order", order);
        model.addAttribute("bids", bidService.getBidsByOrder(order));
        return "bids/order-bids";
    }
}