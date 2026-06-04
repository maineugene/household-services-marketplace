package com.zhukovskiy.platform.util;

import java.util.List;

/**
 * Single source of truth for service categories used throughout the application.
 */
public final class ServiceCategories {

    private ServiceCategories() {
    }

    private static final List<String> ALL_CATEGORIES = List.of(
            "Сантехника",
            "Электрика",
            "Ремонт квартир",
            "Уборка",
            "Переезды",
            "Ремонт техники",
            "Садоводство",
            "Репетиторство",
            "Фотография",
            "Дизайн",
            "Строительство",
            "Клининг",
            "Грузоперевозки",
            "Красота и здоровье",
            "IT и программирование"
    );

    public static List<String> getAll() {
        return ALL_CATEGORIES;
    }
}
