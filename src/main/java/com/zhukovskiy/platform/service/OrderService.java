package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.OrderDto;
import com.zhukovskiy.platform.model.*;
import com.zhukovskiy.platform.repository.OrderRepository;
import com.zhukovskiy.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final int MAX_ACTIVE_ORDERS = 5; // Максимум активных заказов для заказчика

    /**
     * Создание нового заказа
     */
    @Transactional
    public Order createOrder(User customer, OrderDto orderDto) {
        // Проверяем количество активных заказов у заказчика
        long activeOrdersCount = orderRepository.countByCustomerAndStatusIn(customer,
                List.of(OrderStatus.ACTIVE, OrderStatus.IN_PROGRESS));

        if (activeOrdersCount >= MAX_ACTIVE_ORDERS) {
            throw new RuntimeException("Превышен лимит активных заказов (максимум " + MAX_ACTIVE_ORDERS + ")");
        }

        Order order = Order.builder()
                .customer(customer)
                .description(orderDto.getDescription())
                .address(orderDto.getAddress())
                .budget(orderDto.getBudget())
                .deadline(orderDto.getDeadline())
                .status(OrderStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Уведомляем подходящих специалистов
        notificationService.notifySpecialistsAboutNewOrder(savedOrder);

        return savedOrder;
    }

    /**
     * Получение всех активных заказов
     */
    public List<Order> getActiveOrders() {
        return orderRepository.findByStatus(OrderStatus.ACTIVE);
    }

    /**
     * Получение заказов заказчика
     */
    public List<Order> getOrdersByCustomer(User customer) {
        return orderRepository.findByCustomer(customer);
    }

    /**
     * Получение заказов специалиста
     */
    public List<Order> getOrdersBySpecialist(User specialist) {
        return orderRepository.findBySpecialist(specialist);
    }

    /**
     * Получение доступных заказов для специалиста
     */
    public List<Order> getAvailableOrdersForSpecialist(User specialist) {
        return orderRepository.findAvailableOrdersForSpecialist(specialist);
    }

    /**
     * Выбор исполнителя для заказа
     */
    @Transactional
    public Order selectSpecialist(Long orderId, User specialist) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        // Проверяем, что заказ еще активен
        if (order.getStatus() != OrderStatus.ACTIVE) {
            throw new RuntimeException("Заказ уже не активен");
        }

        // Проверяем, что текущий пользователь - заказчик
        if (!order.getCustomer().getId().equals(specialist.getId())) {
            throw new RuntimeException("Только заказчик может выбрать исполнителя");
        }

        order.setSpecialist(specialist);
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);

        // Уведомляем выбранного специалиста
        notificationService.notifySpecialistSelected(updatedOrder);

        return updatedOrder;
    }

    /**
     * Изменение статуса заказа
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        // Проверяем права на изменение статуса
        if (order.getCustomer().getId().equals(user.getId())) {
            // Заказчик может отменить заказ или отметить как выполненный
            if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.COMPLETED) {
                order.setStatus(newStatus);
            } else {
                throw new RuntimeException("Заказчик может только отменить или завершить заказ");
            }
        } else if (order.getSpecialist() != null && order.getSpecialist().getId().equals(user.getId())) {
            // Специалист может отметить как выполненный
            if (newStatus == OrderStatus.COMPLETED) {
                order.setStatus(newStatus);
            } else {
                throw new RuntimeException("Специалист может только завершить заказ");
            }
        } else {
            throw new RuntimeException("Нет прав для изменения статуса заказа");
        }

        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /**
     * Получение заказа по ID
     */
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }
}