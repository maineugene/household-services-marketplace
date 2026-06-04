package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.model.Bid;
import com.zhukovskiy.platform.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    /**
     * Уведомление специалистов о новом заказе
     */
    public void notifySpecialistsAboutNewOrder(Order order) {
        // Здесь будет логика отправки email/sms/push уведомлений
        log.info("Уведомление специалистов о новом заказе #{}", order.getId());
        // TODO: Реализовать отправку уведомлений специалистам подходящих категорий
    }

    /**
     * Уведомление заказчика о новой заявке
     */
    public void notifyCustomerAboutNewBid(Order order, Bid bid) {
        log.info("Уведомление заказчика о новой заявке на заказ #{}, цена: {}",
                order.getId(), bid.getPrice());
        // TODO: Реализовать отправку уведомления заказчику
    }

    /**
     * Уведомление специалиста о выборе его заявки
     */
    public void notifySpecialistSelected(Order order) {
        log.info("Уведомление специалиста о выборе на заказ #{}", order.getId());
        // TODO: Реализовать отправку уведомления специалисту
    }
}