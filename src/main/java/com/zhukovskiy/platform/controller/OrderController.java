package com.zhukovskiy.platform.controller;

import com.zhukovskiy.platform.dto.OrderDto;
import com.zhukovskiy.platform.model.Order;
import com.zhukovskiy.platform.model.OrderStatus;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.security.SecurityUtils;
import com.zhukovskiy.platform.service.BidService;
import com.zhukovskiy.platform.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;
    private final BidService bidService;
    private final SecurityUtils securityUtils;

    /**
     * Форма создания заказа
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("order", new OrderDto());
        return "orders/create-form";
    }

    /**
     * Создание заказа
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/create")
    public String createOrder(@Valid @ModelAttribute("order") OrderDto orderDto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "orders/create-form";
        }

        try {
            User currentUser = securityUtils.getCurrentUser();
            Order order = orderService.createOrder(currentUser, orderDto);
            redirectAttributes.addFlashAttribute("success", "Заказ успешно создан!");
            return "redirect:/orders/my";
        } catch (Exception e) {
            log.error("Ошибка при создании заказа", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании заказа");
            return "redirect:/orders/create";
        }
    }

    /**
     * Мои заказы (как заказчик)
     */
    @GetMapping("/my")
    public String myOrders(Model model) {
        User currentUser = securityUtils.getCurrentUser();
        model.addAttribute("orders", orderService.getOrdersByCustomer(currentUser));
        model.addAttribute("role", "customer");
        return "orders/my-orders";
    }

    /**
     * Заказы, где я специалист
     */
    @GetMapping("/my-work")
    public String myWorkOrders(Model model) {
        User currentUser = securityUtils.getCurrentUser();
        model.addAttribute("orders", orderService.getOrdersBySpecialist(currentUser));
        model.addAttribute("role", "specialist");
        return "orders/my-orders";
    }

    /**
     * Доступные заказы для специалистов
     */
    @PreAuthorize("hasRole('SPECIALIST')")
    @GetMapping("/available")
    public String availableOrders(Model model) {
        User currentUser = securityUtils.getCurrentUser();
        model.addAttribute("orders", orderService.getAvailableOrdersForSpecialist(currentUser));
        return "orders/available-orders";
    }

    /**
     * Просмотр деталей заказа
     */
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        User currentUser = securityUtils.getCurrentUser();

        model.addAttribute("order", order);
        model.addAttribute("currentUser", currentUser);

        // Если пользователь - заказчик, показываем заявки
        if (order.getCustomer().getId().equals(currentUser.getId())) {
            model.addAttribute("bids", bidService.getBidsByOrder(order));
        }

        // Если пользователь - специалист, проверял ли он уже заявку
        if (currentUser.getRole().toString().equals("SPECIALIST")) {
            boolean hasBid = bidService.hasBid(order, currentUser);
            model.addAttribute("hasBid", hasBid);
        }

        return "orders/view-order";
    }

    /**
     * Отмена заказа (для заказчика)
     */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            orderService.updateOrderStatus(id, OrderStatus.CANCELLED, currentUser);
            redirectAttributes.addFlashAttribute("success", "Заказ отменен");
        } catch (Exception e) {
            log.error("Ошибка при отмене заказа", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при отмене заказа");
        }
        return "redirect:/orders/my";
    }

    /**
     * Завершение заказа (для заказчика или специалиста)
     */
    @PostMapping("/{id}/complete")
    public String completeOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            orderService.updateOrderStatus(id, OrderStatus.COMPLETED, currentUser);
            redirectAttributes.addFlashAttribute("success", "Заказ завершен");
        } catch (Exception e) {
            log.error("Ошибка при завершении заказа", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при завершении заказа");
        }
        return "redirect:/orders/my";
    }
}