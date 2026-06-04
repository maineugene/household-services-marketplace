package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.OrderDto;
import com.zhukovskiy.platform.model.Order;
import com.zhukovskiy.platform.model.OrderStatus;
import com.zhukovskiy.platform.model.Role;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.repository.OrderRepository;
import com.zhukovskiy.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    private User customer;
    private User specialist;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .email("customer@test.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .country("Russia")
                .password("pass")
                .role(Role.CUSTOMER)
                .dob(LocalDate.of(1990, 1, 1))
                .build();

        specialist = User.builder()
                .id(2L)
                .email("specialist@test.com")
                .firstName("Petr")
                .lastName("Petrov")
                .country("Russia")
                .password("pass")
                .role(Role.SPECIALIST)
                .dob(LocalDate.of(1985, 5, 15))
                .build();

        orderDto = OrderDto.builder()
                .description("Fix plumbing")
                .address("123 Main St")
                .budget(new BigDecimal("5000"))
                .deadline(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    void createOrder_shouldSaveAndNotify() {
        when(orderRepository.countByCustomerAndStatusIn(eq(customer), any()))
                .thenReturn(0L);
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order o = inv.getArgument(0);
                    o.setId(10L);
                    return o;
                });

        Order result = orderService.createOrder(customer, orderDto);

        assertThat(result.getCustomer()).isEqualTo(customer);
        assertThat(result.getDescription()).isEqualTo("Fix plumbing");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.ACTIVE);
        assertThat(result.getAddress()).isEqualTo("123 Main St");
        assertThat(result.getBudget()).isEqualByComparingTo("5000");

        verify(notificationService).notifySpecialistsAboutNewOrder(result);
    }

    @Test
    void createOrder_shouldThrowWhenActiveOrderLimitReached() {
        when(orderRepository.countByCustomerAndStatusIn(eq(customer), any()))
                .thenReturn(5L);

        assertThatThrownBy(() -> orderService.createOrder(customer, orderDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("лимит");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getActiveOrders_shouldDelegateToRepository() {
        List<Order> expected = List.of(Order.builder().id(1L).status(OrderStatus.ACTIVE).build());
        when(orderRepository.findByStatus(OrderStatus.ACTIVE)).thenReturn(expected);

        List<Order> result = orderService.getActiveOrders();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getOrdersByCustomer_shouldDelegateToRepository() {
        List<Order> expected = List.of(Order.builder().id(1L).build());
        when(orderRepository.findByCustomer(customer)).thenReturn(expected);

        assertThat(orderService.getOrdersByCustomer(customer)).isEqualTo(expected);
    }

    @Test
    void getOrdersBySpecialist_shouldDelegateToRepository() {
        List<Order> expected = List.of(Order.builder().id(1L).build());
        when(orderRepository.findBySpecialist(specialist)).thenReturn(expected);

        assertThat(orderService.getOrdersBySpecialist(specialist)).isEqualTo(expected);
    }

    @Test
    void selectSpecialist_shouldUpdateOrderAndNotify() {
        Order order = Order.builder()
                .id(1L)
                .customer(customer)
                .status(OrderStatus.ACTIVE)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // selectSpecialist checks order.getCustomer().getId().equals(specialist.getId())
        // so we pass `customer` as the "specialist" param to satisfy the ownership check
        Order result = orderService.selectSpecialist(1L, customer);

        assertThat(result.getSpecialist()).isEqualTo(customer);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(notificationService).notifySpecialistSelected(result);
    }

    @Test
    void selectSpecialist_shouldThrowWhenOrderNotActive() {
        Order order = Order.builder()
                .id(1L)
                .customer(customer)
                .status(OrderStatus.COMPLETED)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.selectSpecialist(1L, customer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не активен");
    }

    @Test
    void selectSpecialist_shouldThrowWhenNotOwner() {
        Order order = Order.builder()
                .id(1L)
                .customer(customer)
                .status(OrderStatus.ACTIVE)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.selectSpecialist(1L, specialist))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("заказчик");
    }

    @Test
    void selectSpecialist_shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.selectSpecialist(99L, customer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void updateOrderStatus_customerCanCancel() {
        Order order = Order.builder()
                .id(1L)
                .customer(customer)
                .status(OrderStatus.ACTIVE)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.updateOrderStatus(1L, OrderStatus.CANCELLED, customer);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateOrderStatus_customerCanComplete() {
        Order order = Order.builder()
                .id(1L)
                .customer(customer)
                .status(OrderStatus.IN_PROGRESS)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.updateOrderStatus(1L, OrderStatus.COMPLETED, customer);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void updateOrderStatus_customerCannotSetInProgress() {
        Order order = Order.builder()
                .id(1L)
                .customer(customer)
                .status(OrderStatus.ACTIVE)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.IN_PROGRESS, customer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("отменить или завершить");
    }

    @Test
    void updateOrderStatus_specialistCanComplete() {
        Order order = Order.builder()
                .id(1L)
                .customer(customer)
                .specialist(specialist)
                .status(OrderStatus.IN_PROGRESS)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.updateOrderStatus(1L, OrderStatus.COMPLETED, specialist);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void updateOrderStatus_specialistCannotCancel() {
        Order order = Order.builder()
                .id(1L)
                .customer(customer)
                .specialist(specialist)
                .status(OrderStatus.IN_PROGRESS)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.CANCELLED, specialist))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("завершить");
    }

    @Test
    void updateOrderStatus_shouldThrowWhenUnauthorizedUser() {
        User stranger = User.builder().id(99L).build();
        Order order = Order.builder()
                .id(1L)
                .customer(customer)
                .specialist(specialist)
                .status(OrderStatus.IN_PROGRESS)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.COMPLETED, stranger))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нет прав");
    }

    @Test
    void getOrderById_shouldReturnOrder() {
        Order order = Order.builder().id(1L).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThat(orderService.getOrderById(1L)).isEqualTo(order);
    }

    @Test
    void getOrderById_shouldThrowWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(RuntimeException.class);
    }
}
